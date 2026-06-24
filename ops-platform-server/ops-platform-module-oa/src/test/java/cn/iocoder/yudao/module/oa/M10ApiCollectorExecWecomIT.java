package cn.iocoder.yudao.module.oa;

import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.oa.dal.dataobject.collect.CollectTaskDO;
import cn.iocoder.yudao.module.oa.dal.dataobject.collect.WeworkDailyStatsDO;
import cn.iocoder.yudao.module.oa.dal.dataobject.personal.WeworkAccountDO;
import cn.iocoder.yudao.module.oa.dal.mysql.collect.WeworkDailyStatsMapper;
import cn.iocoder.yudao.module.oa.dal.mysql.personal.WeworkAccountMapper;
import cn.iocoder.yudao.module.oa.service.collect.CollectExecutionResult;
import cn.iocoder.yudao.module.oa.service.collect.CollectExecutionService;
import cn.iocoder.yudao.module.oa.service.collect.wework.WeComAdapter;
import cn.iocoder.yudao.module.oa.service.collect.wework.WeComApiClient;
import cn.iocoder.yudao.module.oa.util.AesUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import okhttp3.mockwebserver.Dispatcher;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.io.IOException;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * M10-WECOM-S-01: WeComAdapter 直连 OpenAPI + CollectExecutionService 路由（MockWebServer）。
 */
class M10ApiCollectorExecWecomIT extends OaITBase {

    private static final long TENANT_1 = 1L;
    private static final long WEWORK_ACCOUNT_ID = 9001L;

    static final MockWebServer server;

    static {
        try {
            server = new MockWebServer();
            server.setDispatcher(new Dispatcher() {
                @Override
                public MockResponse dispatch(RecordedRequest request) {
                    String path = request.getPath() == null ? "" : request.getPath();
                    if (path.contains("/cgi-bin/gettoken")) {
                        return jsonResponse("""
                                {"errcode":0,"errmsg":"ok","access_token":"mock_token","expires_in":7200}
                                """);
                    }
                    if (path.contains("/cgi-bin/externalcontact/list")) {
                        return jsonResponse("""
                                {"errcode":0,"errmsg":"ok","external_userid":["wo_a","wo_b","wo_c"]}
                                """);
                    }
                    if (path.contains("/cgi-bin/externalcontact/get_user_behavior_data")) {
                        return jsonResponse("""
                                {"errcode":0,"errmsg":"ok","behavior_data":[{"stat_time":1718985600,"chat_cnt":7,"message_cnt":21,"new_apply_cnt":1,"new_contact_cnt":2}]}
                                """);
                    }
                    return new MockResponse().setResponseCode(404);
                }
            });
            server.start();
        } catch (IOException ex) {
            throw new ExceptionInInitializerError(ex);
        }
    }

    private static MockResponse jsonResponse(String body) {
        return new MockResponse()
                .setBody(body)
                .addHeader("Content-Type", "application/json");
    }

    @Autowired
    private WeComAdapter weComAdapter;

    @Autowired
    private WeComApiClient weComApiClient;

    @Autowired
    private CollectExecutionService collectExecutionService;

    @Autowired
    private WeworkAccountMapper weworkAccountMapper;

    @Autowired
    private WeworkDailyStatsMapper weworkDailyStatsMapper;

    @Autowired
    private AesUtil aesUtil;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @AfterAll
    static void stopServer() throws IOException {
        server.shutdown();
    }

    @DynamicPropertySource
    static void registerProperties(DynamicPropertyRegistry registry) {
        registry.add("oa.wework.base-url", () -> server.url("/").toString().replaceAll("/$", ""));
        registry.add("oa.wework.stub", () -> "false");
    }

    @BeforeEach
    void setUp() {
        TenantContextHolder.setTenantId(TENANT_1);
        TenantContextHolder.setUsername("it-m10-wecom-s01");
        cleanupDailyStats();
        seedWeworkSecret();
        weComApiClient.clearTokenCacheForTest();
    }

    @AfterEach
    void tearDown() {
        cleanupDailyStats();
        TenantContextHolder.clear();
    }

    private void cleanupDailyStats() {
        jdbcTemplate.update(
                "DELETE FROM oa_wework_daily_stats WHERE tenant_id = ? AND wework_account_id = ?",
                TENANT_1, WEWORK_ACCOUNT_ID);
    }

    private void seedWeworkSecret() {
        WeworkAccountDO account = weworkAccountMapper.selectById(WEWORK_ACCOUNT_ID);
        account.setSecretEncrypted(aesUtil.encrypt("stub_wecom_secret"));
        weworkAccountMapper.updateById(account);
    }

    @Test
    @DisplayName("M10-WECOM-S-01: WeComAdapter 日统计落库 oa_wework_daily_stats")
    void adapterSyncDailyStatsViaMockHttp() {
        int count = weComAdapter.executeDailyStatsCollect(WEWORK_ACCOUNT_ID);
        assertEquals(1, count);

        WeworkDailyStatsDO stats = weworkDailyStatsMapper.selectOne(
                new LambdaQueryWrapper<WeworkDailyStatsDO>()
                        .eq(WeworkDailyStatsDO::getTenantId, TENANT_1)
                        .eq(WeworkDailyStatsDO::getWeworkAccountId, WEWORK_ACCOUNT_ID)
                        .eq(WeworkDailyStatsDO::getStatDate, LocalDate.now()));
        assertNotNull(stats);
        assertEquals(3, stats.getTotalFriends());
        assertEquals(7, stats.getTodayFriendInteractions());
        assertEquals(21, stats.getTodayMessagesSent());
        assertNotNull(stats.getSyncedAt());
    }

    @Test
    @DisplayName("M10-WECOM-S-01: CollectExecutionService 路由 WECOM_API + WEWORK")
    void executionServiceRoutesWeComApi() {
        CollectTaskDO task = new CollectTaskDO();
        task.setTenantId(TENANT_1);
        task.setPlatformType("WEWORK");
        task.setAccountId(WEWORK_ACCOUNT_ID);
        task.setMethod("INTERNAL");
        task.setSource("WECOM_API");
        task.setDataType("WECOM_DAILY_STATS");

        CollectExecutionResult result = collectExecutionService.execute(task);
        assertTrue(result.isSuccess());
        assertEquals(1, result.getRecordCount());

        WeworkDailyStatsDO stats = weworkDailyStatsMapper.selectOne(
                new LambdaQueryWrapper<WeworkDailyStatsDO>()
                        .eq(WeworkDailyStatsDO::getTenantId, TENANT_1)
                        .eq(WeworkDailyStatsDO::getWeworkAccountId, WEWORK_ACCOUNT_ID)
                        .eq(WeworkDailyStatsDO::getStatDate, LocalDate.now()));
        assertNotNull(stats);
        assertEquals(3, stats.getTotalFriends());
    }
}
