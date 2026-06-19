package cn.iocoder.yudao.module.oa.service.content;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import cn.iocoder.yudao.framework.common.exception.OaErrorCodes;
import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.module.oa.api.dto.content.ContentTypesetReq;
import cn.iocoder.yudao.module.oa.api.dto.content.ContentTypesetVO;
import cn.iocoder.yudao.module.oa.api.dto.content.LayoutMergePreviewReq;
import cn.iocoder.yudao.module.oa.api.dto.content.LayoutMergePreviewVO;
import cn.iocoder.yudao.module.oa.api.dto.content.TypesettingRuleVO;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.oa.dal.dataobject.content.LayoutStyleDO;
import cn.iocoder.yudao.module.oa.dal.mysql.content.LayoutStyleMapper;
import cn.iocoder.yudao.module.oa.util.LayoutJsonHelper;
import cn.iocoder.yudao.module.oa.util.LayoutSchemaHelper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * One-click typesetting: template merge or rule-based optimization (ADR-020 body preservation).
 */
@Service
@RequiredArgsConstructor
public class TypesettingService {

    private static final Pattern TAG_PATTERN = Pattern.compile("<[^>]+>");
    private static final Pattern STYLE_ATTR_PATTERN = Pattern.compile("(?i)style=\"([^\"]*)\"");

    private final TypesettingRuleService typesettingRuleService;
    private final WechatLayoutTemplateService layoutTemplateService;
    private final LayoutStyleMapper layoutStyleMapper;

    public ContentTypesetVO typeset(ContentTypesetReq req) {
        String mode = resolveMode(req);
        if ("TEMPLATE".equals(mode)) {
            return typesetByTemplate(req);
        }
        return typesetByRules(req);
    }

    private String resolveMode(ContentTypesetReq req) {
        if (StrUtil.isNotBlank(req.getMode())) {
            return req.getMode().toUpperCase();
        }
        return req.getTemplateId() != null ? "TEMPLATE" : "AUTO";
    }

    private ContentTypesetVO typesetByTemplate(ContentTypesetReq req) {
        Long templateId = req.getTemplateId();
        if (templateId == null) {
            throw new ServiceException(OaErrorCodes.BAD_REQUEST.getCode(), "按模板排版需选择模板");
        }
        String body = resolveMergeBody(req);
        if (StrUtil.isBlank(body)) {
            throw new ServiceException(OaErrorCodes.LAYOUT_APPLY_BODY_EMPTY);
        }
        String plainBefore = extractPlainText(body);

        LayoutMergePreviewReq previewReq = new LayoutMergePreviewReq();
        previewReq.setBody(body);
        previewReq.setParamOverrides(req.getParamOverrides());
        LayoutMergePreviewVO merged = layoutTemplateService.previewMerge(templateId, previewReq);

        String plainAfter = extractPlainTextFromLayout(merged.getLayoutJson());
        ContentTypesetVO vo = new ContentTypesetVO();
        vo.setMode("TEMPLATE");
        vo.setTemplateId(templateId);
        vo.setHtml(merged.getLayoutHtml());
        vo.setLayoutJson(merged.getLayoutJson());
        vo.setOverflowSegmentCount(merged.getOverflowSegmentCount());
        vo.setPlainTextBefore(plainBefore);
        vo.setPlainTextAfter(plainAfter);
        vo.setRulesApplied(1);
        return vo;
    }

    private ContentTypesetVO typesetByRules(ContentTypesetReq req) {
        String html = LayoutJsonHelper.sanitizeHtml(req.getHtml());
        String plainBefore = extractPlainText(html);
        List<TypesettingRuleVO> rules = typesettingRuleService.listEnabled();
        Set<String> filter = req.getRuleCodes() != null ? new HashSet<>(req.getRuleCodes()) : null;

        int applied = 0;
        String result = html;
        for (TypesettingRuleVO rule : rules) {
            if (filter != null && !filter.isEmpty() && !filter.contains(rule.getRuleCode())) {
                continue;
            }
            JSONObject config = JSONUtil.parseObj(JSONUtil.toJsonStr(rule.getRuleConfig()));
            String type = config.getStr("type", "");
            if ("TEMPLATE_LINK".equals(type)) {
                Long linkedId = config.getLong("linkedTemplateId");
                if (linkedId != null) {
                    ContentTypesetReq tplReq = new ContentTypesetReq();
                    tplReq.setHtml(html);
                    tplReq.setBody(resolveMergeBody(req));
                    tplReq.setTemplateId(linkedId);
                    tplReq.setParamOverrides(req.getParamOverrides());
                    tplReq.setMode("TEMPLATE");
                    return typesetByTemplate(tplReq);
                }
                continue;
            }
            result = applyRule(result, config);
            applied++;
        }

        String plainAfter = extractPlainText(result);
        ContentTypesetVO vo = new ContentTypesetVO();
        vo.setMode("AUTO");
        vo.setHtml(result);
        vo.setPlainTextBefore(plainBefore);
        vo.setPlainTextAfter(plainAfter);
        vo.setRulesApplied(applied);
        return vo;
    }

