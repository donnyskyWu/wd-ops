package cn.iocoder.yudao.module.oa.util;

import cn.hutool.core.util.EscapeUtil;
import cn.hutool.core.util.StrUtil;
import org.apache.poi.xwpf.usermodel.IBodyElement;
import org.apache.poi.xwpf.usermodel.ParagraphAlignment;
import org.apache.poi.xwpf.usermodel.UnderlinePatterns;
import org.apache.poi.xwpf.usermodel.XWPFAbstractNum;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFNum;
import org.apache.poi.xwpf.usermodel.XWPFNumbering;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFPicture;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.apache.poi.xwpf.usermodel.XWPFStyle;
import org.apache.poi.xwpf.usermodel.XWPFStyles;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.apache.poi.xwpf.usermodel.XWPFTableCell;
import org.apache.poi.xwpf.usermodel.XWPFTableRow;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTNumPr;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTPPr;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.STNumberFormat;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.util.ArrayDeque;
import java.util.Base64;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Converts Word (.docx) to inline-styled HTML for WeChat layout import preview (ADR-021 subset).
 */
public final class DocxToHtmlConverter {

    private static final int TWIPS_PER_PT = 20;
    private static final String LIST_STYLE = "margin:0 0 12px 18px;padding-left:18px;";

    private DocxToHtmlConverter() {
    }

    public static String convert(InputStream inputStream) throws IOException {
        try (XWPFDocument document = new XWPFDocument(inputStream)) {
            return convertDocument(document);
        }
    }

    public static String convert(byte[] bytes) throws IOException {
        try (XWPFDocument document = new XWPFDocument(new java.io.ByteArrayInputStream(bytes))) {
            return convertDocument(document);
        }
    }

    static String convertDocument(XWPFDocument document) {
        StringBuilder html = new StringBuilder();
        html.append("<section class=\"docx-import\">");
        ListState listState = new ListState();
        Map<BigInteger, Boolean> orderedByNumId = buildOrderedLookup(document);
        for (IBodyElement element : document.getBodyElements()) {
            if (element instanceof XWPFParagraph paragraph) {
                appendParagraph(html, document, paragraph, listState, orderedByNumId);
            } else if (element instanceof XWPFTable table) {
                listState.closeAll(html);
                appendTable(html, document, table, orderedByNumId);
            }
        }
        listState.closeAll(html);
        html.append("</section>");
        return html.toString();
    }

    public static boolean hasExtractableContent(String html) {
        if (StrUtil.isBlank(html)) {
            return false;
        }
        if (html.contains("<img ")) {
            return true;
        }
        return StrUtil.isNotBlank(html.replaceAll("(?s)<[^>]+>", "").trim());
    }

    private static void appendParagraph(StringBuilder html, XWPFDocument document, XWPFParagraph paragraph,
                                        ListState listState, Map<BigInteger, Boolean> orderedByNumId) {
        String text = paragraph.getText();
        if (StrUtil.isBlank(text) && !paragraphHasPicture(paragraph)) {
            return;
        }
        CTNumPr numPr = paragraph.getCTP().getPPr() != null ? paragraph.getCTP().getPPr().getNumPr() : null;
        if (numPr != null && numPr.getNumId() != null) {
            appendListItem(html, paragraph, listState, orderedByNumId, numPr);
            return;
        }
        listState.closeAll(html);
        String tag = resolveBlockTag(document, paragraph);
        String style = buildParagraphStyle(document, paragraph, tag);
        html.append('<').append(tag);
        if (StrUtil.isNotBlank(style)) {
            html.append(" style=\"").append(style).append('"');
        }
        html.append('>');
        appendRuns(html, paragraph);
        html.append("</").append(tag).append('>');
    }

