package cn.iocoder.yudao.module.oa.util;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import cn.iocoder.yudao.framework.common.exception.OaErrorCodes;
import cn.iocoder.yudao.framework.common.exception.ServiceException;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Layout schema v2: validate, extract from HTML/v1, skeleton preview (ADR-020).
 */
public final class LayoutSchemaHelper {

    private static final Set<String> SCHEMA_BLOCK_TYPES = Set.of(
            "heading", "slot", "divider", "frame", "fixed", "section");
    private static final Set<String> SLOT_KINDS = Set.of(
            "heading", "paragraph", "quote", "list", "image");
    private static final String PLACEHOLDER_HEADING = "标题样式";
    private static final String PLACEHOLDER_PARAGRAPH = "正文段落样式";
    private static final String PLACEHOLDER_QUOTE = "引用样式";
    private static final String PLACEHOLDER_LIST = "列表样式";
    private static final String PLACEHOLDER_IMAGE = "图片框";
    private static final String PLACEHOLDER_FIXED = "装饰区块";

    private LayoutSchemaHelper() {
    }

    public static JSONObject defaultGlobalStyles() {
        JSONObject styles = new JSONObject();
        styles.set("heading2", styleMap("18px", "bold", "#1a1a1a", "1.4"));
        styles.set("heading3", styleMap("16px", "bold", "#333333", "1.4"));
        styles.set("paragraph", styleMap("16px", null, "#333333", "1.75"));
        styles.set("quote", quoteStyle());
        styles.set("divider", dividerStyle());
        styles.set("image", imageStyle());
        styles.set("list", styleMap("16px", null, "#333333", "1.75"));
        return styles;
    }

    public static JSONObject emptySchema() {
        JSONObject schema = new JSONObject();
        schema.set("version", 2);
        schema.set("globalStyles", defaultGlobalStyles());
        schema.set("blocks", new JSONArray());
        return schema;
    }

    public static void validateLayoutSchema(Object layoutSchema) {
        if (layoutSchema == null) {
            throw new ServiceException(OaErrorCodes.LAYOUT_SCHEMA_INVALID);
        }
        JSONObject schema = toJsonObject(layoutSchema);
        if (schema.getInt("version", 0) != 2) {
            throw new ServiceException(OaErrorCodes.LAYOUT_SCHEMA_INVALID);
        }
        JSONArray blocks = schema.getJSONArray("blocks");
        if (blocks == null) {
            throw new ServiceException(OaErrorCodes.LAYOUT_SCHEMA_INVALID);
        }
        for (int i = 0; i < blocks.size(); i++) {
            JSONObject block = blocks.getJSONObject(i);
            if (block == null) {
                throw new ServiceException(OaErrorCodes.LAYOUT_SCHEMA_INVALID);
            }
            String type = block.getStr("type");
            if (!SCHEMA_BLOCK_TYPES.contains(type)) {
                throw new ServiceException(OaErrorCodes.LAYOUT_SCHEMA_INVALID);
            }
            if ("slot".equals(type) || "frame".equals(type)) {
                String slotKind = block.getStr("slotKind");
                if (!SLOT_KINDS.contains(slotKind)) {
                    throw new ServiceException(OaErrorCodes.LAYOUT_SCHEMA_INVALID);
                }
            }
        }
    }

    public static String toJsonString(Object layoutSchema) {
        validateLayoutSchema(layoutSchema);
        return JSONUtil.toJsonStr(toJsonObject(layoutSchema));
    }

    public static JSONObject extractLayoutSchemaFromHtml(String html) {
        JSONObject v1 = LayoutJsonHelper.parseHtmlToLayout(html);
        return extractLayoutSchemaFromLayoutJson(v1);
    }

