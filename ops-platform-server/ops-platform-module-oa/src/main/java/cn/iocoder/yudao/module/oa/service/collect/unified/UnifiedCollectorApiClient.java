package cn.iocoder.yudao.module.oa.service.collect.unified;

import cn.hutool.core.io.IORuntimeException;
import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import cn.iocoder.yudao.module.oa.config.UnifiedCollectorProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * unify-collector-api HTTP 客户端（Channel-A · ADR-047 · M10-API-S-02）。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class UnifiedCollectorApiClient {

    private final UnifiedCollectorProperties properties;

    public UnifiedCollectorImportResult importAccount(String platform, Map<String, Object> credential,
                                                      String nickname, Map<String, Object> identity) {
        if (properties.isStub()) {
            return stubImport(platform);
        }
        JSONObject body = JSONUtil.createObj()
                .set("platform", platform)
                .set("credential", credential)
                .set("nickname", StrUtil.blankToDefault(nickname, platform));
        if (identity != null && !identity.isEmpty()) {
            body.set("identity", identity);
        }
        JSONObject data = postJson("/api/v1/accounts/import", body);
        return toImportResult(data);
    }

    public void deleteAccount(String collectorAccountId) {
        if (properties.isStub()) {
            return;
        }
        delete("/api/v1/accounts/" + collectorAccountId);
    }

    public Map<String, Object> getAccount(String collectorAccountId) {
        if (properties.isStub()) {
            return stubAccount(collectorAccountId);
        }
        JSONObject data = getJson("/api/v1/accounts/" + collectorAccountId);
        return data == null ? Collections.emptyMap() : data;
    }

    public Map<String, Object> getAccountHealthEntry(String collectorAccountId) {
        if (properties.isStub()) {
            return stubHealthEntry(collectorAccountId);
        }
        JSONObject data = getJson("/api/v1/accounts/health");
        if (data == null) {
            return Collections.emptyMap();
        }
        JSONArray accounts = data.getJSONArray("accounts");
        if (accounts == null) {
            return Collections.emptyMap();
        }
        for (int i = 0; i < accounts.size(); i++) {
            JSONObject item = accounts.getJSONObject(i);
            if (collectorAccountId.equals(item.getStr("account_id"))) {
                return item;
            }
        }
        return Collections.emptyMap();
    }

    public List<Map<String, Object>> listReloginAccounts() {
        if (properties.isStub()) {
            return Collections.emptyList();
        }
        JSONObject data = getJson("/api/v1/accounts/relogin");
        if (data == null) {
            return Collections.emptyList();
        }
        JSONArray accounts = data.getJSONArray("accounts");
        if (accounts == null) {
            return Collections.emptyList();
        }
        List<Map<String, Object>> result = new ArrayList<>();
        for (int i = 0; i < accounts.size(); i++) {
            result.add(accounts.getJSONObject(i));
        }
        return result;
    }

    public Map<String, Object> getWechatMpFollowerList(String collectorAccountId) {
        if (properties.isStub()) {
            return stubWechatMpFollowerList(collectorAccountId);
        }
        JSONObject data = getJson("/api/v1/internal/wechat-mp/follower-list?account_id=" + collectorAccountId);
        return data == null ? Collections.emptyMap() : data;
    }

    /** 官方 API：分页粉丝 openid（已认证号 · user/get） */
    public Map<String, Object> getWechatMpOfficialFollowerListPage(String collectorAccountId, int page) {
        if (properties.isStub()) {
            return stubWechatMpOfficialFollowerPage(collectorAccountId, page);
        }
        JSONObject data = getJson("/api/v1/internal/wechat-mp/official/follower-list?account_id="
                + collectorAccountId + "&page=" + page);
        return data == null ? Collections.emptyMap() : data;
    }

    /** 官方 API：批量粉丝详情（batchget，最多 100 openid） */
    public Map<String, Object> getWechatMpOfficialFollowerBatch(String collectorAccountId, List<String> openids) {
        if (properties.isStub()) {
            return stubWechatMpOfficialFollowerBatch(collectorAccountId, openids);
        }
        StringBuilder path = new StringBuilder("/api/v1/internal/wechat-mp/official/follower-batch?account_id=")
                .append(collectorAccountId);
        for (String openid : openids) {
            path.append("&openids=").append(openid);
        }
        JSONObject data = postJson(path.toString(), JSONUtil.createObj());
        return data == null ? Collections.emptyMap() : data;
    }

    /** 官方 API：累计用户数（getusercumulate） */
    public Map<String, Object> getWechatMpOfficialUserCumulate(String collectorAccountId,
                                                                String beginDate, String endDate) {
        if (properties.isStub()) {
            return stubWechatMpOfficialUserCumulate(collectorAccountId);
        }
        JSONObject data = getJson("/api/v1/internal/wechat-mp/official/user-cumulate?account_id="
                + collectorAccountId + "&begin_date=" + beginDate + "&end_date=" + endDate);
        return data == null ? Collections.emptyMap() : data;
    }

    /** @deprecated 微信已下线 getarticlesummary，请用 {@link #getWechatMpOfficialArticleTotalDetail} */
    @Deprecated
    public Map<String, Object> getWechatMpOfficialArticleStats(String collectorAccountId,
                                                                String beginDate, String endDate) {
        if (properties.isStub()) {
            return stubWechatMpOfficialArticleStats(collectorAccountId);
        }
        JSONObject data = getJson("/api/v1/internal/wechat-mp/official/article-stats?account_id="
                + collectorAccountId + "&begin_date=" + beginDate + "&end_date=" + endDate);
        return data == null ? Collections.emptyMap() : data;
    }

    public Map<String, Object> getWechatMpFollowerStats(String collectorAccountId) {
        if (properties.isStub()) {
            return stubWechatMpFollowerStats(collectorAccountId);
        }
        JSONObject data = getJson("/api/v1/internal/wechat-mp/follower-stats?account_id=" + collectorAccountId);
        return data == null ? Collections.emptyMap() : data;
    }
    /** 官方 API：图文素材列表（batchget_material · news） */
    public Map<String, Object> getWechatMpOfficialMaterialList(String collectorAccountId, int offset, int count) {
        if (properties.isStub()) {
            return stubWechatMpOfficialMaterialList(collectorAccountId);
        }
        JSONObject data = getJson("/api/v1/internal/wechat-mp/official/material-list?account_id="
                + collectorAccountId + "&offset=" + offset + "&count=" + count);
        return data == null ? Collections.emptyMap() : data;
    }

    /** 官方 API：已发布消息列表（freepublish/batchget） */
    public Map<String, Object> getWechatMpOfficialFreepublishList(String collectorAccountId, int offset, int count) {
        if (properties.isStub()) {
            return stubWechatMpOfficialFreepublishList(collectorAccountId);
        }
        JSONObject data = getJson("/api/v1/internal/wechat-mp/official/freepublish-list?account_id="
                + collectorAccountId + "&offset=" + offset + "&count=" + count + "&no_content=1");
        return data == null ? Collections.emptyMap() : data;
    }

    /** 官方 API：已发布图文详情（freepublish/getarticle） */
    public Map<String, Object> getWechatMpOfficialFreepublishArticle(String collectorAccountId, String articleId) {
        if (properties.isStub()) {
            return stubWechatMpOfficialFreepublishArticle(collectorAccountId, articleId);
        }
        JSONObject data = getJson("/api/v1/internal/wechat-mp/official/freepublish-article?account_id="
                + collectorAccountId + "&article_id=" + articleId);
        return data == null ? Collections.emptyMap() : data;
    }

    /** 官方 API：发表内容详细数据（getarticletotaldetail，仅 1 天） */
    public Map<String, Object> getWechatMpOfficialArticleTotalDetail(String collectorAccountId,
                                                                      String beginDate, String endDate) {
        if (properties.isStub()) {
            return stubWechatMpOfficialArticleTotalDetail(collectorAccountId);
        }
        JSONObject data = getJson("/api/v1/internal/wechat-mp/official/article-total-detail?account_id="
                + collectorAccountId + "&begin_date=" + beginDate + "&end_date=" + endDate);
        return data == null ? Collections.emptyMap() : data;
    }

    /** 官方 API：发表内容概况总数据（getbizsummary，最长 30 天） */
    public Map<String, Object> getWechatMpOfficialBizSummary(String collectorAccountId,
                                                              String beginDate, String endDate) {
        if (properties.isStub()) {
            return stubWechatMpOfficialBizSummary(collectorAccountId);
        }
        JSONObject data = getJson("/api/v1/internal/wechat-mp/official/biz-summary?account_id="
                + collectorAccountId + "&begin_date=" + beginDate + "&end_date=" + endDate);
        return data == null ? Collections.emptyMap() : data;
    }

    public Map<String, Object> getWechatMpPublishList(String collectorAccountId) {
        if (properties.isStub()) {
            return stubWechatMpPublishList(collectorAccountId);
        }
        JSONObject data = getJson("/api/v1/internal/wechat-mp/publish-list?account_id=" + collectorAccountId
                + "&begin=0&end=20");
        return data == null ? Collections.emptyMap() : data;
    }

    public Map<String, Object> getWechatMpArticleList(String collectorAccountId, String fakeid) {
        if (properties.isStub()) {
            return stubWechatMpArticleList(collectorAccountId);
        }
        String path = "/api/v1/internal/wechat-mp/article-list?account_id=" + collectorAccountId
                + "&fakeid=" + fakeid;
        JSONObject data = getJson(path);
        return data == null ? Collections.emptyMap() : data;
    }

    public Map<String, Object> getWechatMpArticleData(String collectorAccountId, String msgid, String publishDate) {
        if (properties.isStub()) {
            return stubWechatMpArticleData(collectorAccountId, msgid);
        }
        StringBuilder path = new StringBuilder("/api/v1/internal/wechat-mp/article-data?account_id=")
                .append(collectorAccountId)
                .append("&publish_date=").append(publishDate);
        if (StrUtil.isNotBlank(msgid) && msgid.contains("_")) {
            path.append("&msgid=").append(msgid);
        } else if (StrUtil.isNotBlank(msgid)) {
            path.append("&appmsgid=").append(msgid).append("&idx=1");
        }
        JSONObject data = getJson(path.toString());
        return data == null ? Collections.emptyMap() : data;
    }

    public Map<String, Object> getWechatMpArticleDownload(String collectorAccountId, String url) {
        if (properties.isStub()) {
            return stubWechatMpArticleDownload(collectorAccountId, url);
        }
        String path = "/api/v1/internal/wechat-mp/article-download?account_id=" + collectorAccountId
                + "&url=" + url;
        JSONObject data = getJson(path);
        return data == null ? Collections.emptyMap() : data;
    }

    public Map<String, Object> getDouyinFollowerStats(String collectorAccountId) {
        if (properties.isStub()) {
            return stubDouyinFollowerStats(collectorAccountId);
        }
        JSONObject data = getJson("/api/v1/internal/douyin/follower-stats?account_id=" + collectorAccountId);
        return data == null ? Collections.emptyMap() : data;
    }

    public Map<String, Object> getDouyinFollowerList(String collectorAccountId, long cursor, int pageSize) {
        if (properties.isStub()) {
            return stubDouyinFollowerList(collectorAccountId);
        }
        String path = "/api/v1/internal/douyin/follower-list?account_id=" + collectorAccountId
                + "&cursor=" + cursor + "&page_size=" + pageSize;
        JSONObject data = getJson(path);
        return data == null ? Collections.emptyMap() : data;
    }

    public Map<String, Object> getDouyinVideoList(String collectorAccountId, long cursor, int pageSize) {
        if (properties.isStub()) {
            return stubDouyinVideoList(collectorAccountId);
        }
        String path = "/api/v1/internal/douyin/video-list?account_id=" + collectorAccountId
                + "&cursor=" + cursor + "&page_size=" + pageSize;
        JSONObject data = getJson(path);
        return data == null ? Collections.emptyMap() : data;
    }

    public Map<String, Object> getDouyinVideoStats(String collectorAccountId, String itemId, String videoUrl) {
        if (properties.isStub()) {
            return stubDouyinVideoStats(collectorAccountId, itemId);
        }
        StringBuilder path = new StringBuilder("/api/v1/internal/douyin/video-stats?account_id=")
                .append(collectorAccountId);
        if (StrUtil.isNotBlank(itemId)) {
            path.append("&item_id=").append(itemId);
        }
        if (StrUtil.isNotBlank(videoUrl)) {
            path.append("&video_url=").append(videoUrl);
        }
        JSONObject data = getJson(path.toString());
        return data == null ? Collections.emptyMap() : data;
    }

    public Map<String, Object> getKuaishouFollowerStats(String collectorAccountId) {
        if (properties.isStub()) {
            return stubKuaishouFollowerStats(collectorAccountId);
        }
        JSONObject data = getJson("/api/v1/internal/kuaishou/follower-stats?account_id=" + collectorAccountId);
        return data == null ? Collections.emptyMap() : data;
    }

    public Map<String, Object> getKuaishouVideoList(String collectorAccountId, String cursor, int pageSize) {
        if (properties.isStub()) {
            return stubKuaishouVideoList(collectorAccountId);
        }
        String path = "/api/v1/internal/kuaishou/video-list?account_id=" + collectorAccountId
                + "&cursor=" + StrUtil.blankToDefault(cursor, "")
                + "&page_size=" + pageSize;
        JSONObject data = getJson(path);
        return data == null ? Collections.emptyMap() : data;
    }

    public Map<String, Object> getKuaishouVideoStats(String collectorAccountId, String videoUrl, String photoId) {
        if (properties.isStub()) {
            return stubKuaishouVideoStats(collectorAccountId, photoId);
        }
        StringBuilder path = new StringBuilder("/api/v1/internal/kuaishou/video-stats?account_id=")
                .append(collectorAccountId);
        if (StrUtil.isNotBlank(photoId)) {
            path.append("&photo_id=").append(photoId);
        }
        if (StrUtil.isNotBlank(videoUrl)) {
            path.append("&video_url=").append(videoUrl);
        }
        JSONObject data = getJson(path.toString());
        return data == null ? Collections.emptyMap() : data;
    }

    public Map<String, Object> getWechatVideoFollowerStats(String collectorAccountId) {
        if (properties.isStub()) {
            return stubWechatVideoFollowerStats(collectorAccountId);
        }
        JSONObject data = getJson("/api/v1/internal/wechat-channels/follower-stats?account_id=" + collectorAccountId);
        return data == null ? Collections.emptyMap() : data;
    }

    public Map<String, Object> getWechatVideoList(String collectorAccountId, String cursor, int pageSize) {
        if (properties.isStub()) {
            return stubWechatVideoList(collectorAccountId);
        }
        String path = "/api/v1/internal/wechat-channels/video-list?account_id=" + collectorAccountId
                + "&cursor=" + StrUtil.blankToDefault(cursor, "")
                + "&page_size=" + pageSize;
        JSONObject data = getJson(path);
        return data == null ? Collections.emptyMap() : data;
    }

    public Map<String, Object> getWechatVideoStats(String collectorAccountId, String videoUrl, String objectId) {
        if (properties.isStub()) {
            return stubWechatVideoStats(collectorAccountId, objectId);
        }
        StringBuilder path = new StringBuilder("/api/v1/internal/wechat-channels/video-stats?account_id=")
                .append(collectorAccountId);
        if (StrUtil.isNotBlank(objectId)) {
            path.append("&object_id=").append(objectId);
        }
        if (StrUtil.isNotBlank(videoUrl)) {
            path.append("&video_url=").append(videoUrl);
        }
        JSONObject data = getJson(path.toString());
        return data == null ? Collections.emptyMap() : data;
    }

    public Map<String, Object> getXiaohongshuFollowerStats(String collectorAccountId) {
        if (properties.isStub()) {
            return stubXiaohongshuFollowerStats(collectorAccountId);
        }
        JSONObject data = getJson("/api/v1/internal/xiaohongshu/follower-stats?account_id=" + collectorAccountId);
        return data == null ? Collections.emptyMap() : data;
    }

    public Map<String, Object> getXiaohongshuNoteList(String collectorAccountId, String cursor, int pageSize) {
        if (properties.isStub()) {
            return stubXiaohongshuNoteList(collectorAccountId);
        }
        String path = "/api/v1/internal/xiaohongshu/video-list?account_id=" + collectorAccountId
                + "&cursor=" + StrUtil.blankToDefault(cursor, "")
                + "&page_size=" + pageSize;
        JSONObject data = getJson(path);
        return data == null ? Collections.emptyMap() : data;
    }

    public Map<String, Object> getXiaohongshuNoteStats(String collectorAccountId, String noteId, String xsecToken) {
        if (properties.isStub()) {
            return stubXiaohongshuNoteStats(collectorAccountId, noteId);
        }
        String path = "/api/v1/internal/xiaohongshu/video-stats?account_id=" + collectorAccountId
                + "&note_id=" + noteId + "&xsec_token=" + xsecToken;
        JSONObject data = getJson(path);
        return data == null ? Collections.emptyMap() : data;
    }

    public Map<String, Object> getBilibiliFollowerStats(String collectorAccountId) {
        if (properties.isStub()) {
            return stubBilibiliFollowerStats(collectorAccountId);
        }
        // unify-collector-api 无 follower-stats；粉丝数在 GET /user/me（Header X-Account-Id）
        JSONObject data = getJsonWithAccountId("/api/v1/internal/bilibili/user/me", collectorAccountId);
        return data == null ? Collections.emptyMap() : data;
    }

    /** POST /api/v1/auth/qrcode — 启动统一扫码登录 */
    public Map<String, Object> startQrLogin(String collectorPlatform) {
        if (properties.isStub()) {
            return stubQrStart(collectorPlatform);
        }
        JSONObject body = JSONUtil.createObj().set("platform", collectorPlatform);
        JSONObject data = postJsonWithTimeout("/api/v1/auth/qrcode", body, qrStartTimeoutMs());
        return data == null ? Collections.emptyMap() : data;
    }

    /** GET /api/v1/auth/poll — 轮询扫码状态 */
    public Map<String, Object> pollQrLogin(String sessionId) {
        if (properties.isStub()) {
            return stubQrPoll(sessionId);
        }
        JSONObject data = getJson("/api/v1/auth/poll?session_id=" + sessionId);
        return data == null ? Collections.emptyMap() : data;
    }

    /** DELETE /api/v1/auth/qrcode — 取消扫码 */
    public void cancelQrLogin(String sessionId) {
        if (properties.isStub()) {
            return;
        }
        delete("/api/v1/auth/qrcode?session_id=" + sessionId);
    }

    /** GET /livez — 用于 Live Smoke IT 与运维探活（不经 JSON envelope） */
    public boolean checkLive() {
        if (properties.isStub()) {
            return true;
        }
        try {
            HttpResponse response = executeRequest(HttpRequest.get(normalizeBaseUrl() + "/livez")
                    .timeout(properties.getTimeoutMs()));
            return response.getStatus() >= 200 && response.getStatus() < 300;
        } catch (UnifiedCollectorApiException ex) {
            return false;
        }
    }

    private UnifiedCollectorImportResult stubImport(String platform) {
        UnifiedCollectorImportResult result = new UnifiedCollectorImportResult();
        result.setAccountId("acc_" + platform + "_stub001");
        result.setPlatform(platform);
        result.setStatus("active");
        return result;
    }

    private Map<String, Object> stubAccount(String collectorAccountId) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("account_id", collectorAccountId);
        map.put("status", "active");
        return map;
    }

    private Map<String, Object> stubHealthEntry(String collectorAccountId) {
        if (collectorAccountId != null && collectorAccountId.contains("fail")) {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("account_id", collectorAccountId);
            map.put("status", "relogin_needed");
            map.put("alive", false);
            return map;
        }
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("account_id", collectorAccountId);
        map.put("status", "active");
        map.put("alive", true);
        return map;
    }

    private Map<String, Object> stubDouyinFollowerStats(String collectorAccountId) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("account_id", collectorAccountId);
        map.put("total_followers", 125_000);
        map.put("follower_count", 125_000);
        map.put("following_count", 320);
        map.put("total_favorited", 8_900_000);
        return map;
    }

    private Map<String, Object> stubDouyinFollowerList(String collectorAccountId) {
        Map<String, Object> followerA = new LinkedHashMap<>();
        followerA.put("follower_id", "MS4wLjABStubFollower001");
        followerA.put("open_id", "MS4wLjABStubFollower001");
        followerA.put("nickname", "Stub抖音粉丝A");
        followerA.put("avatar_url", "https://example.com/dy-a.png");

        Map<String, Object> followerB = new LinkedHashMap<>();
        followerB.put("follower_id", "MS4wLjABStubFollower002");
        followerB.put("open_id", "MS4wLjABStubFollower002");
        followerB.put("nickname", "Stub抖音粉丝B");

        Map<String, Object> map = new LinkedHashMap<>();
        map.put("account_id", collectorAccountId);
        map.put("total", 2);
        map.put("has_more", false);
        map.put("cursor", "0");
        map.put("followers", List.of(followerA, followerB));
        return map;
    }

    private Map<String, Object> stubDouyinVideoList(String collectorAccountId) {
        Map<String, Object> videoA = new LinkedHashMap<>();
        videoA.put("video_id", "stub_douyin_video_001");
        videoA.put("item_id", "stub_douyin_video_001");
        videoA.put("title", "Stub抖音作品A");
        videoA.put("description", "Stub抖音作品A");
        videoA.put("video_url", "https://www.douyin.com/video/stub_douyin_video_001");
        videoA.put("cover_url", "https://example.com/dy-cover-a.png");
        videoA.put("duration", 15);
        videoA.put("publish_time", 1_700_300_000);
        videoA.put("play_count", 12_000);
        videoA.put("like_count", 800);
        videoA.put("share_count", 50);
        videoA.put("comment_count", 120);
        videoA.put("collect_count", 30);

        Map<String, Object> videoB = new LinkedHashMap<>();
        videoB.put("video_id", "stub_douyin_video_002");
        videoB.put("item_id", "stub_douyin_video_002");
        videoB.put("title", "Stub抖音作品B");
        videoB.put("video_url", "https://www.douyin.com/video/stub_douyin_video_002");
        videoB.put("publish_time", 1_700_400_000);
        videoB.put("play_count", 8_500);
        videoB.put("like_count", 420);
        videoB.put("share_count", 18);
        videoB.put("comment_count", 66);
        videoB.put("collect_count", 12);

        Map<String, Object> map = new LinkedHashMap<>();
        map.put("account_id", collectorAccountId);
        map.put("total", 2);
        map.put("has_more", false);
        map.put("cursor", "0");
        map.put("videos", List.of(videoA, videoB));
        return map;
    }

    private Map<String, Object> stubDouyinVideoStats(String collectorAccountId, String itemId) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("account_id", collectorAccountId);
        map.put("video_id", StrUtil.blankToDefault(itemId, "stub_douyin_video_001"));
        map.put("title", "Stub抖音作品A");
        map.put("play_count", 56_000);
        map.put("like_count", 3_200);
        map.put("share_count", 180);
        map.put("comment_count", 420);
        map.put("collect_count", 95);
        return map;
    }

    private Map<String, Object> stubKuaishouFollowerStats(String collectorAccountId) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("account_id", collectorAccountId);
        map.put("fans_count", 88_800);
        return map;
    }

    private Map<String, Object> stubWechatVideoFollowerStats(String collectorAccountId) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("account_id", collectorAccountId);
        map.put("follower_count", 45_600);
        return map;
    }

    private Map<String, Object> stubXiaohongshuFollowerStats(String collectorAccountId) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("account_id", collectorAccountId);
        map.put("fans_count", 12_500);
        return map;
    }

    private Map<String, Object> stubBilibiliFollowerStats(String collectorAccountId) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("account_id", collectorAccountId);
        map.put("follower_count", 99_000);
        return map;
    }

    private Map<String, Object> stubWechatMpFollowerList(String collectorAccountId) {
        Map<String, Object> followerA = new LinkedHashMap<>();
        followerA.put("openid", "oStubFollower001");
        followerA.put("nickname", "Stub粉丝A");
        followerA.put("subscribe_time", 1_700_000_000);
        followerA.put("headimgurl", "https://example.com/a.png");
        followerA.put("unionid", "uStub001");

        Map<String, Object> followerB = new LinkedHashMap<>();
        followerB.put("openid", "oStubFollower002");
        followerB.put("nickname", "Stub粉丝B");
        followerB.put("subscribe_time", 1_700_001_000);

        Map<String, Object> map = new LinkedHashMap<>();
        map.put("account_id", collectorAccountId);
        map.put("total", 2);
        map.put("followers", List.of(followerA, followerB));
        return map;
    }

    private Map<String, Object> stubWechatMpOfficialFollowerPage(String collectorAccountId, int page) {
        if (page > 1) {
            Map<String, Object> empty = new LinkedHashMap<>();
            empty.put("total", 2);
            empty.put("count", 0);
            empty.put("openids", List.of());
            empty.put("next_openid", "");
            return empty;
        }
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("total", 2);
        map.put("count", 2);
        map.put("openids", List.of("oStubFollower001", "oStubFollower002"));
        map.put("next_openid", "");
        return map;
    }

    private Map<String, Object> stubWechatMpOfficialFollowerBatch(String collectorAccountId, List<String> openids) {
        List<Map<String, Object>> users = new ArrayList<>();
        for (String openid : openids) {
            Map<String, Object> user = new LinkedHashMap<>();
            user.put("openid", openid);
            user.put("nickname", "Stub粉丝-" + openid.substring(Math.max(0, openid.length() - 4)));
            user.put("subscribe_time", 1_700_000_000);
            user.put("headimgurl", "https://example.com/a.png");
            users.add(user);
        }
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("count", users.size());
        map.put("users", users);
        return map;
    }

    private Map<String, Object> stubWechatMpOfficialUserCumulate(String collectorAccountId) {
        Map<String, Object> stat = new LinkedHashMap<>();
        stat.put("ref_date", "2026-06-24");
        stat.put("cumulate_user", 50_000);
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("stats", List.of(stat));
        return map;
    }

    private Map<String, Object> stubWechatMpOfficialArticleStats(String collectorAccountId) {
        Map<String, Object> article = new LinkedHashMap<>();
        article.put("title", "Stub图文A");
        article.put("int_page_read_user", 12_500);
        article.put("share_user", 45);
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("articles", List.of(article));
        return map;
    }

    private Map<String, Object> stubWechatMpOfficialMaterialList(String collectorAccountId) {
        Map<String, Object> newsItem = new LinkedHashMap<>();
        newsItem.put("title", "Stub官方素材A");
        newsItem.put("url", "https://mp.weixin.qq.com/s/stub_official_001");
        newsItem.put("thumb_url", "https://example.com/cover-official.png");
        newsItem.put("digest", "Stub官方素材摘要");
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("media_id", "stub_media_001");
        item.put("update_time", 1_700_100_000);
        item.put("news_items", List.of(newsItem));
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("total_count", 1);
        map.put("item_count", 1);
        map.put("items", List.of(item));
        return map;
    }

    private Map<String, Object> stubWechatMpOfficialFreepublishList(String collectorAccountId) {
        Map<String, Object> newsItem = new LinkedHashMap<>();
        newsItem.put("title", "Stub已发布图文A");
        newsItem.put("url", "https://mp.weixin.qq.com/s/stub_freepublish_001");
        newsItem.put("thumb_url", "https://example.com/cover-fp.png");
        newsItem.put("digest", "Stub已发布摘要");
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("article_id", "stub_fp_article_001");
        item.put("update_time", 1_700_100_000);
        item.put("news_items", List.of(newsItem));
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("total_count", 1);
        map.put("item_count", 1);
        map.put("items", List.of(item));
        return map;
    }

    private Map<String, Object> stubWechatMpOfficialFreepublishArticle(String collectorAccountId, String articleId) {
        Map<String, Object> newsItem = new LinkedHashMap<>();
        newsItem.put("title", "Stub已发布图文A");
        newsItem.put("content", "<p>Stub公众号正文HTML</p>");
        newsItem.put("digest", "Stub已发布摘要");
        newsItem.put("url", "https://mp.weixin.qq.com/s/stub_freepublish_001");
        newsItem.put("thumb_url", "https://example.com/cover-fp.png");
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("article_id", StrUtil.blankToDefault(articleId, "stub_fp_article_001"));
        map.put("news_items", List.of(newsItem));
        return map;
    }

    private Map<String, Object> stubWechatMpOfficialArticleTotalDetail(String collectorAccountId) {
        Map<String, Object> detail = new LinkedHashMap<>();
        detail.put("stat_date", "2026-06-24");
        detail.put("read_user", 12_500);
        detail.put("share_user", 45);
        detail.put("like_user", 320);
        detail.put("zaikan_user", 88);
        detail.put("comment_count", 12);
        Map<String, Object> article = new LinkedHashMap<>();
        article.put("ref_date", "2026-06-24");
        article.put("msgid", "2651883347_1");
        article.put("title", "Stub已发布图文A");
        article.put("content_url", "https://mp.weixin.qq.com/s/stub_freepublish_001");
        article.put("detail_list", List.of(detail));
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("count", 1);
        map.put("articles", List.of(article));
        return map;
    }

    private Map<String, Object> stubWechatMpOfficialBizSummary(String collectorAccountId) {
        Map<String, Object> stat = new LinkedHashMap<>();
        stat.put("ref_date", "2026-06-24");
        stat.put("read_user", 50_000);
        stat.put("share_user", 200);
        stat.put("like_user", 1_200);
        stat.put("send_page_count", 5);
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("count", 1);
        map.put("stats", List.of(stat));
        return map;
    }

    private Map<String, Object> stubWechatMpArticleList(String collectorAccountId) {
        Map<String, Object> articleA = new LinkedHashMap<>();
        articleA.put("article_id", "stub_article_001");
        articleA.put("title", "Stub图文A");
        articleA.put("url", "https://mp.weixin.qq.com/s/stub001");
        articleA.put("cover_url", "https://example.com/cover-a.png");
        articleA.put("publish_time", 1_700_100_000);
        articleA.put("read_count", 12_500);
        articleA.put("like_count", 320);
        articleA.put("share_count", 45);

        Map<String, Object> articleB = new LinkedHashMap<>();
        articleB.put("article_id", "stub_article_002");
        articleB.put("title", "Stub图文B");
        articleB.put("url", "https://mp.weixin.qq.com/s/stub002");
        articleB.put("publish_time", 1_700_200_000);
        articleB.put("read_count", 8_800);

        Map<String, Object> map = new LinkedHashMap<>();
        map.put("account_id", collectorAccountId);
        map.put("total", 2);
        map.put("articles", List.of(articleA, articleB));
        return map;
    }

    private Map<String, Object> stubWechatMpFollowerStats(String collectorAccountId) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("account_id", collectorAccountId);
        map.put("total_followers", 50_000);
        return map;
    }

    private Map<String, Object> stubWechatMpPublishList(String collectorAccountId) {
        return stubWechatMpArticleList(collectorAccountId);
    }

    private Map<String, Object> stubWechatMpArticleData(String collectorAccountId, String msgid) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("account_id", collectorAccountId);
        map.put("msgid", msgid);
        map.put("read_count", 15_800);
        map.put("like_count", 420);
        map.put("share_count", 88);
        return map;
    }

    private Map<String, Object> stubWechatMpArticleDownload(String collectorAccountId, String url) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("account_id", collectorAccountId);
        map.put("url", url);
        map.put("title", "Stub图文正文");
        map.put("content_text", "Stub公众号正文内容");
        return map;
    }

    private Map<String, Object> stubKuaishouVideoList(String collectorAccountId) {
        Map<String, Object> videoA = new LinkedHashMap<>();
        videoA.put("photo_id", "stub_ks_video_001");
        videoA.put("title", "Stub快手作品A");
        videoA.put("video_url", "https://www.kuaishou.com/short-video/stub001");
        videoA.put("publish_time", 1_700_300_000);
        videoA.put("play_count", 12_000);

        Map<String, Object> map = new LinkedHashMap<>();
        map.put("account_id", collectorAccountId);
        map.put("total", 1);
        map.put("has_more", false);
        map.put("videos", List.of(videoA));
        return map;
    }

    private Map<String, Object> stubKuaishouVideoStats(String collectorAccountId, String photoId) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("account_id", collectorAccountId);
        map.put("photo_id", StrUtil.blankToDefault(photoId, "stub_ks_video_001"));
        map.put("play_count", 18_500);
        map.put("like_count", 960);
        map.put("comment_count", 120);
        return map;
    }

    private Map<String, Object> stubWechatVideoList(String collectorAccountId) {
        Map<String, Object> videoA = new LinkedHashMap<>();
        videoA.put("export_id", "stub_wv_video_001");
        videoA.put("title", "Stub视频号作品A");
        videoA.put("video_url", "https://channels.weixin.qq.com/stub001");
        videoA.put("read_count", 8_800);
        videoA.put("publish_time", 1_700_300_000);

        Map<String, Object> map = new LinkedHashMap<>();
        map.put("account_id", collectorAccountId);
        map.put("total", 1);
        map.put("has_more", false);
        map.put("videos", List.of(videoA));
        return map;
    }

    private Map<String, Object> stubWechatVideoStats(String collectorAccountId, String objectId) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("account_id", collectorAccountId);
        map.put("video_id", StrUtil.blankToDefault(objectId, "stub_wv_video_001"));
        map.put("play_count", 9_200);
        map.put("like_count", 520);
        map.put("comment_count", 66);
        return map;
    }

    private Map<String, Object> stubXiaohongshuNoteList(String collectorAccountId) {
        Map<String, Object> noteA = new LinkedHashMap<>();
        noteA.put("note_id", "stub_xhs_note_001");
        noteA.put("xsec_token", "stub_xsec_token_001");
        noteA.put("title", "Stub小红书笔记A");
        noteA.put("note_url", "https://www.xiaohongshu.com/explore/stub001");
        noteA.put("liked_count", 320);

        Map<String, Object> map = new LinkedHashMap<>();
        map.put("account_id", collectorAccountId);
        map.put("total", 1);
        map.put("has_more", false);
        map.put("notes", List.of(noteA));
        return map;
    }

    private Map<String, Object> stubXiaohongshuNoteStats(String collectorAccountId, String noteId) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("account_id", collectorAccountId);
        map.put("note_id", StrUtil.blankToDefault(noteId, "stub_xhs_note_001"));
        map.put("liked_count", 450);
        map.put("collected_count", 88);
        map.put("comment_count", 36);
        return map;
    }

    private JSONObject postJson(String path, JSONObject body) {
        return postJsonWithTimeout(path, body, properties.getTimeoutMs());
    }

    private JSONObject postJsonWithTimeout(String path, JSONObject body, int timeoutMs) {
        HttpResponse response = executeRequest(HttpRequest.post(normalizeBaseUrl() + path)
                .timeout(timeoutMs)
                .header("Content-Type", "application/json")
                .body(body.toString()));
        return parseEnvelope(response);
    }

    private int qrStartTimeoutMs() {
        return Math.max(properties.getTimeoutMs(), 120_000);
    }

    /** 1x1 透明 PNG */
    private static final String STUB_QR_PNG_BASE64 =
            "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mP8z8BQDwAEhQGAhKmMIQAAAABJRU5ErkJggg==";

    private final java.util.concurrent.ConcurrentHashMap<String, Integer> stubQrPollCounts =
            new java.util.concurrent.ConcurrentHashMap<>();

    private Map<String, Object> stubQrStart(String collectorPlatform) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("session_id", "stub_qr_" + collectorPlatform + "_001");
        map.put("status", "pending");
        map.put("qrcode_base64", STUB_QR_PNG_BASE64);
        map.put("expires_in_seconds", 300);
        map.put("message", "stub 扫码会话");
        return map;
    }

    private Map<String, Object> stubQrPoll(String sessionId) {
        int count = stubQrPollCounts.merge(sessionId, 1, Integer::sum);
        Map<String, Object> map = new LinkedHashMap<>();
        if (count < 2) {
            map.put("status", "pending");
            map.put("message", "等待扫码");
            return map;
        }
        map.put("status", "confirmed");
        map.put("message", "登录成功");
        map.put("account_id", "acc_stub_qr_confirmed");
        Map<String, Object> credential = new LinkedHashMap<>();
        credential.put("cookie", "stub_cookie=1; sessionid=stub_session");
        credential.put("token", "stub_mp_token");
        map.put("credential", credential);
        stubQrPollCounts.remove(sessionId);
        return map;
    }

    private JSONObject getJson(String path) {
        return getJsonWithAccountId(path, null);
    }

    private JSONObject getJsonWithAccountId(String path, String collectorAccountId) {
        HttpRequest request = HttpRequest.get(normalizeBaseUrl() + path)
                .timeout(properties.getTimeoutMs());
        if (StrUtil.isNotBlank(collectorAccountId)) {
            request.header("X-Account-Id", collectorAccountId);
        }
        HttpResponse response = executeRequest(request);
        return parseEnvelope(response);
    }

    private void delete(String path) {
        HttpResponse response = executeRequest(HttpRequest.delete(normalizeBaseUrl() + path)
                .timeout(properties.getTimeoutMs()));
        parseEnvelope(response);
    }

    private HttpResponse executeRequest(HttpRequest request) {
        try {
            return authorized(request).execute();
        } catch (IORuntimeException ex) {
            throw toConnectionException(ex);
        }
    }

    private UnifiedCollectorApiException toConnectionException(IORuntimeException ex) {
        String detail = ex.getCause() != null ? ex.getCause().getMessage() : ex.getMessage();
        String message = "Collector 服务不可达，请确认 unify-collector-api 已启动（:8000）";
        if (StrUtil.isNotBlank(detail)) {
            message = message + "：" + detail;
        }
        return new UnifiedCollectorApiException("DISCONNECTED", message);
    }

    private HttpRequest authorized(HttpRequest request) {
        if (StrUtil.isNotBlank(properties.getApiToken())) {
            request.header("Authorization", "Bearer " + properties.getApiToken());
        }
        return request;
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
            if (StrUtil.isBlank(message)) {
                message = "Collector API 请求失败 HTTP " + status + " (code=" + code + ")";
            }
            String connStatus = code == 40002 ? "TOKEN_FAIL" : "DISCONNECTED";
            throw new UnifiedCollectorApiException(connStatus, message, code);
        }
        return unwrapNestedData(root.getJSONObject("data"));
    }

    /**
     * 部分平台路由二次包裹 envelope（如 kuaishou internal_api 返回 ok(data=ok(...))）。
     */
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

    private UnifiedCollectorImportResult toImportResult(JSONObject data) {
        if (data == null) {
            throw new UnifiedCollectorApiException("DISCONNECTED", "Collector import 无 data");
        }
        UnifiedCollectorImportResult result = new UnifiedCollectorImportResult();
        result.setAccountId(data.getStr("account_id"));
        result.setPlatform(data.getStr("platform"));
        result.setStatus(data.getStr("status"));
        if (StrUtil.isBlank(result.getAccountId())) {
            throw new UnifiedCollectorApiException("DISCONNECTED", "Collector import 未返回 account_id");
        }
        return result;
    }

    private String normalizeBaseUrl() {
        return StrUtil.removeSuffix(StrUtil.blankToDefault(properties.getBaseUrl(), "").trim(), "/");
    }
}