    private static void appendListItem(StringBuilder html, XWPFParagraph paragraph, ListState listState,
                                       Map<BigInteger, Boolean> orderedByNumId, CTNumPr numPr) {
        BigInteger numId = numPr.getNumId().getVal();
        int ilvl = numPr.getIlvl() != null ? numPr.getIlvl().getVal().intValue() : 0;
        boolean ordered = orderedByNumId.getOrDefault(numId, false);
        String listTag = ordered ? "ol" : "ul";
        listState.sync(html, listTag, ilvl);
        html.append("<li>");
        appendRuns(html, paragraph);
        html.append("</li>");
    }

    private static void appendTable(StringBuilder html, XWPFDocument document, XWPFTable table,
                                    Map<BigInteger, Boolean> orderedByNumId) {
        html.append("<table style=\"border-collapse:collapse;width:100%;\">");
        for (XWPFTableRow row : table.getRows()) {
            html.append("<tr>");
            for (XWPFTableCell cell : row.getTableCells()) {
                html.append("<td style=\"border:1px solid #ddd;padding:6px;vertical-align:top;\">");
                ListState cellListState = new ListState();
                for (IBodyElement element : cell.getBodyElements()) {
                    if (element instanceof XWPFParagraph paragraph) {
                        appendParagraph(html, document, paragraph, cellListState, orderedByNumId);
                    }
                }
                cellListState.closeAll(html);
                html.append("</td>");
            }
            html.append("</tr>");
        }
        html.append("</table>");
    }

    private static void appendRuns(StringBuilder html, XWPFParagraph paragraph) {
        for (XWPFRun run : paragraph.getRuns()) {
            for (XWPFPicture picture : run.getEmbeddedPictures()) {
                appendPicture(html, picture);
            }
            String runText = run.text();
            if (StrUtil.isEmpty(runText)) {
                continue;
            }
            String runStyle = buildRunStyle(run);
            if (StrUtil.isBlank(runStyle)) {
                html.append(escapeHtml(runText));
                continue;
            }
            html.append("<span style=\"").append(runStyle).append("\">")
                    .append(escapeHtml(runText))
                    .append("</span>");
        }
    }

    private static void appendPicture(StringBuilder html, XWPFPicture picture) {
        if (picture.getPictureData() == null) {
            return;
        }
        byte[] data = picture.getPictureData().getData();
        if (data == null || data.length == 0) {
            return;
        }
        String mime = StrUtil.blankToDefault(picture.getPictureData().getPackagePart().getContentType(),
                "image/png");
        String base64 = Base64.getEncoder().encodeToString(data);
        html.append("<img src=\"data:").append(mime).append(";base64,").append(base64)
                .append("\" style=\"max-width:100%;height:auto;display:block;margin:8px 0;\" alt=\"\"/>");
    }

    private static boolean paragraphHasPicture(XWPFParagraph paragraph) {
        for (XWPFRun run : paragraph.getRuns()) {
            if (!run.getEmbeddedPictures().isEmpty()) {
                return true;
            }
        }
        return false;
    }

    private static String resolveBlockTag(XWPFDocument document, XWPFParagraph paragraph) {
        if (isQuoteParagraph(document, paragraph)) {
            return "blockquote";
        }
        return switch (resolveHeadingLevel(document, paragraph)) {
            case 1 -> "h1";
            case 2 -> "h2";
            case 3 -> "h3";
            default -> "p";
        };
    }

    private static boolean isQuoteParagraph(XWPFDocument document, XWPFParagraph paragraph) {
        String styleName = resolveStyleName(document, paragraph);
        if (StrUtil.isNotBlank(styleName)) {
            String lower = styleName.toLowerCase(Locale.ROOT);
            if (lower.contains("quote") || lower.contains("引用")) {
                return true;
            }
        }
        if (paragraph.getIndentationLeft() >= 720) {
            return true;
        }
        return false;
    }

