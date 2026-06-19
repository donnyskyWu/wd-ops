package cn.iocoder.yudao.module.oa.util;

import cn.hutool.core.util.ReUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.iocoder.yudao.framework.common.exception.OaErrorCodes;
import cn.iocoder.yudao.framework.common.exception.ServiceException;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Fetch and parse WeChat official account article HTML (URL or saved MHTML).
 */
public final class WechatArticleHtmlFetcher {

    private static final String WECHAT_MOBILE_UA =
            "Mozilla/5.0 (iPhone; CPU iPhone OS 16_0 like Mac OS X) AppleWebKit/605.1.15 "
                    + "(KHTML, like Gecko) Mobile/15E148 MicroMessenger/8.0.38 NetType/WIFI Language/zh_CN";

    private static final Pattern ANTI_BOT_PATTERN = Pattern.compile("环境异常|完成验证后即可继续访问");
    private static final Pattern MHTML_BOUNDARY_PATTERN =
            Pattern.compile("(?im)boundary=\"([^\"]+)\"");
    private static final Pattern MHTML_HTML_PART_PATTERN =
            Pattern.compile("(?is)Content-Type:\\s*text/html.*?\\r?\\n\\r?\\n(.*?)(?:\\r?\\n--|$)");

    private WechatArticleHtmlFetcher() {
    }

    public static String fetchFromUrl(String sourceUrl) {
        return fetchFromUrl(sourceUrl, 0);
    }

    private static String fetchFromUrl(String sourceUrl, int redirectDepth) {
        if (redirectDepth > 5) {
            throw new ServiceException(OaErrorCodes.LAYOUT_URL_IMPORT_FAILED.getCode(),
                    "公众号 URL 重定向次数过多");
        }
        HttpResponse response = HttpRequest.get(sourceUrl)
                .timeout(15000)
                .setFollowRedirects(true)
                .setMaxRedirectCount(5)
                .header("User-Agent", WECHAT_MOBILE_UA)
                .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                .header("Accept-Language", "zh-CN,zh;q=0.9")
                .header("Referer", "https://mp.weixin.qq.com/")
                .execute();
        int status = response.getStatus();
        String body = response.body();
        if (status >= 300 && status < 400) {
            String location = response.header("Location");
            if (StrUtil.isNotBlank(location)) {
                String nextUrl = location.startsWith("http") ? location : "https://mp.weixin.qq.com" + location;
                return fetchFromUrl(nextUrl, redirectDepth + 1);
            }
        }
        if (status < 200 || status >= 400) {
            throw new ServiceException(OaErrorCodes.LAYOUT_URL_IMPORT_FAILED.getCode(),
                    "公众号 URL 抓取失败（HTTP " + status + "）");
        }
        if (StrUtil.isBlank(body)) {
            throw new ServiceException(OaErrorCodes.LAYOUT_URL_IMPORT_FAILED.getCode(),
                    "公众号 URL 返回空内容");
        }
        if (ANTI_BOT_PATTERN.matcher(body).find()) {
            throw new ServiceException(OaErrorCodes.LAYOUT_URL_IMPORT_FAILED.getCode(),
                    "微信反爬验证页，请改用手动粘贴 HTML 或上传 .mhtml 文件");
        }
        return extractArticleHtml(body);
    }

    public static String parseMhtml(InputStream inputStream) throws IOException {
        return parseMhtml(inputStream.readAllBytes());
    }

    public static String parseMhtml(byte[] bytes) {
        if (bytes == null || bytes.length == 0) {
            throw new ServiceException(OaErrorCodes.BAD_REQUEST.getCode(), "MHTML 文件为空");
        }
        String probe = new String(bytes, 0, Math.min(bytes.length, 8192), StandardCharsets.UTF_8);
        String raw = looksQuotedPrintable(probe)
                ? new String(bytes, StandardCharsets.ISO_8859_1)
                : new String(bytes, StandardCharsets.UTF_8);
        String htmlPart = extractMhtmlHtmlPart(raw);
        if (StrUtil.isBlank(htmlPart)) {
            throw new ServiceException(OaErrorCodes.BAD_REQUEST.getCode(), "MHTML 中未找到 HTML 内容");
        }
        String decoded = looksQuotedPrintable(htmlPart) ? decodeQuotedPrintable(htmlPart) : htmlPart;
        return extractArticleHtml(decoded);
    }

    static boolean looksQuotedPrintable(String part) {
        if (StrUtil.isBlank(part)) {
            return false;
        }
        return part.contains("=3D") || part.contains("=3d") || part.contains("=\n")
                || part.contains("=E") || part.contains("=e");
    }

    public static String extractArticleHtml(String pageHtml) {
        if (StrUtil.isBlank(pageHtml)) {
            throw new ServiceException(OaErrorCodes.LAYOUT_URL_IMPORT_FAILED.getCode(),
                    "页面 HTML 为空");
        }
        String jsContent = extractElementInnerHtml(pageHtml, "js_content");
        if (StrUtil.isNotBlank(jsContent) && jsContent.replaceAll("<[^>]+>", "").trim().length() > 20) {
            return LayoutImportExtractor.normalizeWechatPreviewHtml(jsContent);
        }
        String proseMirror = extractByClassInnerHtml(pageHtml, "ProseMirror");
        if (StrUtil.isNotBlank(proseMirror)) {
            return LayoutImportExtractor.normalizeWechatPreviewHtml(
                    "<div class=\"rich_media_content\">" + proseMirror + "</div>");
        }
        if (StrUtil.isNotBlank(jsContent)) {
            return LayoutImportExtractor.normalizeWechatPreviewHtml(jsContent);
        }
        String rich = extractByClassInnerHtml(pageHtml, "rich_media_content");
        if (StrUtil.isNotBlank(rich) && rich.replaceAll("<[^>]+>", "").trim().length() > 20) {
            return LayoutImportExtractor.normalizeWechatPreviewHtml(rich);
        }
        throw new ServiceException(OaErrorCodes.LAYOUT_URL_IMPORT_FAILED.getCode(),
                "未找到文章正文区域（js_content / rich_media_content）");
    }

