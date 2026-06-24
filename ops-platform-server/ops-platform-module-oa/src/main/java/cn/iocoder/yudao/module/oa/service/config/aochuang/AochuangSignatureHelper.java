package cn.iocoder.yudao.module.oa.service.config.aochuang;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 奥创 OpenAPI 签名（Apifox doc-8348826 · Signature-Version 2.0）。
 * <p>
 * {@code Signature = sha1(requestUri + payload + timestamp + accountName + secret)}
 */
public final class AochuangSignatureHelper {

    public static final String SIGNATURE_VERSION = "2.0";

    private AochuangSignatureHelper() {
    }

    public static String sign(String requestUri, String payload, String timestamp,
                              String accountName, String secret) {
        String preSign = requestUri + payload + timestamp + accountName + secret;
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-1");
            byte[] digest = md.digest(preSign.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(digest.length * 2);
            for (byte b : digest) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-1 not available", ex);
        }
    }

    /**
     * v2.0 GET：参数按调用顺序拼接，仅 value URL 编码。
     */
    public static String encodeQueryString(Map<String, String> params) {
        return params.entrySet().stream()
                .map(entry -> entry.getKey() + "=" + urlEncode(entry.getValue()))
                .collect(Collectors.joining("&"));
    }

    public static Map<String, String> orderedParams() {
        return new LinkedHashMap<>();
    }

    private static String urlEncode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