    private static int resolveHeadingLevel(XWPFDocument document, XWPFParagraph paragraph) {
        String styleName = resolveStyleName(document, paragraph);
        if (StrUtil.isNotBlank(styleName)) {
            int fromName = headingLevelFromStyleName(styleName);
            if (fromName > 0) {
                return fromName;
            }
        }
        CTPPr pPr = paragraph.getCTP().getPPr();
        if (pPr != null && pPr.getOutlineLvl() != null) {
            int lvl = pPr.getOutlineLvl().getVal().intValue();
            if (lvl >= 0 && lvl <= 2) {
                return lvl + 1;
            }
        }
        if (!paragraph.getRuns().isEmpty()) {
            XWPFRun firstRun = paragraph.getRuns().get(0);
            if (firstRun.getFontSize() > 0) {
                int pt = firstRun.getFontSize() / 2;
                if (pt >= 24) {
                    return 1;
                }
                if (pt >= 18) {
                    return 2;
                }
                if (pt >= 14 && firstRun.isBold()) {
                    return 3;
                }
            }
        }
        return 0;
    }

    private static int headingLevelFromStyleName(String styleName) {
        String normalized = styleName.toLowerCase(Locale.ROOT).replace(" ", "");
        if (normalized.contains("heading1") || normalized.contains("标题1") || normalized.equals("h1")) {
            return 1;
        }
        if (normalized.contains("heading2") || normalized.contains("标题2") || normalized.equals("h2")) {
            return 2;
        }
        if (normalized.contains("heading3") || normalized.contains("标题3") || normalized.equals("h3")) {
            return 3;
        }
        if (normalized.matches("标题[123]")) {
            return Character.getNumericValue(normalized.charAt(normalized.length() - 1));
        }
        return 0;
    }

    private static String resolveStyleName(XWPFDocument document, XWPFParagraph paragraph) {
        String styleId = paragraph.getStyleID();
        if (StrUtil.isBlank(styleId)) {
            styleId = paragraph.getStyle();
        }
        if (StrUtil.isBlank(styleId)) {
            return "";
        }
        if (document == null) {
            return styleId;
        }
        XWPFStyles styles = document.getStyles();
        if (styles == null) {
            return styleId;
        }
        XWPFStyle style = styles.getStyle(styleId);
        if (style != null && StrUtil.isNotBlank(style.getName())) {
            return style.getName();
        }
        return styleId;
    }

    private static String buildParagraphStyle(XWPFDocument document, XWPFParagraph paragraph, String tag) {
        StringBuilder style = new StringBuilder();
        ParagraphAlignment alignment = paragraph.getAlignment();
        if (alignment != null) {
            switch (alignment) {
                case CENTER -> style.append("text-align:center;");
                case RIGHT -> style.append("text-align:right;");
                case BOTH -> style.append("text-align:justify;");
                default -> style.append("text-align:left;");
            }
        }
        if (paragraph.getSpacingBefore() > 0) {
            style.append("margin-top:").append(paragraph.getSpacingBefore() / TWIPS_PER_PT).append("pt;");
        }
        if (paragraph.getSpacingAfter() > 0) {
            style.append("margin-bottom:").append(paragraph.getSpacingAfter() / TWIPS_PER_PT).append("pt;");
        }
        if (paragraph.getSpacingBetween() > 0) {
            style.append("line-height:")
                    .append(formatLineHeight((int) paragraph.getSpacingBetween()))
                    .append(";");
        }
        if ("blockquote".equals(tag)) {
            style.append("border-left:4px solid #07c160;padding:12px;margin:12px 0;color:#666;");
        } else if (tag.startsWith("h")) {
            style.append(defaultHeadingStyle(Integer.parseInt(tag.substring(1))));
        } else if (style.isEmpty()) {
            style.append("margin:0 0 12px 0;line-height:1.75;color:#333;");
        }
        return style.toString();
    }

    private static String defaultHeadingStyle(int level) {
        return switch (level) {
            case 1 -> "font-size:24px;font-weight:bold;color:#111;margin:16px 0 12px 0;line-height:1.4;";
            case 2 -> "font-size:20px;font-weight:bold;color:#222;margin:14px 0 10px 0;line-height:1.45;";
            case 3 -> "font-size:17px;font-weight:bold;color:#333;margin:12px 0 8px 0;line-height:1.5;";
            default -> "font-weight:bold;margin:12px 0 8px 0;";
        };
    }

