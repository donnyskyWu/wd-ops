package cn.iocoder.yudao.module.oa.util;

import cn.hutool.core.util.ReUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Import pipeline: preserve full sanitized HTML for preview; extract layout_schema for merge-on-apply (ADR-027 §4.2).
 */
public final class LayoutImportExtractor {

    private static final Pattern IFRAME_PATTERN = Pattern.compile("(?is)<iframe[^>]*>.*?</iframe>");
    private static final Pattern STYLE_TAG_PATTERN = Pattern.compile("(?is)<style[^>]*>(.*?)</style>");
    private static final Pattern INLINE_STYLE_PATTERN = Pattern.compile("(?is)\\bstyle=[\"']([^\"']*)[\"']");

    private LayoutImportExtractor() {
    }

    @Getter
    public static final class ExtractResult {
        private final String rawHtml;
        private final String previewHtml;
        private final String layoutHtml;
        private final String styleCss;
        private final JSONObject layoutSchema;
        private final JSONObject extractionReport;

        ExtractResult(String rawHtml, String previewHtml, String layoutHtml, String styleCss,
                      JSONObject layoutSchema, JSONObject extractionReport) {
            this.rawHtml = rawHtml;
            this.previewHtml = previewHtml;
            this.layoutHtml = layoutHtml;
            this.styleCss = styleCss;
            this.layoutSchema = layoutSchema;
            this.extractionReport = extractionReport;
        }
    }

    public static ExtractResult extract(String html) {
        String sanitized = sanitizeImportHtml(html);
        JSONObject v1 = LayoutJsonHelper.parseHtmlToLayout(sanitized);
        JSONObject schema = LayoutSchemaHelper.extractLayoutSchemaFromLayoutJson(v1);
        LayoutSchemaHelper.enrichGlobalStylesFromBlocks(v1, schema);
        String styleCss = extractStyleCss(sanitized);
        String previewHtml = buildPreviewHtml(sanitized, styleCss);
        String layoutHtml = previewHtml;
        JSONObject report = LayoutSchemaHelper.buildExtractionReport(v1, schema);
        report.set("previewMode", "fullHtml");
        report.set("inlineStyleCount", countInlineStyles(sanitized));
        if (StrUtil.isNotBlank(styleCss)) {
            report.set("styleCssExtracted", true);
        }
        List<String> warnings = new ArrayList<>();
        if (report.get("warnings") != null) {
            warnings.addAll(report.getJSONArray("warnings").toList(String.class));
        }
        warnings.add(0, "预览展示完整导入 HTML；套用模板时仍通过 layout_schema 合并正文（ADR-020 保真）");
        report.set("warnings", warnings);
        return new ExtractResult(sanitized, previewHtml, layoutHtml, styleCss, schema, report);
    }

    public static String sanitizeImportHtml(String html) {
        if (StrUtil.isBlank(html)) {
            return "";
        }
        String out = normalizeWechatPreviewHtml(html);
        out = LayoutJsonHelper.sanitizeHtml(out);
        out = IFRAME_PATTERN.matcher(out).replaceAll("");
        out = out.replaceAll("(?i)<object[^>]*>.*?</object>", "");
        out = out.replaceAll("(?i)<embed[^>]*/?>", "");
        out = out.replaceAll("(?i)<img[^>]*class=\"[^\"]*ProseMirror-separator[^\"]*\"[^>]*/?>", "");
        out = out.replaceAll("(?i)\\scontenteditable=\"(?:true|false)\"", "");
        return out.trim();
    }

    /** WeChat pages hide #js_content until JS runs; strip so preview is visible. */
    public static String normalizeWechatPreviewHtml(String html) {
        if (StrUtil.isBlank(html)) {
            return "";
        }
        String out = html;
        out = out.replaceAll("(?i)visibility\\s*:\\s*hidden\\s*;?", "");
        out = out.replaceAll("(?i)opacity\\s*:\\s*0(?:\\.0+)?\\s*;?", "");
        out = out.replaceAll("(?i)\\sstyle=\"\\s*;?\\s*\"", "");
        out = out.replaceAll("(?i)\\sstyle='\\s*;?\\s*'", "");
        return out;
    }

    static String extractStyleCss(String html) {
        if (StrUtil.isBlank(html)) {
            return "";
        }
        StringBuilder css = new StringBuilder();
        for (String block : ReUtil.findAllGroup1(STYLE_TAG_PATTERN, html)) {
            if (StrUtil.isNotBlank(block)) {
                if (!css.isEmpty()) {
                    css.append('\n');
                }
                css.append(block.trim());
            }
        }
        return css.toString().trim();
    }

    static String buildPreviewHtml(String contentHtml, String styleCss) {
        String body = StrUtil.blankToDefault(contentHtml, "");
        if (StrUtil.isBlank(styleCss)) {
            return body;
        }
        return "<div class=\"layout-import-preview\"><style>" + styleCss + "</style>" + body + "</div>";
    }

    static int countInlineStyles(String html) {
        if (StrUtil.isBlank(html)) {
            return 0;
        }
        return ReUtil.findAllGroup1(INLINE_STYLE_PATTERN, html).size();
    }
}
