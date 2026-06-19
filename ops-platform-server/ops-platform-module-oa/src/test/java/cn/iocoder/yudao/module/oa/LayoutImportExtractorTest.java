package cn.iocoder.yudao.module.oa;

import cn.iocoder.yudao.module.oa.service.content.LayoutMergeService;
import cn.iocoder.yudao.module.oa.util.LayoutImportExtractor;
import cn.iocoder.yudao.module.oa.util.LayoutSchemaHelper;
import cn.iocoder.yudao.module.oa.util.WechatArticleHtmlFetcher;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LayoutImportExtractorTest {

    private static final String WECHAT_LIKE_HTML = """
            <style>.rich_media_content p{color:#333;font-size:16px;line-height:1.75;}</style>
            <section style="background:#f5f5f5;padding:12px;border-radius:8px;">
              <h2 style="font-size:22px;color:#07c160;font-weight:bold;">赛事战报标题</h2>
              <p style="color:#333;font-size:16px;line-height:1.75;">第一段正文内容，含 inline 样式。</p>
              <hr style="border-color:#e5e5e5;margin:24px 0;"/>
              <blockquote style="border-left:4px solid #07c160;padding:12px;">引用块样式</blockquote>
              <p style="color:#666;">第二段正文。</p>
            </section>
            """;

    @Test
    @DisplayName("import extract: previewHtml preserves inline styles")
    void extractPreservesInlineStyles() {
        LayoutImportExtractor.ExtractResult result = LayoutImportExtractor.extract(WECHAT_LIKE_HTML);

        assertNotNull(result.getPreviewHtml());
        assertTrue(result.getPreviewHtml().contains("color:#07c160"));
        assertTrue(result.getPreviewHtml().contains("border-left:4px solid #07c160"));
        assertEquals("fullHtml", result.getExtractionReport().getStr("previewMode"));
        assertTrue(result.getExtractionReport().getInt("inlineStyleCount", 0) >= 4);
        assertNotNull(result.getLayoutSchema());
        assertEquals(2, result.getLayoutSchema().getInt("version"));
    }

    @Test
    @DisplayName("import extract: merge preserves body text")
    void mergePreservesBody() {
        LayoutImportExtractor.ExtractResult result = LayoutImportExtractor.extract(WECHAT_LIKE_HTML);
        String body = "用户标题\n\n用户第一段\n\n用户第二段";
        LayoutMergeService mergeService = new LayoutMergeService();
        LayoutMergeService.MergeResult merged = mergeService.merge(body, null, result.getLayoutSchema());

        assertEquals(body, LayoutSchemaHelper.extractTextFromLayout(merged.getLayoutJson()));
        assertFalse(merged.getLayoutHtml().contains("赛事战报标题"));
        assertTrue(merged.getLayoutHtml().contains("用户第一段"));
    }

    @Test
    @DisplayName("sanitize strips script and iframe")
    void sanitizeStripsDangerousTags() {
        String dirty = "<p>ok</p><script>alert(1)</script><iframe src=\"x\"></iframe>";
        String clean = LayoutImportExtractor.sanitizeImportHtml(dirty);
        assertTrue(clean.contains("ok"));
        assertFalse(clean.toLowerCase().contains("<script"));
        assertFalse(clean.toLowerCase().contains("<iframe"));
    }

    @Test
    @DisplayName("normalize removes WeChat hidden js_content styles")
    void normalizeRemovesHiddenStyles() {
        String hidden = "<div id=\"js_content\" style=\"visibility: hidden; opacity: 0;\"><p>正文</p></div>";
        String normalized = LayoutImportExtractor.normalizeWechatPreviewHtml(hidden);
        assertFalse(normalized.contains("visibility: hidden"));
        assertFalse(normalized.contains("opacity: 0"));
        assertTrue(normalized.contains("正文"));

        LayoutImportExtractor.ExtractResult result = LayoutImportExtractor.extract(hidden);
        assertTrue(result.getPreviewHtml().contains("正文"));
        assertFalse(result.getPreviewHtml().contains("visibility: hidden"));
    }

    @Test
    @DisplayName("fetcher extracts js_content with nested divs")
    void fetcherExtractsNestedContent() {
        String page = """
                <html><body>
                <div id="js_content" style="visibility: hidden; opacity: 0;">
                  <section><p>第一段</p><div><p>嵌套</p></div></section>
                </div>
                </body></html>
                """;
        String article = WechatArticleHtmlFetcher.extractArticleHtml(page);
        assertTrue(article.contains("第一段"));
        assertTrue(article.contains("嵌套"));
        assertFalse(article.contains("visibility: hidden"));
    }
}
