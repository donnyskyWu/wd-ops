package cn.iocoder.yudao.module.oa;

import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.oa.dal.dataobject.account.AccountDO;
import cn.iocoder.yudao.module.oa.dal.dataobject.collect.CollectTaskDO;
import cn.iocoder.yudao.module.oa.dal.dataobject.collect.DouyinFollowerDO;
import cn.iocoder.yudao.module.oa.dal.dataobject.collect.DouyinVideoDO;
import cn.iocoder.yudao.module.oa.dal.mysql.account.AccountMapper;
import cn.iocoder.yudao.module.oa.dal.mysql.collect.DouyinFollowerMapper;
import cn.iocoder.yudao.module.oa.dal.mysql.collect.DouyinVideoMapper;
import cn.iocoder.yudao.module.oa.service.collect.CollectExecutionResult;
import cn.iocoder.yudao.module.oa.service.collect.CollectExecutionService;
import cn.iocoder.yudao.module.oa.service.collect.unified.UnifiedCollectorAdapter;
import cn.iocoder.yudao.module.oa.util.AesUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * M10 P2: DOUYIN_OPEN_API + dataType 路由 follower-list / video-list / video-stats。
 */
@AutoConfigureMockMvc
class M10ApiCollectorExecDouyinListIT extends OaITBase {

    private static final long TENANT_1 = 1L;
    private static final long DOUYIN_ACCOUNT_ID = 9006L;
    private static final String AUTH = "Bearer dev-token-oa-admin";
    private static final String TENANT = "1";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UnifiedCollectorAdapter unifiedCollectorAdapter;

    @Autowired
    private CollectExecutionService collectExecutionService;

    @Autowired
    private AccountMapper accountMapper;

    @Autowired
    private DouyinFollowerMapper douyinFollowerMapper;

    @Autowired
    private DouyinVideoMapper douyinVideoMapper;

