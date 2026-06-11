package cn.iocoder.yudao.module.oa.framework.dingtalk;

import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import cn.iocoder.yudao.framework.common.exception.OaErrorCodes;
import cn.iocoder.yudao.framework.common.exception.ServiceException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 钉钉开放平台客户端（企业内部应用）。
 * 文档：https://open.dingtalk.com/document/development/obtain-orgapp-token
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DingTalkClient {

    private static final String TOKEN_URL = "https://api.dingtalk.com/v1.0/oauth2/accessToken";
    private static final String TOPAPI_BASE = "https://oapi.dingtalk.com";

    private final DingTalkProperties properties;

    private volatile String cachedToken;
    private volatile long tokenExpireAtMs;

    public void assertConfigured() {
        if (!properties.isEnabled()) {
            throw new ServiceException(OaErrorCodes.BAD_REQUEST.getCode(), "钉钉集成未启用，请配置 oa.dingtalk.enabled=true");
        }
        if (StrUtil.hasBlank(properties.getClientId(), properties.getClientSecret())) {
            throw new ServiceException(OaErrorCodes.BAD_REQUEST.getCode(), "钉钉 clientId/clientSecret 未配置");
        }
    }

    public String getAccessToken() {
        assertConfigured();
        long now = System.currentTimeMillis();
        if (cachedToken != null && now < tokenExpireAtMs - 60_000) {
            return cachedToken;
        }
        synchronized (this) {
            now = System.currentTimeMillis();
            if (cachedToken != null && now < tokenExpireAtMs - 60_000) {
                return cachedToken;
            }
            JSONObject body = JSONUtil.createObj()
                    .set("appKey", properties.getClientId())
                    .set("appSecret", properties.getClientSecret());
            HttpResponse response = HttpRequest.post(TOKEN_URL)
                    .header("Content-Type", "application/json")
                    .body(body.toString())
                    .timeout(15_000)
                    .execute();
            JSONObject result = parseJson(response.body());
            if (result.containsKey("accessToken")) {
                cachedToken = result.getStr("accessToken");
                int expireIn = result.getInt("expireIn", 7200);
                tokenExpireAtMs = System.currentTimeMillis() + expireIn * 1000L;
                return cachedToken;
            }
            throw new ServiceException(OaErrorCodes.BAD_REQUEST.getCode(),
                    "获取钉钉 access_token 失败: " + result.getStr("message", response.body()));
        }
    }

    /** 获取指定部门的直接子部门列表 */
    public List<DingDeptNode> listSubDepartments(long deptId) {
        JSONObject body = JSONUtil.createObj().set("dept_id", deptId);
        JSONObject result = topApiPost("/topapi/v2/department/listsub", body);
        JSONArray list = result.getJSONArray("result");
        if (list == null || list.isEmpty()) {
            return Collections.emptyList();
        }
        List<DingDeptNode> nodes = new ArrayList<>();
        for (int i = 0; i < list.size(); i++) {
            JSONObject item = list.getJSONObject(i);
            nodes.add(new DingDeptNode(item.getLong("dept_id"), item.getStr("name"), item.getLong("parent_id")));
        }
        return nodes;
    }

    /** 获取部门下用户 userid 列表（分页） */
    public List<String> listUserIds(long dingDeptId) {
        List<String> userIds = new ArrayList<>();
        long cursor = 0;
        int size = 100;
        while (true) {
            JSONObject body = JSONUtil.createObj()
                    .set("dept_id", dingDeptId)
                    .set("cursor", cursor)
                    .set("size", size);
            JSONObject result = topApiPost("/topapi/user/listid", body);
            JSONObject page = result.getJSONObject("result");
            if (page == null) {
                break;
            }
            JSONArray list = page.getJSONArray("userid_list");
            if (list != null) {
                for (int i = 0; i < list.size(); i++) {
                    userIds.add(list.getStr(i));
                }
            }
            Boolean hasMore = page.getBool("has_more");
            if (hasMore == null || !hasMore) {
                break;
            }
            cursor = page.getLong("next_cursor", 0L);
        }
        return userIds;
    }

    /** 获取用户详情 */
    public DingUserDetail getUser(String userid) {
        JSONObject body = JSONUtil.createObj()
                .set("userid", userid)
                .set("language", "zh_CN");
        JSONObject result = topApiPost("/topapi/v2/user/get", body);
        JSONObject user = result.getJSONObject("result");
        if (user == null) {
            return null;
        }
        DingUserDetail detail = new DingUserDetail();
        detail.setUserid(user.getStr("userid"));
        detail.setName(user.getStr("name"));
        detail.setMobile(user.getStr("mobile"));
        detail.setEmail(user.getStr("email"));
        detail.setActive(user.getBool("active", true));
        JSONArray deptIds = user.getJSONArray("dept_id_list");
        if (deptIds != null && !deptIds.isEmpty()) {
            detail.setPrimaryDeptId(deptIds.getLong(0));
        }
        return detail;
    }

    private JSONObject topApiPost(String path, JSONObject body) {
        String token = getAccessToken();
        String url = TOPAPI_BASE + path + "?access_token=" + token;
        HttpResponse response = HttpRequest.post(url)
                .header("Content-Type", "application/json")
                .body(body.toString())
                .timeout(20_000)
                .execute();
        JSONObject result = parseJson(response.body());
        Integer errcode = result.getInt("errcode");
        if (errcode != null && errcode != 0) {
            log.warn("DingTalk API {} failed: {}", path, result);
            throw new ServiceException(OaErrorCodes.BAD_REQUEST.getCode(),
                    "钉钉 API 调用失败: " + result.getStr("errmsg", "unknown"));
        }
        return result;
    }

    private JSONObject parseJson(String body) {
        if (StrUtil.isBlank(body)) {
            throw new ServiceException(OaErrorCodes.BAD_REQUEST.getCode(), "钉钉 API 返回空响应");
        }
        return JSONUtil.parseObj(body);
    }

    public record DingDeptNode(long deptId, String name, Long parentId) {
    }

    @lombok.Data
    public static class DingUserDetail {
        private String userid;
        private String name;
        private String mobile;
        private String email;
        private boolean active;
        private Long primaryDeptId;
    }
}