    public static JSONObject extractLayoutSchemaFromLayoutJson(Object layoutJson) {
        JSONObject v1 = toJsonObject(layoutJson);
        JSONObject schema = emptySchema();
        JSONArray schemaBlocks = new JSONArray();
        JSONArray v1Blocks = v1.getJSONArray("blocks");
        if (v1Blocks == null) {
            schema.set("blocks", schemaBlocks);
            return schema;
        }
        boolean hasParagraphRepeat = false;
        for (int i = 0; i < v1Blocks.size(); i++) {
            JSONObject block = v1Blocks.getJSONObject(i);
            if (block == null) {
                continue;
            }
            String type = block.getStr("type");
            JSONObject schemaBlock = switch (type) {
                case "heading" -> {
                    JSONObject h = new JSONObject();
                    h.set("type", "heading");
                    h.set("level", block.getInt("level", 2));
                    h.set("styleRef", "heading" + block.getInt("level", 2));
                    h.set("slotKind", "heading");
                    yield h;
                }
                case "quote" -> slotBlock("quote", "quote", false, false);
                case "divider" -> {
                    JSONObject d = new JSONObject();
                    d.set("type", "divider");
                    d.set("styleRef", "divider");
                    yield d;
                }
                case "image" -> {
                    JSONObject f = new JSONObject();
                    f.set("type", "frame");
                    f.set("slotKind", "image");
                    f.set("styleRef", "image");
                    f.set("optional", true);
                    yield f;
                }
                case "list" -> slotBlock("list", "list", true, false);
                case "paragraph" -> {
                    if (!hasParagraphRepeat) {
                        hasParagraphRepeat = true;
                        yield slotBlock("paragraph", "paragraph", true, true);
                    }
                    yield null;
                }
                default -> null;
            };
            if (schemaBlock != null) {
                schemaBlocks.add(schemaBlock);
            }
        }
        if (schemaBlocks.isEmpty()) {
            schemaBlocks.add(slotBlock("paragraph", "paragraph", true, true));
        }
        schema.set("blocks", schemaBlocks);
        return schema;
    }

    public static JSONObject buildExtractionReport(JSONObject v1, JSONObject schema) {
        JSONObject report = new JSONObject();
        int stripped = countTextChars(v1);
        report.set("strippedCharCount", stripped);
        report.set("slotCount", schema.getJSONArray("blocks").size());
        report.set("warnings", stripped > 500
                ? List.of("样本正文已剥离，仅保留版式骨架")
                : List.of());
        return report;
    }

    public static String renderSchemaPreview(Object layoutSchema) {
        validateLayoutSchema(layoutSchema);
        JSONObject schema = toJsonObject(layoutSchema);
        JSONObject previewDoc = schemaToPreviewDocument(schema);
        return LayoutJsonHelper.renderHtml(previewDoc, true);
    }

    public static JSONObject schemaToPreviewDocument(JSONObject schema) {
        JSONObject doc = new JSONObject();
        doc.set("version", 2);
        JSONArray out = new JSONArray();
        JSONObject globalStyles = schema.getJSONObject("globalStyles");
        JSONArray blocks = schema.getJSONArray("blocks");
        if (blocks != null) {
            for (int i = 0; i < blocks.size(); i++) {
                appendPreviewBlock(out, blocks.getJSONObject(i), globalStyles);
            }
        }
        doc.set("blocks", out);
        return doc;
    }

    public static JSONObject resolveSchema(WechatLayoutTemplateFields template) {
        if (template.getSchemaVersion() != null && template.getSchemaVersion() >= 2
                && StrUtil.isNotBlank(template.getLayoutSchema())) {
            return JSONUtil.parseObj(template.getLayoutSchema());
        }
        if (StrUtil.isNotBlank(template.getLayoutJson())) {
            return extractLayoutSchemaFromLayoutJson(JSONUtil.parseObj(template.getLayoutJson()));
        }
        return emptySchema();
    }