    @Autowired
    private AesUtil aesUtil;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        TenantContextHolder.setTenantId(TENANT_1);
        TenantContextHolder.setUsername("it-m10-api-douyin-list");
        cleanupArtifacts();
        seedDouyinCredentials();
    }

    @AfterEach
    void tearDown() {
        cleanupArtifacts();
        TenantContextHolder.clear();
    }

    private void cleanupArtifacts() {
        jdbcTemplate.update(
                "DELETE FROM oa_collect_log WHERE task_id IN (SELECT id FROM oa_collect_task WHERE account_id = ?)",
                DOUYIN_ACCOUNT_ID);
        jdbcTemplate.update("DELETE FROM oa_collect_task WHERE account_id = ?", DOUYIN_ACCOUNT_ID);
        jdbcTemplate.update("DELETE FROM oa_douyin_follower WHERE account_id = ?", DOUYIN_ACCOUNT_ID);
        jdbcTemplate.update("DELETE FROM oa_douyin_video WHERE account_id = ?", DOUYIN_ACCOUNT_ID);
        jdbcTemplate.update("DELETE FROM oa_collector_account_bind WHERE oa_account_id = ?", DOUYIN_ACCOUNT_ID);
    }

    private void seedDouyinCredentials() {
        AccountDO account = accountMapper.selectById(DOUYIN_ACCOUNT_ID);
        account.setCookieEncrypted(aesUtil.encrypt("sessionid=stub_douyin_session; sid_guard=stub"));
        accountMapper.updateById(account);
    }

    @Test
    @DisplayName("M10 P2: adapter 同步 stub follower-list 落库")
    void adapterSyncDouyinFollowerListStub() {
        unifiedCollectorAdapter.bindAccount(DOUYIN_ACCOUNT_ID);

        int count = unifiedCollectorAdapter.executeDouyinFollowerListCollect(DOUYIN_ACCOUNT_ID);
        assertEquals(2, count);

        Long stored = douyinFollowerMapper.selectCount(new LambdaQueryWrapper<DouyinFollowerDO>()
                .eq(DouyinFollowerDO::getTenantId, TENANT_1)
                .eq(DouyinFollowerDO::getAccountId, DOUYIN_ACCOUNT_ID));
        assertEquals(2L, stored);
    }

    @Test
    @DisplayName("M10 P2: adapter 同步 stub video-list 落库")
    void adapterSyncDouyinVideoListStub() {
        unifiedCollectorAdapter.bindAccount(DOUYIN_ACCOUNT_ID);

        int count = unifiedCollectorAdapter.executeDouyinVideoListCollect(DOUYIN_ACCOUNT_ID);
        assertEquals(2, count);

        DouyinVideoDO first = douyinVideoMapper.selectOne(new LambdaQueryWrapper<DouyinVideoDO>()
                .eq(DouyinVideoDO::getVideoId, "stub_douyin_video_001"));
        assertNotNull(first);
        assertEquals("Stub抖音作品A", first.getTitle());
        assertEquals(12_000, first.getPlayCount());
        assertNotNull(first.getStatsSyncedAt());
    }

    @Test
    @DisplayName("M10 P2: video-list 已含统计时 video-stats 跳过逐条明细拉取")
    void videoStatsSkipsWhenListAlreadyHasStats() {
        unifiedCollectorAdapter.bindAccount(DOUYIN_ACCOUNT_ID);
        unifiedCollectorAdapter.executeDouyinVideoListCollect(DOUYIN_ACCOUNT_ID);

        int count = unifiedCollectorAdapter.executeDouyinVideoStatsCollect(DOUYIN_ACCOUNT_ID);
        assertEquals(2, count);

        DouyinVideoDO first = douyinVideoMapper.selectOne(new LambdaQueryWrapper<DouyinVideoDO>()
                .eq(DouyinVideoDO::getVideoId, "stub_douyin_video_001"));
        assertEquals(12_000, first.getPlayCount());
    }

    @Test
    @DisplayName("M10 P2: adapter 同步 stub video-stats 更新作品明细")
    void adapterSyncDouyinVideoStatsStub() {
        unifiedCollectorAdapter.bindAccount(DOUYIN_ACCOUNT_ID);
        unifiedCollectorAdapter.executeDouyinVideoListCollect(DOUYIN_ACCOUNT_ID);

        jdbcTemplate.update(
                "UPDATE oa_douyin_video SET play_count = NULL, stats_synced_at = NULL WHERE account_id = ?",
                DOUYIN_ACCOUNT_ID);

        int count = unifiedCollectorAdapter.executeDouyinVideoStatsCollect(DOUYIN_ACCOUNT_ID);
        assertEquals(2, count);

        DouyinVideoDO first = douyinVideoMapper.selectOne(new LambdaQueryWrapper<DouyinVideoDO>()
                .eq(DouyinVideoDO::getVideoId, "stub_douyin_video_001"));
        assertEquals(56_000, first.getPlayCount());
        assertEquals(3_200, first.getLikeCount());
        assertNotNull(first.getStatsSyncedAt());
    }

    @Test
    @DisplayName("M10 P2: CollectExecutionService 路由 DOUYIN_FOLLOWER_LIST")
    void executionServiceRoutesDouyinFollowerList() {
        unifiedCollectorAdapter.bindAccount(DOUYIN_ACCOUNT_ID);

        CollectTaskDO task = douyinTask("DOUYIN_FOLLOWER_LIST");
        CollectExecutionResult result = collectExecutionService.execute(task);
        assertTrue(result.isSuccess());
        assertEquals(2, result.getRecordCount());
    }

    @Test
    @DisplayName("M10 P2: CollectExecutionService 路由 DOUYIN_VIDEO_LIST")
    void executionServiceRoutesDouyinVideoList() {
        unifiedCollectorAdapter.bindAccount(DOUYIN_ACCOUNT_ID);

        CollectExecutionResult result = collectExecutionService.execute(douyinTask("DOUYIN_VIDEO_LIST"));
        assertTrue(result.isSuccess());
        assertEquals(2, result.getRecordCount());
    }

    @Test
    @DisplayName("M10 P2: CollectExecutionService 路由 DOUYIN_VIDEO_STATS")
    void executionServiceRoutesDouyinVideoStats() {
        unifiedCollectorAdapter.bindAccount(DOUYIN_ACCOUNT_ID);
        unifiedCollectorAdapter.executeDouyinVideoListCollect(DOUYIN_ACCOUNT_ID);

        CollectExecutionResult result = collectExecutionService.execute(douyinTask("DOUYIN_VIDEO_STATS"));
        assertTrue(result.isSuccess());
        assertEquals(2, result.getRecordCount());
    }

    @Test
    @DisplayName("M10 P2: 任务 run 写入 DOUYIN_VIDEO_LIST 日志")
    void taskRunPersistsDouyinVideos() throws Exception {
        unifiedCollectorAdapter.bindAccount(DOUYIN_ACCOUNT_ID);
        Long taskId = createTask("抖音作品列表P2", "DOUYIN_VIDEO_LIST");

        mockMvc.perform(post("/admin-api/oa/collect/task/" + taskId + "/run")
                        .header("Authorization", AUTH)
                        .header("X-Tenant-Id", TENANT))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));

        Long stored = douyinVideoMapper.selectCount(new LambdaQueryWrapper<DouyinVideoDO>()
                .eq(DouyinVideoDO::getAccountId, DOUYIN_ACCOUNT_ID));
        assertEquals(2L, stored);
    }

    @Test
    @DisplayName("M10 P2: 未绑定 Collector 执行 DOUYIN_FOLLOWER_LIST 失败")
    void executionFailsWithoutBind() {
        CollectExecutionResult result = collectExecutionService.execute(douyinTask("DOUYIN_FOLLOWER_LIST"));
        assertFalse(result.isSuccess());
        assertTrue(result.getErrorMessage().contains("绑定"));
    }

    private CollectTaskDO douyinTask(String dataType) {
        CollectTaskDO task = new CollectTaskDO();
        task.setTenantId(TENANT_1);
        task.setPlatformType("DOUYIN");
        task.setAccountId(DOUYIN_ACCOUNT_ID);
        task.setMethod("INTERNAL");
        task.setSource("DOUYIN_OPEN_API");
        task.setDataType(dataType);
        return task;
    }

    private Long createTask(String name, String dataType) throws Exception {
        MvcResult createResult = mockMvc.perform(post("/admin-api/oa/collect/task/create")
                        .header("Authorization", AUTH)
                        .header("X-Tenant-Id", TENANT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(String.format("""
                                {
                                  "name": "%s",
                                  "platformType": "DOUYIN",
                                  "accountId": %d,
                                  "method": "INTERNAL",
                                  "source": "DOUYIN_OPEN_API",
                                  "dataType": "%s",
                                  "frequency": "DAILY",
                                  "cron": "0 0 4 * * ?",
                                  "status": "PENDING"
                                }
                                """, name, DOUYIN_ACCOUNT_ID, dataType)))
                .andExpect(jsonPath("$.code").value(0))
                .andReturn();
        return JsonPath.parse(createResult.getResponse().getContentAsString()).read("$.data", Long.class);
    }
}
