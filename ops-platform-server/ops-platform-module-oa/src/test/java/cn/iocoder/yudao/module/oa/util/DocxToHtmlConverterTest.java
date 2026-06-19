package cn.iocoder.yudao.module.oa.util;

import cn.iocoder.yudao.module.oa.util.LayoutImportExtractor.ExtractResult;
import org.apache.poi.xwpf.usermodel.ParagraphAlignment;
import org.apache.poi.xwpf.usermodel.UnderlinePatterns;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DocxToHtmlConverterTest {

    @Test
    @DisplayName("docx convert: preserves bold, color, alignment inline styles")
    void convertsStyledParagraphsToInlineHtml() throws IOException {
        byte[] docx = buildStyledDocx();
        String html = DocxToHtmlConverter.convert(new ByteArrayInputStream(docx));

        assertTrue(html.contains("font-weight:bold"));
        assertTrue(html.contains("color:#FF0000"));
        assertTrue(html.contains("text-align:center"));
        assertTrue(html.contains("text-decoration:underline"));
        assertTrue(html.contains("<h1"));
        assertTrue(html.contains("docx-import"));
        assertTrue(DocxToHtmlConverter.hasExtractableContent(html));
        assertTrue(html.contains("赛事标题"), "Chinese heading must not be JS-escaped");
        assertTrue(html.contains("居中红色加粗下划线正文"), "Chinese body must not be JS-escaped");
        assertFalse(html.contains("\\u"), "must not contain JS unicode escapes");
    }

    @Test
    @DisplayName("docx convert: preserves Chinese through import extractor pipeline")
    void preservesChineseThroughExtractPipeline() throws IOException {
        byte[] docx = buildStyledDocx();
        String html = DocxToHtmlConverter.convert(new ByteArrayInputStream(docx));
        ExtractResult result = LayoutImportExtractor.extract(html);

        assertTrue(result.getPreviewHtml().contains("赛事标题"));
        assertTrue(result.getPreviewHtml().contains("居中红色加粗下划线正文"));
        assertFalse(result.getPreviewHtml().contains("\\u"));
    }

    @Test
    @DisplayName("docx convert: passes through LayoutImportExtractor for preview")
    void extractPipelinePreservesDocxStyles() throws IOException {
        byte[] docx = buildStyledDocx();
        String html = DocxToHtmlConverter.convert(new ByteArrayInputStream(docx));
        ExtractResult result = LayoutImportExtractor.extract(html);

        assertNotNull(result.getPreviewHtml());
        assertTrue(result.getPreviewHtml().contains("color:#FF0000"));
        assertTrue(result.getExtractionReport().getInt("inlineStyleCount", 0) >= 2);
        assertNotNull(result.getLayoutSchema());
    }

    @Test
    @DisplayName("docx convert: empty document has no extractable content")
    void emptyDocxHasNoContent() throws IOException {
        try (XWPFDocument document = new XWPFDocument();
             ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            document.write(baos);
            String html = DocxToHtmlConverter.convert(baos.toByteArray());
            assertFalse(DocxToHtmlConverter.hasExtractableContent(html));
        }
    }

    private static byte[] buildStyledDocx() throws IOException {
        try (XWPFDocument document = new XWPFDocument();
             ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            XWPFParagraph heading = document.createParagraph();
            heading.setStyle("Heading1");
            XWPFRun headingRun = heading.createRun();
            headingRun.setText("赛事标题");
            headingRun.setBold(true);
            headingRun.setFontSize(28);

            XWPFParagraph body = document.createParagraph();
            body.setAlignment(ParagraphAlignment.CENTER);
            XWPFRun bodyRun = body.createRun();
            bodyRun.setText("居中红色加粗下划线正文");
            bodyRun.setBold(true);
            bodyRun.setColor("FF0000");
            bodyRun.setUnderline(UnderlinePatterns.SINGLE);

            document.write(baos);
            return baos.toByteArray();
        }
    }
}