    private static String buildRunStyle(XWPFRun run) {
        StringBuilder style = new StringBuilder();
        if (run.isBold()) {
            style.append("font-weight:bold;");
        }
        if (run.isItalic()) {
            style.append("font-style:italic;");
        }
        UnderlinePatterns underline = run.getUnderline();
        if (underline != null && underline != UnderlinePatterns.NONE) {
            style.append("text-decoration:underline;");
        }
        if (run.isStrikeThrough()) {
            style.append("text-decoration:line-through;");
        }
        if (run.getFontSize() > 0) {
            style.append("font-size:").append(run.getFontSize() / 2).append("pt;");
        }
        String color = normalizeColor(run.getColor());
        if (StrUtil.isNotBlank(color)) {
            style.append("color:#").append(color).append(";");
        }
        String fontFamily = run.getFontFamily();
        if (StrUtil.isNotBlank(fontFamily)) {
            style.append("font-family:").append(fontFamily).append(";");
        }
        return style.toString();
    }

    private static String normalizeColor(String color) {
        if (StrUtil.isBlank(color) || "auto".equalsIgnoreCase(color)) {
            return "";
        }
        String hex = color.replace("#", "").toUpperCase(Locale.ROOT);
        if (hex.length() == 6) {
            return hex;
        }
        return "";
    }

    private static String formatLineHeight(int lineTwips) {
        double ratio = lineTwips / 240.0;
        if (ratio <= 0) {
            return "1.75";
        }
        return String.format(Locale.ROOT, "%.2f", ratio);
    }

    private static String escapeHtml(String text) {
        // escapeHtml4 preserves Unicode; EscapeUtil.escape() JS-escapes non-ASCII (garbled preview)
        return EscapeUtil.escapeHtml4(text).replace("\n", "<br/>");
    }

    private static Map<BigInteger, Boolean> buildOrderedLookup(XWPFDocument document) {
        Map<BigInteger, Boolean> map = new HashMap<>();
        XWPFNumbering numbering = document.getNumbering();
        if (numbering == null) {
            return map;
        }
        for (XWPFNum num : numbering.getNums()) {
            if (num == null || num.getCTNum() == null) {
                continue;
            }
            BigInteger numId = num.getCTNum().getNumId();
            BigInteger abstractNumId = num.getCTNum().getAbstractNumId() != null
                    ? num.getCTNum().getAbstractNumId().getVal() : null;
            if (numId == null || abstractNumId == null) {
                continue;
            }
            XWPFAbstractNum abstractNum = numbering.getAbstractNum(abstractNumId);
            if (abstractNum == null || abstractNum.getAbstractNum() == null) {
                continue;
            }
            boolean ordered = abstractNum.getAbstractNum().getLvlList().stream()
                    .findFirst()
                    .map(lvl -> {
                        if (lvl.getNumFmt() == null || lvl.getNumFmt().getVal() == null) {
                            return false;
                        }
                        STNumberFormat.Enum fmt = lvl.getNumFmt().getVal();
                        return fmt != STNumberFormat.BULLET && fmt != STNumberFormat.NONE;
                    })
                    .orElse(false);
            map.put(numId, ordered);
        }
        return map;
    }

    private static final class ListState {
        private final Deque<String> stack = new ArrayDeque<>();

        void sync(StringBuilder html, String tag, int level) {
            while (stack.size() > level + 1) {
                html.append("</").append(stack.pop()).append(">");
            }
            if (stack.size() == level + 1 && !tag.equals(stack.peek())) {
                html.append("</").append(stack.pop()).append(">");
            }
            while (stack.size() <= level) {
                html.append('<').append(tag).append(" style=\"").append(LIST_STYLE).append("\">");
                stack.push(tag);
            }
        }

        void closeAll(StringBuilder html) {
            while (!stack.isEmpty()) {
                html.append("</").append(stack.pop()).append(">");
            }
        }
    }
}