    static String extractElementInnerHtml(String html, String elementId) {
        Pattern openPattern = Pattern.compile("(?is)<([a-z][a-z0-9]*)\\b[^>]*\\bid=[\"']"
                + Pattern.quote(elementId) + "[\"'][^>]*>");
        Matcher matcher = openPattern.matcher(html);
        if (!matcher.find()) {
            return null;
        }
        int contentStart = matcher.end();
        return extractBalancedInnerHtml(html, contentStart, matcher.group(1));
    }

    static String extractByClassInnerHtml(String html, String className) {
        Pattern openPattern = Pattern.compile("(?is)<([a-z][a-z0-9]*)\\b[^>]*\\bclass=[\"'][^\"']*\\b"
                + Pattern.quote(className) + "\\b[^\"']*[\"'][^>]*>");
        Matcher matcher = openPattern.matcher(html);
        String best = null;
        while (matcher.find()) {
            String candidate = extractBalancedInnerHtml(html, matcher.end(), matcher.group(1));
            if (StrUtil.isNotBlank(candidate) && (best == null || candidate.length() > best.length())) {
                best = candidate;
            }
        }
        return best;
    }

    static String extractBalancedInnerHtml(String html, int contentStart, String tagName) {
        String openToken = "<" + tagName;
        String closeToken = "</" + tagName;
        int depth = 1;
        int pos = contentStart;
        while (pos < html.length() && depth > 0) {
            int nextOpen = indexOfIgnoreCase(html, openToken, pos);
            int nextClose = indexOfIgnoreCase(html, closeToken, pos);
            if (nextClose < 0) {
                break;
            }
            if (nextOpen >= 0 && nextOpen < nextClose) {
                depth++;
                pos = nextOpen + openToken.length();
            } else {
                depth--;
                if (depth == 0) {
                    return html.substring(contentStart, nextClose).trim();
                }
                pos = nextClose + closeToken.length();
            }
        }
        return null;
    }

    static String extractMhtmlHtmlPart(String mhtml) {
        Matcher partMatcher = MHTML_HTML_PART_PATTERN.matcher(mhtml);
        String best = null;
        int bestScore = -1;
        while (partMatcher.find()) {
            String part = partMatcher.group(1);
            if (StrUtil.isBlank(part)) {
                continue;
            }
            String candidate = looksQuotedPrintable(part) ? decodeQuotedPrintable(part) : part;
            int score = scoreHtmlPart(candidate);
            if (score > bestScore) {
                bestScore = score;
                best = part;
            }
        }
        if (StrUtil.isNotBlank(best)) {
            return best;
        }
        Matcher boundaryMatcher = MHTML_BOUNDARY_PATTERN.matcher(mhtml);
        if (!boundaryMatcher.find()) {
            return null;
        }
        String boundary = boundaryMatcher.group(1).trim();
        String[] chunks = mhtml.split("--" + Pattern.quote(boundary));
        for (String chunk : chunks) {
            if (!chunk.toLowerCase().contains("content-type: text/html")) {
                continue;
            }
            int bodyStart = chunk.indexOf("\n\n");
            if (bodyStart < 0) {
                bodyStart = chunk.indexOf("\r\n\r\n");
                if (bodyStart >= 0) {
                    bodyStart += 4;
                }
            } else {
                bodyStart += 2;
            }
            if (bodyStart > 0 && bodyStart < chunk.length()) {
                String part = chunk.substring(bodyStart).trim();
                if (part.endsWith("--")) {
                    part = part.substring(0, part.length() - 2).trim();
                }
                if (StrUtil.isNotBlank(part)) {
                    String candidate = looksQuotedPrintable(part) ? decodeQuotedPrintable(part) : part;
                    int score = scoreHtmlPart(candidate);
                    if (score > bestScore) {
                        bestScore = score;
                        best = part;
                    }
                }
            }
        }
        return best;
    }

    static int scoreHtmlPart(String html) {
        if (StrUtil.isBlank(html)) {
            return 0;
        }
        int score = html.length();
        if (html.contains("ProseMirror")) {
            score += 100_000;
        }
        if (html.contains("js_content")) {
            score += 50_000;
        }
        if (html.contains("rich_media_content")) {
            score += 10_000;
        }
        return score;
    }

    static String decodeQuotedPrintable(String input) {
        if (StrUtil.isBlank(input)) {
            return "";
        }
        String normalized = input.replace("\r\n", "\n").replace('\r', '\n');
        normalized = normalized.replaceAll("=\\n", "");
        java.io.ByteArrayOutputStream bytes = new java.io.ByteArrayOutputStream(normalized.length());
        for (int i = 0; i < normalized.length(); i++) {
            char ch = normalized.charAt(i);
            if (ch == '=' && i + 2 < normalized.length()) {
                char h1 = normalized.charAt(i + 1);
                char h2 = normalized.charAt(i + 2);
                if (ReUtil.isMatch("[0-9A-Fa-f]{2}", "" + h1 + h2)) {
                    bytes.write(Integer.parseInt("" + h1 + h2, 16));
                    i += 2;
                    continue;
                }
            }
            bytes.write((byte) (ch & 0xFF));
        }
        return bytes.toString(StandardCharsets.UTF_8);
    }

    private static int indexOfIgnoreCase(String source, String token, int fromIndex) {
        return source.toLowerCase().indexOf(token.toLowerCase(), fromIndex);
    }
}
