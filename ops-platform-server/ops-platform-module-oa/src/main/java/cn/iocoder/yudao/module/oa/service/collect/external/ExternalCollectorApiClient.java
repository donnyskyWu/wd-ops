package cn.iocoder.yudao.module.oa.service.collect.external;

import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import cn.iocoder.yudao.module.oa.config.UnifiedCollectorProperties;
import cn.iocoder.yudao.module.oa.service.collect.unified.UnifiedCollectorApiException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Channel-D · unify-collector-api HTTP 客户端（ADR-052 · GATE-EXT-P0）。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ExternalCollectorApiClient {

    private static final int DEFAULT_PAGE_SIZE = 20;

    private final UnifiedCollectorProperties properties;

    public Map<String, Object> getKuaishouUserVideos(String userId, String cursor, int pageSize,
                                                     ExternalCollectorCredential credential) {
        if (properties.isStub()) {
            return stubKuaishouUserVideos(userId);
        }
        String path = "/api/v1/external/kuaishou/user-videos?user_id=" + userId
                + "&cursor=" + StrUtil.blankToDefault(cursor, "")
                + "&page_size=" + pageSize;
        JSONObject data = getJson(path, credential);
        return data == null ? Collections.emptyMap() : data;
    }

    private JSONObject getJson(String path, ExternalCollectorCredential credential) {
        HttpRequest request = HttpRequest.get(normalizeBaseUrl() + path)
                .timeout(properties.getTimeoutMs());
        if (StrUtil.isNotBlank(properties.getApiToken())) {
            request.header("Authorization", "Bearer " + properties.getApiToken());
        }
        if (credential != null && credential.hasCookie()) {
            request.header("Cookie", credential.getCookie());
        }
        HttpResponse response = request.execute();
        return parseEnvelope(response);
    }

    private JSONObject parseEnvelope(HttpResponse response) {
        int status = response.getStatus();
        String body = response.body();
        if (StrUtil.isBlank(body)) {
            if (status == 401 || status == 403) {
                throw new UnifiedCollectorApiException("TOKEN_FAIL", "Collector API 鉴权失败 HTTP " + status);
            }
            throw new UnifiedCollectorApiException("DISCONNECTED", "Collector API 空响应 HTTP " + status);
        }
        JSONObject root = JSONUtil.parseObj(body);
        if (status == 401 || status == 403) {
            String message = root.getStr("message", "Collector API 鉴权失败 HTTP " + status);
            int code = root.getInt("code", -1);
            throw new UnifiedCollectorApiException("TOKEN_FAIL", message, code);
        }
        if (status == 422) {
            throw new UnifiedCollectorApiException("DISCONNECTED", "Collector API 参数错误 HTTP 422");
        }
        int code = root.getInt("code", status >= 200 && status < 300 ? 0 : -1);
        if (status < 200 || status >= 300 || code != 0) {
            String message = root.getStr("message", "Collector API 请求失败 HTTP " + status);
            String connStatus = code == 40002 ? "TOKEN_FAIL" : "DISCONNECTED";
            throw new UnifiedCollectorApiException(connStatus, message, code);
        }
        return unwrapNestedData(root.getJSONObject("data"));
    }

    private JSONObject unwrapNestedData(JSONObject data) {
        if (data == null) {
            return null;
        }
        if (data.containsKey("code") && data.containsKey("data")) {
            int innerCode = data.getInt("code", -1);
            if (innerCode == 0) {
                Object inner = data.get("data");
                if (inner instanceof JSONObject innerObj) {
                    return unwrapNestedData(innerObj);
                }
            }
        }
        return data;
    }

    private Map<String, Object> stubKuaishouUserVideos(String userId) {
        Map<String, Object> videoA = new LinkedHashMap<>();
        videoA.put("photo_id", "stub_ext_ks_001");
        videoA.put("caption", "Stub快手竞品作品A");
        videoA.put("share_url", "https://www.kuaishou.com/short-video/stub_ext_ks_001");
        videoA.put("view_count", 15_800);
        videoA.put("like_count", 960);
        videoA.put("comment_count", 120);
        videoA.put("create_time", 1_700_300_000);

        Map<String, Object> videoB = new LinkedHashMap<>();
        videoB.put("photo_id", "stub_ext_ks_002");
        videoB.put("title", "Stub快手竞品作品B");
        videoB.put("video_url", "https://www.kuaishou.com/short-video/stub_ext_ks_002");
        videoB.put("view_count", 9_200);
        videoB.put("like_count", 520);
        videoB.put("comment_count", 66);
        videoB.put("create_time", 1_700_400_000);

        Map<String, Object> map = new LinkedHashMap<>();
        map.put("user_id", userId);
        map.put("user_name", "Stub竞品达人");
        map.put("fan_count", 888_000);
        map.put("total", 2);
        map.put("has_more", false);
        map.put("cursor", "");
        map.put("videos", List.of(videoA, videoB));
        return map;
    }

    private String normalizeBaseUrl() {
        return StrUtil.removeSuffix(StrUtil.blankToDefault(properties.getBaseUrl(), "").trim(), "/");
    }
}
