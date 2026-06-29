package cn.iocoder.yudao.module.oa.service.content.publish;

import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import cn.iocoder.yudao.module.oa.config.WechatOfficialProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 微信公众号 Open API 客户端（M2 发布 · ADR-047 Phase 2 · draft/add）。
 *
 * <p>凭证：AppID + AppSecret（M4 档案字段），与采集 bind 职责分离。</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WechatOfficialApiClient {

    /** 1×1 占位封面图（JPEG），用于未配置 coverImage 时上传 thumb 素材 */
    private static final byte[] DEFAULT_THUMB_JPEG = Base64.getDecoder().decode(
            "/9j/4AAQSkZJRgABAQEASABIAAD/2wBDAP//////////////////////////////////////////"
                    + "2wBDAAYEBQYFBAYGBQYHBwYIChAKCgkJChQODwwQFxQYGBcUFhYaHSUfGhsjHBYWICwgIyYnKSopGR8tLT0sLTD/"
                    + "2wBDAQcHBwoIChMKChMoGhYaKCgoKCgoKCgoKCgoKCgoKCgoKCgoKCgoKCgoKCgoKCgoKCgoKCgoKCgoKCgoKCgoKCj/"
                    + "wAARCAABAAEDASIAAhEBAxEB/8QAFQABAQAAAAAAAAAAAAAAAAAAAAn/xAAUEAEAAAAAAAAAAAAAAAAAAAAA/"
                    + "8QAFQEBAQAAAAAAAAAAAAAAAAAAAAX/xAAUEQEAAAAAAAAAAAAAAAAAAAAA/9oADAMBAAIRAxEAPwCwAB//2Q==");

    private final WechatOfficialProperties properties;
    private final ConcurrentHashMap<String, TokenCache> tokenCache = new ConcurrentHashMap<>();

    /** 仅 IT 使用：避免跨用例 token 缓存干扰。 */
    public void clearTokenCacheForTest() {
        tokenCache.clear();
    }

    public String getAccessToken(String appId, String appSecret) {
        if (properties.isStub()) {
            return "stub_wechat_access_token";
        }
        String cacheKey = appId + ":" + appSecret;
        TokenCache cached = tokenCache.get(cacheKey);
        if (cached != null && cached.isValid()) {
            return cached.accessToken;
        }
        String url = normalizeBaseUrl() + "/cgi-bin/token?grant_type=client_credential"
                + "&appid=" + appId + "&secret=" + appSecret;
        HttpResponse response = HttpRequest.get(url)
                .timeout(properties.getTimeoutMs())
                .execute();
        JSONObject body = parseBody(response, "获取 access_token 失败");
        String accessToken = body.getStr("access_token");
        if (StrUtil.isBlank(accessToken)) {
            throw new WechatOfficialApiException("微信未返回 access_token");
        }
        int expiresIn = body.getInt("expires_in", 7200);
        tokenCache.put(cacheKey, new TokenCache(accessToken,
                System.currentTimeMillis() + (expiresIn - 300L) * 1000));
        return accessToken;
    }

    /**
     * 上传永久 thumb 素材，返回 media_id（draft/add 必填 thumb_media_id）。
     */
    public String uploadThumbMedia(String accessToken, byte[] imageBytes, String filename) {
        if (properties.isStub()) {
            return "stub-thumb-" + UUID.randomUUID().toString().substring(0, 8);
        }
        String url = normalizeBaseUrl() + "/cgi-bin/material/add_material?access_token=" + accessToken
                + "&type=thumb";
        HttpResponse response = HttpRequest.post(url)
                .timeout(properties.getTimeoutMs())
                .form("media", imageBytes, StrUtil.blankToDefault(filename, "thumb.jpg"))
                .execute();
        JSONObject body = parseBody(response, "上传封面素材失败");
        String mediaId = body.getStr("media_id");
        if (StrUtil.isBlank(mediaId)) {
            throw new WechatOfficialApiException("微信未返回 thumb media_id");
        }
        return mediaId;
    }

    /**
     * 新增草稿到草稿箱（draft/add），返回 media_id。
     */
    public String addDraft(String accessToken, Map<String, Object> article) {
        if (properties.isStub()) {
            return "stub-draft-" + UUID.randomUUID().toString().substring(0, 8);
        }
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("articles", java.util.List.of(article));
        String url = normalizeBaseUrl() + "/cgi-bin/draft/add?access_token=" + accessToken;
        HttpResponse response = HttpRequest.post(url)
                .timeout(properties.getTimeoutMs())
                .body(JSONUtil.toJsonStr(payload))
                .contentType("application/json; charset=utf-8")
                .execute();
        JSONObject body = parseBody(response, "新增草稿失败");
        String mediaId = body.getStr("media_id");
        if (StrUtil.isBlank(mediaId)) {
            throw new WechatOfficialApiException("微信未返回草稿 media_id");
        }
        return mediaId;
    }

    /**
     * 发布草稿为正式图文（freepublish/submit），返回 publish_id。
     *
     * @see <a href="https://developers.weixin.qq.com/doc/service/api/public/api_freepublish_submit.html">freepublish/submit</a>
     */
    public String freepublishSubmit(String accessToken, String mediaId) {
        if (properties.isStub()) {
            return "stub-publish-" + UUID.randomUUID().toString().substring(0, 8);
        }
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("media_id", mediaId);
        String url = normalizeBaseUrl() + "/cgi-bin/freepublish/submit?access_token=" + accessToken;
        HttpResponse response = HttpRequest.post(url)
                .timeout(properties.getTimeoutMs())
                .body(JSONUtil.toJsonStr(payload))
                .contentType("application/json; charset=utf-8")
                .execute();
        JSONObject body = parseBody(response, "正式发布失败");
        String publishId = body.getStr("publish_id");
        if (StrUtil.isBlank(publishId)) {
            throw new WechatOfficialApiException("微信未返回 publish_id");
        }
        return publishId;
    }

    public byte[] resolveThumbBytes(String coverImageUrl) {
        if (StrUtil.isBlank(coverImageUrl)) {
            return DEFAULT_THUMB_JPEG;
        }
        if (!coverImageUrl.startsWith("http://") && !coverImageUrl.startsWith("https://")) {
            log.warn("coverImage 非 HTTP URL，使用默认封面: {}", coverImageUrl);
            return DEFAULT_THUMB_JPEG;
        }
        if (properties.isStub()) {
            return DEFAULT_THUMB_JPEG;
        }
        HttpResponse response = HttpRequest.get(coverImageUrl)
                .timeout(properties.getTimeoutMs())
                .execute();
        if (!response.isOk()) {
            throw new WechatOfficialApiException("下载封面图失败: HTTP " + response.getStatus());
        }
        byte[] bytes = response.bodyBytes();
        if (bytes == null || bytes.length == 0) {
            throw new WechatOfficialApiException("封面图为空");
        }
        return bytes;
    }

    private JSONObject parseBody(HttpResponse response, String action) {
        if (!response.isOk()) {
            throw new WechatOfficialApiException(action + ": HTTP " + response.getStatus());
        }
        JSONObject body = JSONUtil.parseObj(response.body());
        Integer errcode = body.getInt("errcode");
        if (errcode != null && errcode != 0) {
            String errmsg = body.getStr("errmsg", "");
            if (errcode == 40001 || errcode == 40014 || errcode == 42001) {
                tokenCache.clear();
            }
            throw new WechatOfficialApiException(action + ": errcode=" + errcode + ", errmsg=" + errmsg);
        }
        return body;
    }

    private String normalizeBaseUrl() {
        return StrUtil.removeSuffix(StrUtil.blankToDefault(properties.getBaseUrl(),
                "https://api.weixin.qq.com"), "/");
    }

    private static final class TokenCache {
        private final String accessToken;
        private final long expiresAtMs;

        private TokenCache(String accessToken, long expiresAtMs) {
            this.accessToken = accessToken;
            this.expiresAtMs = expiresAtMs;
        }

        private boolean isValid() {
            return System.currentTimeMillis() < expiresAtMs;
        }
    }
}
