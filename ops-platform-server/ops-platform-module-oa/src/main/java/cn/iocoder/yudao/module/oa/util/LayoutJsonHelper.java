package cn.iocoder.yudao.module.oa.util;

import cn.hutool.core.util.EscapeUtil;
import cn.hutool.core.util.ReUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import cn.iocoder.yudao.framework.common.exception.OaErrorCodes;
import cn.iocoder.yudao.framework.common.exception.ServiceException;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * 公推版式 layout_json 校验、渲染与 HTML 解析（ADR-019 §2.2）
 */
public final class LayoutJsonHelper {

    private static final Set<String> BLOCK_TYPES = Set.of(
            "heading", "paragraph", "image", "quote", "divider", "list");
    private static final Pattern SCRIPT_PATTERN = Pattern.compile("(?is)<script[^>]*>.*?</script>");
    private static final Pattern ON_EVENT_PATTERN = Pattern.compile("(?i)\\s(on\\w+)\\s*=");

    private LayoutJsonHelper() {
    }

    public static JSONObject emptyDocument() {
        JSONObject doc = new JSONObject();
        doc.set("version", 1);
        doc.set("blocks", new JSONArray());
        return doc;
    }

    public static void validateLayoutJson(Object layoutJson) {
        if (layoutJson == null) {
            throw new ServiceException(OaErrorCodes.LAYOUT_JSON_INVALID);
        }
        JSONObject doc = toJsonObject(layoutJson);
        int version = doc.getInt("version", 0);
        if (version != 1 && version != 2) {
            throw new ServiceException(OaErrorCodes.LAYOUT_JSON_INVALID);
        }
        JSONArray blocks = doc.getJSONArray("blocks");
        if (blocks == null) {
            throw new ServiceException(OaErrorCodes.LAYOUT_JSON_INVALID);
        }
        for (int i = 0; i < blocks.size(); i++) {
            JSONObject block = blocks.getJSONObject(i);
            if (block == null) {
                throw new ServiceException(OaErrorCodes.LAYOUT_JSON_INVALID);
            }
            String type = block.getStr("type");
            if (!BLOCK_TYPES.contains(type) && !"fixed".equals(type)) {
                throw new ServiceException(OaErrorCodes.LAYOUT_JSON_INVALID);
            }
        }
    }

    public static String toJsonString(Object layoutJson) {
        validateLayoutJson(layoutJson);
        return JSONUtil.toJsonStr(toJsonObject(layoutJson));
    }

    public static String renderHtml(Object layoutJson) {
        return renderHtml(layoutJson, false);
    }

    public static String renderHtml(Object layoutJson, boolean skeletonMode) {
        validateLayoutJson(layoutJson);
        JSONArray blocks = toJsonObject(layoutJson).getJSONArray("blocks");
        StringBuilder sb = new StringBuilder();
        sb.append("<section class=\"layout-article\">");
        for (int i = 0; i < blocks.size(); i++) {
            sb.append(renderBlock(blocks.getJSONObject(i), skeletonMode));
        }
        sb.append("</section>");
        return sanitizeHtml(sb.toString());
    }

    public static JSONObject parseHtmlToLayout(String html) {
        if (StrUtil.isBlank(html)) {
            return emptyDocument();
        }
        String cleaned = sanitizeHtml(html);
        JSONArray blocks = new JSONArray();
        List<String> segments = splitHtmlSegments(cleaned);
        for (String seg : segments) {
            JSONObject block = segmentToBlock(seg.trim());
            if (block != null) {
                blocks.add(block);
            }
        }
        if (blocks.isEmpty()) {
            blocks.add(paragraphBlock(stripTags(cleaned)));
        }
        JSONObject doc = emptyDocument();
        doc.set("blocks", blocks);
        validateLayoutJson(doc);
        return doc;
    }

    public static String sanitizeHtml(String html) {
        if (StrUtil.isBlank(html)) {
            return "";
        }
        String out = SCRIPT_PATTERN.matcher(html).replaceAll("");
        out = ON_EVENT_PATTERN.matcher(out).replaceAll(" data-blocked-event=\"$1\"=");
        out = out.replaceAll("(?i)javascript:", "");
        return out.trim();
    }

    private static JSONObject toJsonObject(Object layoutJson) {
        if (layoutJson instanceof JSONObject jsonObject) {
            return jsonObject;
        }
        if (layoutJson instanceof String str) {
            if (!JSONUtil.isTypeJSONObject(str)) {
                throw new ServiceException(OaErrorCodes.LAYOUT_JSON_INVALID);
            }
            return JSONUtil.parseObj(str);
        }
        return JSONUtil.parseObj(JSONUtil.toJsonStr(layoutJson));
    }

    private static String renderBlock(JSONObject block) {
        return renderBlock(block, false);
    }

