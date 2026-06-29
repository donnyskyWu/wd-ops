package cn.iocoder.yudao.module.oa;

import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.iocoder.yudao.module.oa.service.content.LayoutMergeService;
import cn.iocoder.yudao.module.oa.util.LayoutSchemaHelper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 排版对比 / 一键排版：模板 merge 须保留正文图片（TypesettingPanel → typeset API）。
 */
class LayoutMergeImageTest {

    @Test
    @DisplayName("splitMergeSegments keeps img-only paragraph")
    void splitMergeSegmentsPreservesImageOnlyBlock() {
        List<String> segments = LayoutSchemaHelper.splitMergeSegments(
                "<p>导语文字</p><p><img src=\"https://example.com/a.jpg\" alt=\"\"/></p>");

        assertEquals(2, segments.size());
        assertEquals("导语文字", segments.get(0));
        assertTrue(LayoutSchemaHelper.isImageSegment(segments.get(1)));
        assertEquals("https://example.com/a.jpg", LayoutSchemaHelper.extractImageSrc(segments.get(1)));
    }

    @Test
    @DisplayName("template merge renders image segment into layoutHtml")
    void mergeRendersImageInLayoutHtml() {
        JSONObject schema = imageParagraphSchema();
        String mergeBody = String.join("\n\n", LayoutSchemaHelper.splitMergeSegments(
                "<p>正文段落</p><p><img src=\"https://example.com/photo.png\"/></p>"));

        LayoutMergeService mergeService = new LayoutMergeService();
        LayoutMergeService.MergeResult merged = mergeService.merge(mergeBody, null, schema);

        assertTrue(merged.getLayoutHtml().contains("https://example.com/photo.png"),
                () -> "layoutHtml should contain image src: " + merged.getLayoutHtml());
        assertTrue(merged.getLayoutHtml().contains("正文段落"));
    }

    private static JSONObject imageParagraphSchema() {
        JSONObject schema = LayoutSchemaHelper.emptySchema();
        JSONArray blocks = new JSONArray();

        JSONObject paragraphSlot = new JSONObject();
        paragraphSlot.set("type", "slot");
        paragraphSlot.set("slotKind", "paragraph");
        paragraphSlot.set("styleRef", "paragraph");
        paragraphSlot.set("repeat", true);
        blocks.add(paragraphSlot);

        JSONObject frame = new JSONObject();
        frame.set("type", "frame");
        frame.set("slotKind", "image");
        frame.set("styleRef", "image");
        frame.set("optional", true);
        blocks.add(frame);

        schema.set("blocks", blocks);
        return schema;
    }
}
