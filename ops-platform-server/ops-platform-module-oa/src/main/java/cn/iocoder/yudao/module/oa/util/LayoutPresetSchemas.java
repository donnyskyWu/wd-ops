package cn.iocoder.yudao.module.oa.util;

import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;

/**
 * Flyway seed SSOT for PRESET layout_schema (SEED-M2-公推预置模板草案).
 */
public final class LayoutPresetSchemas {

    private LayoutPresetSchemas() {
    }

    public static JSONObject preset01() {
        return build("blocks", blocks(
                heading(2, "heading2"),
                slot("quote", "quote", false),
                slotRepeat("paragraph", "paragraph"),
                divider(),
                slotRepeat("paragraph", "paragraph")
        ));
    }

    public static JSONObject preset02() {
        JSONObject h = heading(2, "heading2");
        h.set("align", "center");
        return build("blocks", blocks(
                h,
                slot("quote", "quote", false),
                slotRepeat("paragraph", "paragraph"),
                frameImage(),
                divider(),
                slotParagraphMax(2)
        ));
    }

    public static JSONObject preset05() {
        return build("blocks", blocks(
                heading(2, "heading2"),
                fixed("score-highlight", "quote"),
                slotRepeat("paragraph", "paragraph"),
                slot("quote", "quote", true),
                frameImage()
        ));
    }

    public static JSONObject preset06() {
        JSONObject section = new JSONObject();
        section.set("type", "section");
        section.set("repeat", true);
        section.set("maxRepeat", 10);
        JSONArray children = new JSONArray();
        children.add(heading(3, "heading3"));
        JSONObject para = slot("paragraph", "paragraph", false);
        children.add(para);
        section.set("children", children);
        return build("blocks", blocks(
                heading(2, "heading2"),
                section,
                divider()
        ));
    }

    public static JSONObject preset08() {
        JSONObject h = heading(2, "heading2");
        h.set("align", "center");
        JSONObject q = slot("quote", "quote", false);
        q.set("align", "center");
        JSONObject p = slot("paragraph", "paragraph", false);
        p.set("maxRepeat", 2);
        return build("blocks", blocks(
                h,
                q,
                p,
                divider(),
                fixed("brand-footer", "paragraph")
        ));
    }

    /**
     * 图文混排：标题 + 导语 + 图 + 正文循环 + 图 + 正文 + 分隔 + 收尾（典型公众号结构）
     */
    public static JSONObject presetImageTextMixed() {
        return build("blocks", blocks(
                heading(2, "heading2"),
                slot("paragraph", "paragraph", false),
                frameImage(),
                slotRepeat("paragraph", "paragraph"),
                frameImage(),
                slotRepeat("paragraph", "paragraph"),
                divider(),
                slotParagraphMax(2)
        ));
    }

    private static JSONObject build(String blocksKey, JSONArray blocks) {
        JSONObject schema = LayoutSchemaHelper.emptySchema();
        schema.set(blocksKey, blocks);
        return schema;
    }

    private static JSONArray blocks(JSONObject... items) {
        JSONArray arr = new JSONArray();
        for (JSONObject item : items) {
            arr.add(item);
        }
        return arr;
    }

    private static JSONObject heading(int level, String styleRef) {
        JSONObject h = new JSONObject();
        h.set("type", "heading");
        h.set("level", level);
        h.set("styleRef", styleRef);
        h.set("slotKind", "heading");
        return h;
    }

    private static JSONObject slot(String slotKind, String styleRef, boolean optional) {
        JSONObject s = new JSONObject();
        s.set("type", "slot");
        s.set("slotKind", slotKind);
        s.set("styleRef", styleRef);
        if (optional) {
            s.set("optional", true);
        }
        return s;
    }

    private static JSONObject slotRepeat(String slotKind, String styleRef) {
        JSONObject s = slot(slotKind, styleRef, false);
        s.set("repeat", true);
        return s;
    }

    private static JSONObject slotParagraphMax(int max) {
        JSONObject s = slotRepeat("paragraph", "paragraph");
        s.set("maxRepeat", max);
        return s;
    }

    private static JSONObject divider() {
        JSONObject d = new JSONObject();
        d.set("type", "divider");
        d.set("styleRef", "divider");
        return d;
    }

    private static JSONObject frameImage() {
        JSONObject f = new JSONObject();
        f.set("type", "frame");
        f.set("slotKind", "image");
        f.set("styleRef", "image");
        f.set("optional", true);
        return f;
    }

    private static JSONObject fixed(String fixedType, String styleRef) {
        JSONObject f = new JSONObject();
        f.set("type", "fixed");
        f.set("fixedType", fixedType);
        f.set("styleRef", styleRef);
        return f;
    }
}
