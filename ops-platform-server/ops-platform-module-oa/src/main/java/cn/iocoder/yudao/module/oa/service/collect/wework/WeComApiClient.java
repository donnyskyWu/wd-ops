package cn.iocoder.yudao.module.oa.service.collect.wework;

import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import cn.iocoder.yudao.module.oa.config.WeComProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 企微 OpenAPI 客户端（Channel-C · ADR-048 · 直连，不经 collector）。
 *
 * <ul>
 *   <li>gettoken — corp_id + secret（任务前置）</li>
 *   <li>92113 — GET /cgi-bin/externalcontact/list</li>
 *   <li>92132 — POST /cgi-bin/externalcontact/get_user_behavior_data</li>
 *   <li>90200 — GET /cgi-bin/user/simplelist</li>
 * </ul>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WeComApiClient {

    private static final ZoneId CHINA = ZoneId.of("Asia/Shanghai");

    private final WeComProperties properties;
    private final ConcurrentHashMap<String, TokenCache> tokenCache = new ConcurrentHashMap<>();

    /** 仅 IT 使用：避免跨用例 token 缓存干扰 MockWebServer 断言。 */
    public void clearTokenCacheForTest() {
        tokenCache.clear();
    }

    public String getAccessToken(String corpId, String secret) {
        if (properties.isStub()) {
            return "stub_access_token";
        }
        String cacheKey = corpId + ":" + secret;
        TokenCache cached = tokenCache.get(cacheKey);
        if (cached != null && cached.isValid()) {
            return cached.accessToken;
        }
        String url = normalizeBaseUrl() + "/cgi-bin/gettoken?corpid=" + corpId + "&corpsecret=" + secret;
        HttpResponse response = HttpRequest.get(url)
                .timeout(properties.getTimeoutMs())
                .execute();
        JSONObject body = parseBody(response, "获取 access_token 失败");
        String accessToken = body.getStr("access_token");
        if (StrUtil.isBlank(accessToken)) {
            throw new WeComApiException("企微未返回 access_token");
        }
        int expiresIn = body.getInt("expires_in", 7200);
        tokenCache.put(cacheKey, new TokenCache(accessToken, System.currentTimeMillis() + (expiresIn - 60L) * 1000));
        return accessToken;
    }

    /**
     * 92113 — 获取指定成员添加的客户 external_userid 列表。
     */
    public List<String> listExternalContactIds(String accessToken, String userid) {
        if (properties.isStub()) {
            return List.of("wo_stub_001", "wo_stub_002", "wo_stub_003");
        }
        String url = normalizeBaseUrl() + "/cgi-bin/externalcontact/list?access_token=" + accessToken
                + "&userid=" + userid;
        HttpResponse response = HttpRequest.get(url)
                .timeout(properties.getTimeoutMs())
                .execute();
        JSONObject body = parseBody(response, "获取客户列表失败");
        JSONArray array = body.getJSONArray("external_userid");
        if (array == null || array.isEmpty()) {
            return List.of();
        }
        List<String> ids = new ArrayList<>(array.size());
        for (Object item : array) {
            if (item != null && StrUtil.isNotBlank(String.valueOf(item))) {
                ids.add(String.valueOf(item));
            }
        }
        return ids;
    }

    /**
     * 92132 — 获取「联系客户统计」数据（按天维度）。
     */
    public List<WeComUserBehaviorData> getUserBehaviorData(String accessToken, List<String> userids,
                                                           LocalDate startDate, LocalDate endDate) {
        if (properties.isStub()) {
            WeComUserBehaviorData row = new WeComUserBehaviorData();
            row.setStatTime(startDate.atStartOfDay(CHINA).toEpochSecond());
            row.setChatCnt(12);
            row.setMessageCnt(34);
            return List.of(row);
        }
        if (userids == null || userids.isEmpty()) {
            return List.of();
        }
        long startTime = startDate.atStartOfDay(CHINA).toEpochSecond();
        long endTime = endDate.atStartOfDay(CHINA).toEpochSecond();
        JSONObject payload = new JSONObject();
        payload.set("userid", userids);
        payload.set("start_time", startTime);
        payload.set("end_time", endTime);

        String url = normalizeBaseUrl() + "/cgi-bin/externalcontact/get_user_behavior_data?access_token="
                + accessToken;
        HttpResponse response = HttpRequest.post(url)
                .timeout(properties.getTimeoutMs())
                .body(payload.toString())
                .contentType("application/json")
                .execute();
        JSONObject body = parseBody(response, "获取联系客户统计失败");
        JSONArray behaviorData = body.getJSONArray("behavior_data");
        if (behaviorData == null || behaviorData.isEmpty()) {
            return List.of();
        }
        List<WeComUserBehaviorData> rows = new ArrayList<>(behaviorData.size());
        for (Object item : behaviorData) {
            JSONObject obj = JSONUtil.parseObj(item);
            WeComUserBehaviorData row = new WeComUserBehaviorData();
            row.setStatTime(obj.getLong("stat_time", 0L));
            row.setChatCnt(obj.getInt("chat_cnt", 0));
            row.setMessageCnt(obj.getInt("message_cnt", 0));
            row.setNewApplyCnt(obj.getInt("new_apply_cnt", 0));
            row.setNewContactCnt(obj.getInt("new_contact_cnt", 0));
            rows.add(row);
        }
        return rows;
    }

    /**
     * 90200 — 获取部门成员 userid 列表（应用可见范围内）。
     */
    public List<String> listDepartmentMemberUserids(String accessToken, long departmentId) {
        if (properties.isStub()) {
            return List.of("stub_user_001");
        }
        String url = normalizeBaseUrl() + "/cgi-bin/user/simplelist?access_token=" + accessToken
                + "&department_id=" + departmentId;
        HttpResponse response = HttpRequest.get(url)
                .timeout(properties.getTimeoutMs())
                .execute();
        JSONObject body = parseBody(response, "获取部门成员失败");
        JSONArray userlist = body.getJSONArray("userlist");
        if (userlist == null || userlist.isEmpty()) {
            return List.of();
        }
        List<String> userids = new ArrayList<>(userlist.size());
        for (Object item : userlist) {
            JSONObject user = JSONUtil.parseObj(item);
            String userid = user.getStr("userid");
            if (StrUtil.isNotBlank(userid)) {
                userids.add(userid);
            }
        }
        return userids;
    }

    private JSONObject parseBody(HttpResponse response, String failurePrefix) {
        int status = response.getStatus();
        String text = response.body();
        if (status < 200 || status >= 300) {
            throw new WeComApiException(failurePrefix + " HTTP " + status);
        }
        if (!JSONUtil.isTypeJSONObject(text)) {
            throw new WeComApiException(failurePrefix + "：响应非 JSON");
        }
        JSONObject body = JSONUtil.parseObj(text);
        int errcode = body.getInt("errcode", -1);
        if (errcode != 0) {
            String errmsg = body.getStr("errmsg", "unknown");
            throw new WeComApiException(errcode, failurePrefix + "：" + errmsg);
        }
        return body;
    }

    private String normalizeBaseUrl() {
        String base = StrUtil.blankToDefault(properties.getBaseUrl(), "https://qyapi.weixin.qq.com");
        return base.endsWith("/") ? base.substring(0, base.length() - 1) : base;
    }

    private static final class TokenCache {
        private final String accessToken;
        private final long expireAtMs;

        private TokenCache(String accessToken, long expireAtMs) {
            this.accessToken = accessToken;
            this.expireAtMs = expireAtMs;
        }

        private boolean isValid() {
            return System.currentTimeMillis() < expireAtMs;
        }
    }
}
