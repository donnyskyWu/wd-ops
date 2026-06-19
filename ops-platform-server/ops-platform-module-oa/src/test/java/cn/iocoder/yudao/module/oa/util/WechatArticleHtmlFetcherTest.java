package cn.iocoder.yudao.module.oa.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WechatArticleHtmlFetcherTest {

    static boolean realMhtmlExists() {
        return Files.exists(Path.of("../../../../公众号.mhtml"))
                || Files.exists(Path.of("../../../../test-article.mhtml"));
    }

    static Path realMhtmlPath() {
        Path testCopy = Path.of("../../../../test-article.mhtml");
        if (Files.exists(testCopy)) {
            return testCopy;
        }
        return Path.of("../../../../公众号.mhtml");
    }

    private static final String SAMPLE_MHTML = """
            From: <Saved by Blink>
            MIME-Version: 1.0
            Content-Type: multipart/related; boundary="----BOUNDARY----"

            ------BOUNDARY----
            Content-Type: text/html
            Content-Transfer-Encoding: quoted-printable

            <!DOCTYPE html><html><body><div id=3D"js_content"><p>=E5=A4=8F=E6=9C=9D=E6=A0=87=E9=A2=98</p></div></body></html>
            ------BOUNDARY------
            """;

    @Test
    @DisplayName("quoted-printable decode")
    void decodeQuotedPrintableText() {
        String decoded = WechatArticleHtmlFetcher.decodeQuotedPrintable(
                "<div id=3D\"js_content\"><p>=E5=A4=8F=E6=9C=9D=E6=A0=87=E9=A2=98</p></div>");
        assertTrue(decoded.contains("夏朝标题"));
        assertTrue(decoded.contains("id=\"js_content\""));
    }

    @Test
    @DisplayName("mhtml: plain html part")
    void parseMhtmlPlainHtml() throws Exception {
        String plain = """
                Content-Type: multipart/related; boundary="BOUND"

                --BOUND
                Content-Type: text/html

                <html><body><div id="js_content"><p>夏朝标题</p></div></body></html>
                --BOUND--
                """;
        String html = WechatArticleHtmlFetcher.parseMhtml(
                new java.io.ByteArrayInputStream(plain.getBytes(StandardCharsets.UTF_8)));
        assertTrue(html.contains("夏朝标题"));
    }

    @Test
    @DisplayName("mhtml: quoted-printable html part")
    void parseMhtmlExtractsContent() throws Exception {
        String html = WechatArticleHtmlFetcher.parseMhtml(
                new java.io.ByteArrayInputStream(SAMPLE_MHTML.getBytes(StandardCharsets.UTF_8)));
        assertTrue(html.contains("夏朝标题"));
        assertFalse(html.contains("visibility: hidden"));
    }

    @Test
    @EnabledIf("realMhtmlExists")
    @DisplayName("integration: parse workspace mhtml bytes")
    void parseRealMhtmlBytes() throws Exception {
        byte[] bytes = Files.readAllBytes(realMhtmlPath());
        String html = WechatArticleHtmlFetcher.parseMhtml(bytes);
        assertTrue(html.length() > 500, "preview length=" + html.length());
    }

    @Test
    @EnabledIf("realMhtmlExists")
    @DisplayName("integration: parse workspace 公众号.mhtml")
    void parseRealMhtmlFile() throws Exception {
        try (var in = Files.newInputStream(realMhtmlPath())) {
            String html = WechatArticleHtmlFetcher.parseMhtml(in);
            assertTrue(html.length() > 500, "preview length=" + html.length());
            assertFalse(html.contains("visibility: hidden"));
        }
    }

    @Test
    @DisplayName("extractArticleHtml prefers ProseMirror when js_content empty")
    void extractProseMirrorEditorContent() {
        String page = """
                <div id="js_content"></div>
                <div class="ProseMirror"><p>编辑器正文</p><p>第二段</p></div>
                """;
        String article = WechatArticleHtmlFetcher.extractArticleHtml(page);
        assertTrue(article.contains("编辑器正文"));
        assertTrue(article.contains("第二段"));
    }
}