    private static void appendPreviewBlock(JSONArray out, JSONObject block, JSONObject globalStyles) {
        if (block == null) {
            return;
        }
        String type = block.getStr("type");
        String styleRef = block.getStr("styleRef", "paragraph");
        JSONObject styles = resolveStyles(globalStyles, styleRef);
        switch (type) {
            case "heading" -> {
                JSONObject h = new JSONObject();
                h.set("type", "heading");
                h.set("level", block.getInt("level", 2));
                h.set("text", PLACEHOLDER_HEADING);
                h.set("styles", styles);
                out.add(h);
            }
            case "slot" -> appendSlotPreview(out, block, styles);
            case "divider" -> {
                JSONObject d = new JSONObject();
                d.set("type", "divider");
                d.set("styles", styles);
                out.add(d);
            }
            case "frame" -> {
                JSONObject img = new JSONObject();
                img.set("type", "image");
                img.set("src", "");
                img.set("width", "100%");
                img.set("styles", styles);
                img.set("placeholder", PLACEHOLDER_IMAGE);
                out.add(img);
            }
            case "fixed" -> {
                JSONObject fixed = new JSONObject();
                fixed.set("type", "quote");
                fixed.set("text", PLACEHOLDER_FIXED);
                fixed.set("styles", styles);
                out.add(fixed);
            }
            case "section" -> {
                JSONArray children = block.getJSONArray("children");
                if (children != null) {
                    for (int i = 0; i < children.size(); i++) {
                        appendPreviewBlock(out, children.getJSONObject(i), globalStyles);
                    }
                }
            }
            default -> {
            }
        }
    }

    private static void appendSlotPreview(JSONArray out, JSONObject block, JSONObject styles) {
        String slotKind = block.getStr("slotKind", "paragraph");
        boolean repeat = block.getBool("repeat", false);
        int count = repeat ? 2 : 1;
        for (int i = 0; i < count; i++) {
            switch (slotKind) {
                case "heading" -> {
                    JSONObject h = new JSONObject();
                    h.set("type", "heading");
                    h.set("level", block.getInt("level", 3));
                    h.set("text", PLACEHOLDER_HEADING);
                    h.set("styles", styles);
                    out.add(h);
                }
                case "quote" -> {
                    JSONObject q = new JSONObject();
                    q.set("type", "quote");
                    q.set("text", PLACEHOLDER_QUOTE);
                    q.set("styles", styles);
                    out.add(q);
                }
                case "list" -> {
                    JSONObject list = new JSONObject();
                    list.set("type", "list");
                    list.set("ordered", block.getBool("ordered", false));
                    list.set("items", List.of(PLACEHOLDER_LIST, PLACEHOLDER_LIST));
                    list.set("styles", styles);
                    out.add(list);
                }
                case "paragraph" -> {
                    JSONObject p = paragraphInstance(PLACEHOLDER_PARAGRAPH, styles, block.getStr("align", "left"));
                    out.add(p);
                }
                default -> {
                }
            }
        }
    }

    public static JSONObject paragraphInstance(String text, JSONObject styles, String align) {
        JSONObject block = new JSONObject();
        block.set("type", "paragraph");
        block.set("align", align);
        block.set("styles", styles);
        JSONArray children = new JSONArray();
        JSONObject child = new JSONObject();
        child.set("text", text);
        child.set("bold", false);
        child.set("italic", false);
        children.add(child);
        block.set("children", children);
        return block;
    }

    public static JSONObject headingInstance(int level, String text, JSONObject styles) {
        JSONObject block = new JSONObject();
        block.set("type", "heading");
        block.set("level", level);
        block.set("text", text);
        block.set("styles", styles);
        return block;
    }

    public static JSONObject quoteInstance(String text, JSONObject styles) {
        JSONObject block = new JSONObject();
        block.set("type", "quote");
        block.set("text", text);
        block.set("styles", styles);
        return block;
    }

    public static JSONObject dividerInstance(JSONObject styles) {
        JSONObject block = new JSONObject();
        block.set("type", "divider");
        block.set("styles", styles);
        return block;
    }

    public static JSONObject fixedInstance(String fixedType, JSONObject styles) {
        JSONObject block = new JSONObject();
        block.set("type", "fixed");
        block.set("fixedType", fixedType);
        block.set("styles", styles);
        return block;
    }

    public static JSONObject resolveStyles(JSONObject globalStyles, String styleRef) {
        if (globalStyles == null || StrUtil.isBlank(styleRef)) {
            return new JSONObject();
        }
        JSONObject ref = globalStyles.getJSONObject(styleRef);
        return ref != null ? ref : new JSONObject();
    }

