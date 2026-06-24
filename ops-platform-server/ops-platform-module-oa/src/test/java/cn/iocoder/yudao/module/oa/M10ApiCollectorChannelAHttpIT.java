package cn.iocoder.yudao.module.oa;

import cn.iocoder.yudao.module.oa.service.collect.unified.UnifiedCollectorApiClient;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Channel-A HTTP 契约：MockWebServer 验证 OA 客户端路径/Header 与 unify-collector-api 对齐。
 *
 * <p>默认 CI 运行（stub=false + MockWebServer）。真实 collector 全链路见 README「Channel-A 手动 E2E」
 * 与 {@link M10ApiCollectorLiveSmokeIT}（{@code UNIFY_COLLECTOR_LIVE=true}）。
 */
@Execution(ExecutionMode.SAME_THREAD)
class M10ApiCollectorChannelAHttpIT extends OaITBase {

    private static final String COLLECTOR_ACCOUNT_ID = "acc_wechat_mp_mock001";
    private static final String FAKEID = "fakeid_seed001";

    static final MockWebServer server;

    static {
        try {
            server = new MockWebServer();
            server.start();
        } catch (IOException ex) {
            throw new ExceptionInInitializerError(ex);
        }
    }

    @Autowired
    private UnifiedCollectorApiClient unifiedCollectorApiClient;

    @AfterAll
    static void stopServer() throws IOException {
        server.shutdown();
    }

    @DynamicPropertySource
    static void registerProperties(DynamicPropertyRegistry registry) {
        registry.add("oa.unified-collector.base-url",
                () -> server.url("/").toString().replaceAll("/$", ""));
        registry.add("oa.unified-collector.api-token", () -> "test-key-2026");
        registry.add("oa.unified-collector.stub", () -> "false");
    }

    @BeforeEach
    void drainPendingRequests() throws InterruptedException {
        while (server.takeRequest(50, TimeUnit.MILLISECONDS) != null) {
            // discard stale requests from prior tests
        }
    }

    @Test
    @DisplayName("Channel-A: wechat-mp follower-list 路径与鉴权")
    void wechatMpFollowerListPath() throws Exception {
        server.enqueue(envelope("""
                {"account_id":"%s","total":1,"followers":[{"openid":"o001","nickname":"粉丝A"}]}
                """.formatted(COLLECTOR_ACCOUNT_ID)));

        Map<String, Object> data = unifiedCollectorApiClient.getWechatMpFollowerList(COLLECTOR_ACCOUNT_ID);
        assertEquals(1, data.get("total"));

        RecordedRequest req = server.takeRequest(5, TimeUnit.SECONDS);
        assertNotNull(req);
        assertEquals("GET", req.getMethod());
        assertTrue(req.getPath().contains("/api/v1/internal/wechat-mp/follower-list"));
        assertTrue(req.getPath().contains("account_id=" + COLLECTOR_ACCOUNT_ID));
        assertEquals("Bearer test-key-2026", req.getHeader("Authorization"));
    }

    @Test
    @DisplayName("Channel-A: wechat-mp article-list 含 fakeid")
    void wechatMpArticleListPath() throws Exception {
        server.enqueue(envelope("""
                {"total":1,"articles":[{"article_id":"a001","title":"图文A"}]}
                """));

        Map<String, Object> data = unifiedCollectorApiClient.getWechatMpArticleList(COLLECTOR_ACCOUNT_ID, FAKEID);
        assertEquals(1, data.get("total"));

        RecordedRequest req = server.takeRequest(5, TimeUnit.SECONDS);
        assertNotNull(req);
        assertTrue(req.getPath().contains("/api/v1/internal/wechat-mp/article-list"));
        assertTrue(req.getPath().contains("fakeid=" + FAKEID));
    }

    @Test
    @DisplayName("Channel-A: douyin follower-stats 返回 total_followers")
    void douyinFollowerStatsPath() throws Exception {
        server.enqueue(envelope("""
                {"total_followers":125000,"new_followers_today":null}
                """));

        Map<String, Object> data = unifiedCollectorApiClient.getDouyinFollowerStats(COLLECTOR_ACCOUNT_ID);
        assertEquals(125_000, data.get("total_followers"));

        RecordedRequest req = server.takeRequest(5, TimeUnit.SECONDS);
        assertNotNull(req);
        assertTrue(req.getPath().contains("/api/v1/internal/douyin/follower-stats"));
    }

    @Test
    @DisplayName("Channel-A: kuaishou follower-stats 解嵌套 envelope")
    void kuaishouFollowerStatsUnwrapsNestedEnvelope() throws Exception {
        server.enqueue(envelope("""
                {"code":0,"message":"success","data":{"total_followers":88800,"user_name":"KS"}}
                """));

        Map<String, Object> data = unifiedCollectorApiClient.getKuaishouFollowerStats(COLLECTOR_ACCOUNT_ID);
        assertEquals(88_800, data.get("total_followers"));
        assertEquals("KS", data.get("user_name"));
    }

    @Test
    @DisplayName("Channel-A: wechat-channels follower-stats")
    void wechatVideoFollowerStatsPath() throws Exception {
        server.enqueue(envelope("""
                {"total_followers":45600}
                """));

        Map<String, Object> data = unifiedCollectorApiClient.getWechatVideoFollowerStats(COLLECTOR_ACCOUNT_ID);
        assertEquals(45_600, data.get("total_followers"));

        RecordedRequest req = server.takeRequest(5, TimeUnit.SECONDS);
        assertNotNull(req);
        assertTrue(req.getPath().contains("/api/v1/internal/wechat-channels/follower-stats"));
    }

    @Test
    @DisplayName("Channel-A: xiaohongshu 使用 GET /user/me（无 follower-stats）")
    void xiaohongshuUsesUserMe() throws Exception {
        server.enqueue(envelope("""
                {"user_id":"xhs001","nickname":"小红书","followers":12500}
                """));

        Map<String, Object> data = unifiedCollectorApiClient.getXiaohongshuFollowerStats(COLLECTOR_ACCOUNT_ID);
        assertEquals(12_500, data.get("followers"));

        RecordedRequest req = server.takeRequest(5, TimeUnit.SECONDS);
        assertNotNull(req);
        assertEquals("/api/v1/internal/xiaohongshu/user/me", req.getPath());
    }

    @Test
    @DisplayName("Channel-A: bilibili 使用 GET /user/me + X-Account-Id")
    void bilibiliUsesUserMeWithAccountHeader() throws Exception {
        server.enqueue(envelope("""
                {"mid":123,"name":"UP主","follower":99000}
                """));

        Map<String, Object> data = unifiedCollectorApiClient.getBilibiliFollowerStats(COLLECTOR_ACCOUNT_ID);
        assertEquals(99_000, data.get("follower"));

        RecordedRequest req = server.takeRequest(5, TimeUnit.SECONDS);
        assertNotNull(req);
        assertEquals("/api/v1/internal/bilibili/user/me", req.getPath());
        assertEquals(COLLECTOR_ACCOUNT_ID, req.getHeader("X-Account-Id"));
    }

    private static MockResponse envelope(String dataJson) {
        return new MockResponse()
                .setBody("""
                        {"code":0,"message":"success","data":%s}
                        """.formatted(dataJson))
                .addHeader("Content-Type", "application/json");
    }
}
