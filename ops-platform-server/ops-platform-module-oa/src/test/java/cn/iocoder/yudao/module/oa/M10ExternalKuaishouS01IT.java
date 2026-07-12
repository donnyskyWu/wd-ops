package cn.iocoder.yudao.module.oa;

import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.oa.dal.dataobject.collect.CollectTaskDO;
import cn.iocoder.yudao.module.oa.dal.dataobject.monitor.ExternalAccountDO;
import cn.iocoder.yudao.module.oa.dal.dataobject.monitor.ExternalWorkDO;
import cn.iocoder.yudao.module.oa.dal.mysql.monitor.ExternalAccountMapper;
import cn.iocoder.yudao.module.oa.dal.mysql.monitor.ExternalWorkMapper;
import cn.iocoder.yudao.module.oa.service.collect.CollectExecutionResult;
import cn.iocoder.yudao.module.oa.service.collect.CollectExecutionService;
import cn.iocoder.yudao.module.oa.service.collect.external.ExternalCollectorAdapter;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * GATE-EXT-P0 · 快手竞品 user-videos MockWebServer E2E 落库。
 */
@Execution(ExecutionMode.SAME_THREAD)
class M10ExternalKuaishouS01IT extends OaITBase {

    private static final long TENANT_1 = 1L;
    private static final String KUAISHOU_USER_ID = "ks_competitor_001";

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
    private ExternalCollectorAdapter externalCollectorAdapter;

    @Autowired
    private CollectExecutionService collectExecutionService;

    @Autowired
    private ExternalAccountMapper externalAccountMapper;