    private String resolveMergeBody(ContentTypesetReq req) {
        if (StrUtil.isNotBlank(req.getBody())) {
            return req.getBody().trim();
        }
        return extractPlainText(req.getHtml());
    }

    private String applyRule(String html, JSONObject config) {
        String type = config.getStr("type", "");
        return switch (type) {
            case "UNIFY_HEADING" -> unifyHeadings(html, config);
            case "PARAGRAPH_SPACING" -> paragraphSpacing(html, config);
            case "NORMALIZE_QUOTE" -> normalizeQuote(html, config);
            case "IMAGE_RESPONSIVE" -> imageResponsive(html, config);
            case "STRIP_INLINE_STYLE" -> stripInlineStyle(html, config.getBool("preserveBold", true));
            case "SMART_OPTIMIZE" -> applySmartOptimize(html, config);
            default -> html;
        };
    }

    private String applySmartOptimize(String html, JSONObject config) {
        String result = detectAndPromoteHeadings(html);
        JSONObject styleRefs = config.getJSONObject("styleRefs");
        if (styleRefs != null) {
            result = applyStyleRef(result, styleRefs.getStr("heading"), "h[1-6]");
            result = applyStyleRef(result, styleRefs.getStr("paragraph"), "p");
            result = applyStyleRef(result, styleRefs.getStr("quote"), "blockquote");
        }
        JSONArray blockSequence = config.getJSONArray("blockSequence");
        if (blockSequence != null) {
            for (int i = 0; i < blockSequence.size(); i++) {
                String subType = blockSequence.getStr(i);
                JSONObject subConfig = defaultConfigForType(subType);
                result = applyRule(result, subConfig);
            }
        }
        return result;
    }

    private JSONObject defaultConfigForType(String type) {
        JSONObject config = new JSONObject();
        config.set("type", type);
        return switch (type) {
            case "UNIFY_HEADING" -> {
                config.set("heading2Size", "18px");
                config.set("heading3Size", "16px");
                yield config;
            }
            case "PARAGRAPH_SPACING" -> {
                config.set("lineHeight", "1.75");
                config.set("marginBottom", "16px");
                config.set("fontSize", "16px");
                yield config;
            }
            case "NORMALIZE_QUOTE" -> {
                config.set("borderColor", "#07c160");
                config.set("backgroundColor", "#f7f7f7");
                yield config;
            }
            case "IMAGE_RESPONSIVE" -> {
                config.set("maxWidth", "100%");
                yield config;
            }
            default -> config;
        };
    }

    /** Short standalone paragraphs → h3 headings (title detection). */
    private String detectAndPromoteHeadings(String html) {
        Pattern pPattern = Pattern.compile("(?is)(<p)([^>]*)(>)(.*?)(</p>)");
        Matcher m = pPattern.matcher(html);
        StringBuilder sb = new StringBuilder();
        while (m.find()) {
            String innerText = extractPlainText(m.group(4));
            if (looksLikeTitle(innerText)) {
                String attrs = m.group(2);
                m.appendReplacement(sb, Matcher.quoteReplacement(
                        "<h3" + attrs + ">" + m.group(4) + "</h3>"));
            } else {
                m.appendReplacement(sb, Matcher.quoteReplacement(m.group(0)));
            }
        }
        m.appendTail(sb);
        return sb.toString();
    }

    private boolean looksLikeTitle(String text) {
        if (StrUtil.isBlank(text) || text.length() > 40) {
            return false;
        }
        if (text.endsWith("。") || text.endsWith("！") || text.endsWith("？")
                || text.endsWith(".") || text.endsWith("!") || text.endsWith("?")) {
            return false;
        }
        return !text.contains("，") && !text.contains(",");
    }