    private static String renderBlock(JSONObject block, boolean skeletonMode) {
        String type = block.getStr("type");
        String styleAttr = inlineStyle(block.getJSONObject("styles"));
        return switch (type) {
            case "heading" -> {
                int level = Math.min(6, Math.max(1, block.getInt("level", 2)));
                String text = skeletonMode && StrUtil.isBlank(block.getStr("text"))
                        ? "标题样式" : block.getStr("text");
                yield "<h" + level + styleAttr + ">" + escapeText(text) + "</h" + level + ">";
            }
            case "paragraph" -> {
                String align = block.getStr("align", "left");
                String style = styleAttr.isEmpty()
                        ? " style=\"text-align:" + escapeAttr(align) + "\""
                        : styleAttr.replace("style=\"", "style=\"text-align:" + escapeAttr(align) + ";");
                yield "<p" + style + ">"
                        + renderInlineChildren(block.getJSONArray("children")) + "</p>";
            }
            case "image" -> {
                String src = escapeAttr(block.getStr("src", ""));
                String width = escapeAttr(block.getStr("width", "100%"));
                if (skeletonMode && StrUtil.isBlank(src)) {
                    yield "<p" + styleAttr + "><span class=\"layout-placeholder\">图片框</span></p>";
                }
                yield "<p" + styleAttr + "><img src=\"" + src + "\" style=\"width:" + width + ";max-width:100%;height:auto;\" alt=\"\"/></p>";
            }
            case "quote" -> {
                String text = skeletonMode && StrUtil.isBlank(block.getStr("text"))
                        ? "引用样式" : block.getStr("text");
                yield "<blockquote" + styleAttr + ">" + escapeText(text) + "</blockquote>";
            }
            case "divider" -> "<hr" + styleAttr + "/>";
            case "fixed" -> "<div class=\"layout-fixed\"" + styleAttr + ">装饰区块</div>";
            case "list" -> {
                boolean ordered = block.getBool("ordered", false);
                String tag = ordered ? "ol" : "ul";
                JSONArray items = block.getJSONArray("items");
                StringBuilder list = new StringBuilder("<" + tag + styleAttr + ">");
                if (items != null) {
                    for (int i = 0; i < items.size(); i++) {
                        list.append("<li>").append(escapeText(items.getStr(i))).append("</li>");
                    }
                }
                list.append("</").append(tag).append(">");
                yield list.toString();
            }
            default -> "";
        };
    }