    @Autowired
    private ExternalWorkMapper externalWorkMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private Long collectConfigId;

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
        registry.add("oa.external-collector.kuaishou-cookie", () -> "kuaishou.web.cp.api_st=it_stub_token");
    }

    @BeforeEach
    void setUp() {
        TenantContextHolder.setTenantId(TENANT_1);
        TenantContextHolder.setUsername("it-m10-ext-kuaishou");
        cleanupArtifacts();
        collectConfigId = seedCollectConfig();
    }

    @AfterEach
    void tearDown() throws InterruptedException {
        cleanupArtifacts();
        while (server.takeRequest(50, TimeUnit.MILLISECONDS) != null) {
            // discard stale requests
        }
        TenantContextHolder.clear();
    }

    @Test
    @DisplayName("GATE-EXT-P0: 快手 user-videos 落库 oa_external_work + comment_count")
    void kuaishouUserVideosPersistExternalWorks() throws Exception {
        server.enqueue(envelope("""
                {
                  "user_id":"%s",
                  "user_name":"IT竞品达人",
                  "fan_count":666000,
                  "has_more":false,
                  "cursor":"",
                  "videos":[
                    {
                      "photo_id":"it_ks_photo_001",
                      "caption":"IT竞品作品A",
                      "share_url":"https://www.kuaishou.com/short-video/it_ks_photo_001",
                      "view_count":15800,
                      "like_count":960,
                      "comment_count":120,
                      "create_time":1700300000
                    },
                    {
                      "photo_id":"it_ks_photo_002",
                      "title":"IT竞品作品B",
                      "video_url":"https://www.kuaishou.com/short-video/it_ks_photo_002",
                      "view_count":9200,
                      "like_count":520,
                      "comment_count":66,
                      "create_time":1700400000
                    }
                  ]
                }
                """.formatted(KUAISHOU_USER_ID)));

        int count = externalCollectorAdapter.execute(collectConfigId, "EXT_KUAISHOU_USER_VIDEOS", "default");
        assertEquals(2, count);

        RecordedRequest req = server.takeRequest(5, TimeUnit.SECONDS);
        assertNotNull(req);
        assertEquals("GET", req.getMethod());
        assertTrue(req.getPath().contains("/api/v1/external/kuaishou/user-videos"));
        assertTrue(req.getPath().contains("user_id=" + KUAISHOU_USER_ID));
        assertEquals("Bearer test-key-2026", req.getHeader("Authorization"));
        assertTrue(req.getHeader("Cookie").contains("kuaishou.web.cp.api_st"));

        ExternalAccountDO account = externalAccountMapper.selectOne(
                new LambdaQueryWrapper<ExternalAccountDO>()
                        .eq(ExternalAccountDO::getCollectConfigId, collectConfigId));
        assertNotNull(account);
        assertEquals(KUAISHOU_USER_ID, account.getExternalUserId());
        assertEquals("IT竞品达人", account.getDisplayName());
        assertEquals(666_000L, account.getFollowerCount());

        List<ExternalWorkDO> works = externalWorkMapper.selectList(
                new LambdaQueryWrapper<ExternalWorkDO>()
                        .eq(ExternalWorkDO::getCollectConfigId, collectConfigId)
                        .orderByAsc(ExternalWorkDO::getPlatformWorkId));
        assertEquals(2, works.size());
        assertEquals(account.getId(), works.get(0).getAccountId());
        assertEquals("it_ks_photo_001", works.get(0).getPlatformWorkId());
        assertEquals(120, works.get(0).getCommentCount());
        assertEquals("https://www.kuaishou.com/short-video/it_ks_photo_001", works.get(0).getWorkUrl());
        assertEquals(66, works.get(1).getCommentCount());
        assertEquals(1, works.get(0).getIsExternal());
    }

    @Test
    @DisplayName("GATE-EXT-P0: CollectExecutionService EXTERNAL 路由")
    void executionRoutesExternalKuaishou() throws Exception {
        server.enqueue(envelope("""
                {
                  "user_id":"%s",
                  "user_name":"路由测试",
                  "has_more":false,
                  "videos":[
                    {
                      "photo_id":"it_ks_route_001",
                      "caption":"路由作品",
                      "share_url":"https://www.kuaishou.com/short-video/it_ks_route_001",
                      "view_count":1000,
                      "like_count":50,
                      "comment_count":8,
                      "create_time":1700300000
                    }
                  ]
                }
                """.formatted(KUAISHOU_USER_ID)));

        CollectExecutionResult result = collectExecutionService.execute(buildExternalTask());
        assertTrue(result.isSuccess());
        assertEquals(1, result.getRecordCount());

        ExternalWorkDO work = externalWorkMapper.selectOne(
                new LambdaQueryWrapper<ExternalWorkDO>()
                        .eq(ExternalWorkDO::getPlatformWorkId, "it_ks_route_001"));
        assertNotNull(work);
        assertEquals(8, work.getCommentCount());
    }

    private CollectTaskDO buildExternalTask() {
        CollectTaskDO task = new CollectTaskDO();
        task.setTenantId(TENANT_1);
        task.setTaskName("IT-快手竞品-user-videos");
        task.setPlatformType("KUAISHOU");
        task.setMethod("EXTERNAL");
        task.setSource("UNIFY_COLLECTOR_EXTERNAL");
        task.setCollectConfigId(collectConfigId);
        task.setCredentialProfile("default");
        task.setDataType("EXT_KUAISHOU_USER_VIDEOS");
        task.setFrequency("DAILY");
        task.setStatus("ENABLED");
        return task;
    }

    private Long seedCollectConfig() {
        jdbcTemplate.update("""
                INSERT INTO oa_collect_config (
                    tenant_id, scope, config_name, sub_type, platform_type,
                    account_identifier, collect_method, status, creator, updater, deleted
                ) VALUES (?, 'EXTERNAL', 'IT-快手竞品', 'account', 'KUAISHOU', ?, 'EXTERNAL', 'ENABLED', 'it', 'it', 0)
                """, TENANT_1, KUAISHOU_USER_ID);
        return jdbcTemplate.queryForObject("SELECT MAX(id) FROM oa_collect_config", Long.class);
    }

    private void cleanupArtifacts() {
        if (collectConfigId != null) {
            jdbcTemplate.update("DELETE FROM oa_external_work WHERE collect_config_id = ?", collectConfigId);
            jdbcTemplate.update("DELETE FROM oa_external_account WHERE collect_config_id = ?", collectConfigId);
            jdbcTemplate.update("DELETE FROM oa_collect_task WHERE collect_config_id = ?", collectConfigId);
            jdbcTemplate.update("DELETE FROM oa_collect_config WHERE id = ?", collectConfigId);
        }
        jdbcTemplate.update("""
                DELETE FROM oa_external_work
                WHERE tenant_id = ? AND platform_work_id IN ('it_ks_photo_001','it_ks_photo_002','it_ks_route_001')
                """, TENANT_1);
    }

    private static MockResponse envelope(String dataJson) {
        return new MockResponse()
                .setBody("""
                        {"code":0,"message":"success","data":%s}
                        """.formatted(dataJson))
                .addHeader("Content-Type", "application/json");
    }
}