    private String applyStyleRef(String html, String styleCode, String tagPattern) {
        if (StrUtil.isBlank(styleCode)) {
            return html;
        }
        LayoutStyleDO style = layoutStyleMapper.selectOne(new LambdaQueryWrapper<LayoutStyleDO>()
                .eq(LayoutStyleDO::getTenantId, TenantContextHolder.getTenantId())
                .eq(LayoutStyleDO::getStyleCode, styleCode)
                .eq(LayoutStyleDO::getStatus, "ENABLED")
                .last("LIMIT 1"));
        if (style == null || StrUtil.isBlank(style.getHtmlSnippet())) {
            return html;
        }
        Matcher sm = STYLE_ATTR_PATTERN.matcher(style.getHtmlSnippet());
        if (!sm.find()) {
            return html;
        }
        String styleDecl = sm.group(1);
        return injectStyle(html, tagPattern, styleDecl);
    }

    private String unifyHeadings(String html, JSONObject config) {
        String h2Size = config.getStr("heading2Size", "18px");
        String h3Size = config.getStr("heading3Size", "16px");
        html = html.replaceAll("(?i)<h1([^>]*)>", "<h2$1>");
        html = html.replaceAll("(?i)</h1>", "</h2>");
        html = html.replaceAll("(?i)<h[456]([^>]*)>", "<h3$1>");
        html = html.replaceAll("(?i)</h[456]>", "</h3>");
        html = injectStyle(html, "h2", "font-size:" + h2Size + ";font-weight:bold;margin:20px 0 12px;");
        html = injectStyle(html, "h3", "font-size:" + h3Size + ";font-weight:bold;margin:16px 0 8px;");
        return html;
    }

    private String paragraphSpacing(String html, JSONObject config) {
        String lineHeight = config.getStr("lineHeight", "1.75");
        String marginBottom = config.getStr("marginBottom", "16px");
        String fontSize = config.getStr("fontSize", "16px");
        return injectStyle(html, "p",
                "font-size:" + fontSize + ";line-height:" + lineHeight + ";margin-bottom:" + marginBottom + ";");
    }

    private String normalizeQuote(String html, JSONObject config) {
        String borderColor = config.getStr("borderColor", "#07c160");
        String bg = config.getStr("backgroundColor", "#f7f7f7");
        return injectStyle(html, "blockquote",
                "border-left:4px solid " + borderColor + ";background:" + bg
                        + ";padding:12px 16px;margin:16px 0;line-height:1.6;");
    }

    private String imageResponsive(String html, JSONObject config) {
        String maxWidth = config.getStr("maxWidth", "100%");
        Pattern imgPattern = Pattern.compile("(?i)(<img[^>]*)(/?>)");
        Matcher m = imgPattern.matcher(html);
        StringBuilder sb = new StringBuilder();
        while (m.find()) {
            String tag = m.group(1);
            if (!tag.contains("style=")) {
                tag += " style=\"max-width:" + maxWidth + ";height:auto;\"";
            } else {
                tag = tag.replaceAll("(?i)style=\"([^\"]*)\"", "style=\"$1max-width:" + maxWidth + ";height:auto;\"");
            }
            m.appendReplacement(sb, Matcher.quoteReplacement(tag + m.group(2)));
        }
        m.appendTail(sb);
        return sb.toString();
    }

    private String stripInlineStyle(String html, boolean preserveBold) {
        html = html.replaceAll("(?i)<span[^>]*style=\"[^\"]*\"[^>]*>", preserveBold ? "<span>" : "");
        html = html.replaceAll("(?i)</span>", preserveBold ? "</span>" : "");
        return html;
    }

    private String injectStyle(String html, String tag, String styleDecl) {
        Pattern p = Pattern.compile("(?i)(<" + tag + ")([^>]*)(>)");
        Matcher m = p.matcher(html);
        StringBuilder sb = new StringBuilder();
        while (m.find()) {
            String attrs = m.group(2);
            if (attrs.contains("style=")) {
                attrs = attrs.replaceAll("(?i)style=\"([^\"]*)\"", "style=\"" + styleDecl + "$1\"");
            } else {
                attrs += " style=\"" + styleDecl + "\"";
            }
            m.appendReplacement(sb, Matcher.quoteReplacement(m.group(1) + attrs + m.group(3)));
        }
        m.appendTail(sb);
        return sb.toString();
    }

    static String extractPlainText(String html) {
        if (StrUtil.isBlank(html)) {
            return "";
        }
        String text = TAG_PATTERN.matcher(html).replaceAll("");
        text = text.replace("&nbsp;", " ").replace("&amp;", "&")
                .replace("&lt;", "<").replace("&gt;", ">").replace("&quot;", "\"");
        return text.replaceAll("\\s+", " ").trim();
    }

    private static String extractPlainTextFromLayout(Object layoutJson) {
        return LayoutSchemaHelper.extractTextFromLayout(layoutJson);
    }
}