    private static String inlineStyle(JSONObject styles) {
        if (styles == null || styles.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder(" style=\"");
        for (Map.Entry<String, Object> e : styles.entrySet()) {
            String cssKey = camelToKebab(e.getKey());
            sb.append(cssKey).append(":").append(escapeAttr(String.valueOf(e.getValue()))).append(";");
        }
        sb.append("\"");
        return sb.toString();
    }

    private static String camelToKebab(String key) {
        return key.replaceAll("([a-z])([A-Z])", "$1-$2").toLowerCase();
    }

    private static String renderInlineChildren(JSONArray children) {
        if (children == null || children.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < children.size(); i++) {
            JSONObject child = children.getJSONObject(i);
            if (child == null) {
                continue;
            }
            String text = escapeText(child.getStr("text", ""));
            if (child.getBool("bold", false)) {
                text = "<strong>" + text + "</strong>";
            }
            if (child.getBool("italic", false)) {
                text = "<em>" + text + "</em>";
            }
            sb.append(text);
        }
        return sb.toString();
    }

    private static JSONObject paragraphBlock(String text) {
        return paragraphBlock(text, null);
    }

    private static JSONObject paragraphBlock(String text, String htmlSeg) {
        JSONObject block = new JSONObject();
        block.set("type", "paragraph");
        block.set("align", "left");
        JSONArray children = new JSONArray();
        JSONObject child = new JSONObject();
        child.set("text", StrUtil.blankToDefault(text, ""));
        child.set("bold", false);
        child.set("italic", false);
        children.add(child);
        block.set("children", children);
        if (htmlSeg != null) {
            attachInlineStyles(block, htmlSeg);
        }
        return block;
    }

    private static List<String> splitHtmlSegments(String html) {
        List<String> segments = new ArrayList<>();
        String remaining = html;
        Pattern blockTag = Pattern.compile("(?is)(<(h[1-6]|p|blockquote|hr|img|ul|ol)[^>]*>.*?</\\2>|</?(hr|img)[^>]*/?>)");
        while (StrUtil.isNotBlank(remaining)) {
            var matcher = blockTag.matcher(remaining);
            if (matcher.find()) {
                if (matcher.start() > 0) {
                    String before = remaining.substring(0, matcher.start()).trim();
                    if (StrUtil.isNotBlank(stripTags(before))) {
                        segments.add(before);
                    }
                }
                segments.add(matcher.group());
                remaining = remaining.substring(matcher.end());
            } else {
                if (StrUtil.isNotBlank(stripTags(remaining))) {
                    segments.add(remaining.trim());
                }
                break;
            }
        }
        return segments;
    }

    private static JSONObject segmentToBlock(String seg) {
        if (StrUtil.isBlank(seg)) {
            return null;
        }
        String lower = seg.toLowerCase();
        if (lower.startsWith("<hr")) {
            JSONObject block = new JSONObject();
            block.set("type", "divider");
            return block;
        }
        if (ReUtil.isMatch("(?is)<h([1-6])[^>]*>.*</h\\1>", seg)) {
            int level = Integer.parseInt(ReUtil.getGroup1("(?is)<h([1-6])", seg));
            JSONObject block = new JSONObject();
            block.set("type", "heading");
            block.set("level", level);
            block.set("text", stripTags(seg));
            attachInlineStyles(block, seg);
            return block;
        }
        if (lower.contains("<blockquote")) {
            JSONObject block = new JSONObject();
            block.set("type", "quote");
            block.set("text", stripTags(seg));
            attachInlineStyles(block, seg);
            return block;
        }
        if (lower.contains("<img")) {
            String src = ReUtil.getGroup1("(?is)src=[\"']([^\"']+)[\"']", seg);
            JSONObject block = new JSONObject();
            block.set("type", "image");
            block.set("src", StrUtil.blankToDefault(src, ""));
            block.set("width", parseImageWidthFromSegment(seg));
            return block;
        }
        if (lower.contains("<ul") || lower.contains("<ol")) {
            JSONObject block = new JSONObject();
            block.set("type", "list");
            block.set("ordered", lower.contains("<ol"));
            JSONArray items = new JSONArray();
            for (String li : ReUtil.findAllGroup1("(?is)<li[^>]*>(.*?)</li>", seg)) {
                items.add(stripTags(li));
            }
            block.set("items", items);
            return block;
        }
        return paragraphBlock(stripTags(seg), seg);
    }

    private static void attachInlineStyles(JSONObject block, String htmlSeg) {
        String styleAttr = ReUtil.getGroup1("(?is)\\bstyle=[\"']([^\"']*)[\"']", htmlSeg);
        if (StrUtil.isBlank(styleAttr)) {
            return;
        }
        JSONObject styles = new JSONObject();
        for (String decl : styleAttr.split(";")) {
            String[] kv = decl.split(":", 2);
            if (kv.length == 2 && StrUtil.isNotBlank(kv[0])) {
                styles.set(kebabToCamel(kv[0].trim()), kv[1].trim());
            }
        }
        if (!styles.isEmpty()) {
            block.set("styles", styles);
        }
    }

    private static String kebabToCamel(String kebab) {
        StringBuilder sb = new StringBuilder();
        boolean upper = false;
        for (char c : kebab.toCharArray()) {
            if (c == '-') {
                upper = true;
            } else if (upper) {
                sb.append(Character.toUpperCase(c));
                upper = false;
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    private static String stripTags(String html) {
        return StrUtil.trim(ReUtil.replaceAll(html, "<[^>]+>", " ")
                .replaceAll("\\s+", " "));
    }

    private static String parseImageWidthFromSegment(String seg) {
        String widthAttr = ReUtil.getGroup1("(?is)\\bwidth=[\"']([^\"']+)[\"']", seg);
        if (StrUtil.isNotBlank(widthAttr)) {
            return widthAttr;
        }
        String dataW = ReUtil.getGroup1("(?is)\\bdata-w=[\"']([^\"']+)[\"']", seg);
        if (StrUtil.isNotBlank(dataW)) {
            return dataW;
        }
        String styleAttr = StrUtil.blankToDefault(ReUtil.getGroup1("(?is)\\bstyle=[\"']([^\"']*)[\"']", seg), "");
        String widthStyle = ReUtil.getGroup1("(?is)(?:^|;)\\s*width\\s*:\\s*([^;]+)", styleAttr);
        if (StrUtil.isNotBlank(widthStyle)) {
            return widthStyle.trim();
        }
        String maxWidthStyle = ReUtil.getGroup1("(?is)(?:^|;)\\s*max-width\\s*:\\s*([^;]+)", styleAttr);
        if (StrUtil.isNotBlank(maxWidthStyle)) {
            return maxWidthStyle.trim();
        }
        return "100%";
    }

    private static String escapeText(String text) {
        return EscapeUtil.escapeHtml4(StrUtil.nullToEmpty(text));
    }

    private static String escapeAttr(String text) {
        return EscapeUtil.escapeHtml4(StrUtil.nullToEmpty(text));
    }
}