    public static List<String> splitBody(String body) {
        if (StrUtil.isBlank(body)) {
            return List.of();
        }
        String normalized = body.replace("\r\n", "\n").trim();
        String[] parts = normalized.split("\n\n+");
        if (parts.length == 1) {
            parts = normalized.split("\n");
        }
        List<String> segments = new ArrayList<>();
        for (String part : parts) {
            String trimmed = part.trim();
            if (StrUtil.isNotBlank(trimmed)) {
                segments.add(trimmed);
            }
        }
        return segments;
    }

    public static String extractTextFromLayout(Object layoutJson) {
        if (layoutJson == null) {
            return "";
        }
        JSONObject doc = toJsonObject(layoutJson);
        JSONArray blocks = doc.getJSONArray("blocks");
        if (blocks == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < blocks.size(); i++) {
            JSONObject block = blocks.getJSONObject(i);
            if (block == null) {
                continue;
            }
            String text = extractBlockText(block);
            if (StrUtil.isNotBlank(text)) {
                if (!sb.isEmpty()) {
                    sb.append("\n\n");
                }
                sb.append(text);
            }
        }
        return sb.toString();
    }

    private static String extractBlockText(JSONObject block) {
        return switch (block.getStr("type")) {
            case "heading", "quote" -> block.getStr("text", "");
            case "paragraph" -> {
                JSONArray children = block.getJSONArray("children");
                if (children == null || children.isEmpty()) {
                    yield "";
                }
                yield children.getJSONObject(0).getStr("text", "");
            }
            case "list" -> {
                JSONArray items = block.getJSONArray("items");
                if (items == null) {
                    yield "";
                }
                List<String> lines = new ArrayList<>();
                for (int i = 0; i < items.size(); i++) {
                    lines.add(items.getStr(i));
                }
                yield String.join("\n", lines);
            }
            default -> "";
        };
    }

    private static JSONObject slotBlock(String slotKind, String styleRef, boolean ordered, boolean repeat) {
        JSONObject slot = new JSONObject();
        slot.set("type", "slot");
        slot.set("slotKind", slotKind);
        slot.set("styleRef", styleRef);
        if ("list".equals(slotKind)) {
            slot.set("ordered", ordered);
        }
        if (repeat) {
            slot.set("repeat", true);
        }
        return slot;
    }

    private static JSONObject styleMap(String fontSize, String fontWeight, String color, String lineHeight) {
        JSONObject s = new JSONObject();
        s.set("fontSize", fontSize);
        if (fontWeight != null) {
            s.set("fontWeight", fontWeight);
        }
        s.set("color", color);
        s.set("lineHeight", lineHeight);
        return s;
    }

    private static JSONObject quoteStyle() {
        JSONObject s = new JSONObject();
        s.set("fontSize", "15px");
        s.set("color", "#666666");
        s.set("backgroundColor", "#f7f7f7");
        s.set("borderLeft", "4px solid #07c160");
        s.set("padding", "12px 16px");
        s.set("lineHeight", "1.6");
        return s;
    }

    private static JSONObject dividerStyle() {
        JSONObject s = new JSONObject();
        s.set("borderColor", "#e5e5e5");
        s.set("margin", "24px 0");
        return s;
    }

    private static JSONObject imageStyle() {
        JSONObject s = new JSONObject();
        s.set("width", "100%");
        s.set("borderRadius", "4px");
        return s;
    }

    private static int countTextChars(JSONObject v1) {
        int count = 0;
        JSONArray blocks = v1.getJSONArray("blocks");
        if (blocks == null) {
            return 0;
        }
        for (int i = 0; i < blocks.size(); i++) {
            count += extractBlockText(blocks.getJSONObject(i)).length();
        }
        return count;
    }

    private static JSONObject toJsonObject(Object obj) {
        if (obj instanceof JSONObject jsonObject) {
            return jsonObject;
        }
        if (obj instanceof String str) {
            if (!JSONUtil.isTypeJSONObject(str)) {
                throw new ServiceException(OaErrorCodes.LAYOUT_SCHEMA_INVALID);
            }
            return JSONUtil.parseObj(str);
        }
        return JSONUtil.parseObj(JSONUtil.toJsonStr(obj));
    }

    /** Minimal field bag for schema resolution without DO dependency in util. */
    public interface WechatLayoutTemplateFields {
        Integer getSchemaVersion();
        String getLayoutSchema();
        String getLayoutJson();
    }
}
