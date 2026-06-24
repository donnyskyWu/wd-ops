package cn.iocoder.yudao.module.oa;

import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.oa.dal.dataobject.account.AccountDO;
import cn.iocoder.yudao.module.oa.dal.dataobject.analytics.AccountStatusLogDO;
import cn.iocoder.yudao.module.oa.dal.dataobject.collect.CollectTaskDO;
import cn.iocoder.yudao.module.oa.dal.dataobject.collect.DouyinFollowerDO;
import cn.iocoder.yudao.module.oa.dal.dataobject.collect.DouyinVideoDO;
import cn.iocoder.yudao.module.oa.dal.mysql.account.AccountMapper;
import cn.iocoder.yudao.module.oa.dal.mysql.analytics.AccountStatusLogMapper;
import cn.iocoder.yudao.module.oa.dal.mysql.collect.DouyinFollowerMapper;
import cn.iocoder.yudao.module.oa.dal.mysql.collect.DouyinVideoMapper;
import cn.iocoder.yudao.module.oa.service.collect.CollectExecutionResult;
import cn.iocoder.yudao.module.oa.service.collect.CollectExecutionService;
import cn.iocoder.yudao.module.oa.service.collect.CollectPlatformDefaults;
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

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * M10: dataType 为空/ALL 时一次任务顺序执行平台全部采集类型。
 */
@AutoConfigureMockMvc
class M10ApiCollectorExecAllTypesIT extends OaITBase {

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
    private AccountStatusLogMapper accountStatusLogMapper;

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
        TenantContextHolder.setUsername("it-m10-api-all-types");
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
        jdbcTemplate.update("DELETE FROM oa_account_status_log WHERE account_id = ? AND stat_date = CURRENT_DATE",
                DOUYIN_ACCOUNT_ID);
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
    @DisplayName("M10: dataType 为空时 CollectExecutionService 顺序执行抖音全部类型")
    void douyinCollectAllTypesInOneRun() {
        unifiedCollectorAdapter.bindAccount(DOUYIN_ACCOUNT_ID);

        CollectTaskDO task = douyinTask(null);
        CollectExecutionResult result = collectExecutionService.execute(task);

        assertTrue(result.isSuccess());
        assertTrue(result.isMultiType());
        assertEquals(4, result.getTypeOutcomes().size());
        assertTrue(result.getRecordCount() > 0);

        AccountStatusLogDO statusLog = accountStatusLogMapper.selectOne(new LambdaQueryWrapper<AccountStatusLogDO>()
                .eq(AccountStatusLogDO::getAccountId, DOUYIN_ACCOUNT_ID)
                .eq(AccountStatusLogDO::getStatDate, LocalDate.now()));
        assertNotNull(statusLog);

        assertEquals(2L, douyinFollowerMapper.selectCount(new LambdaQueryWrapper<DouyinFollowerDO>()
                .eq(DouyinFollowerDO::getAccountId, DOUYIN_ACCOUNT_ID)));

        DouyinVideoDO video = douyinVideoMapper.selectOne(new LambdaQueryWrapper<DouyinVideoDO>()
                .eq(DouyinVideoDO::getVideoId, "stub_douyin_video_001"));
        assertNotNull(video);
        assertEquals(56_000, video.getPlayCount());
    }

    @Test
    @DisplayName("M10: 创建任务省略 method/source 时后端填充平台默认并全量执行")
    void createTaskWithPlatformDefaultsAndRunAll() throws Exception {
        unifiedCollectorAdapter.bindAccount(DOUYIN_ACCOUNT_ID);

        MvcResult createResult = mockMvc.perform(post("/admin-api/oa/collect/task/create")
                        .header("Authorization", AUTH)
                        .header("X-Tenant-Id", TENANT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "抖音全量采集",
                                  "platformType": "DOUYIN",
                                  "accountId": 9006,
                                  "frequency": "DAILY",
                                  "cron": "0 0 2 * * ?",
                                  "status": "PENDING"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andReturn();
        Long taskId = JsonPath.parse(createResult.getResponse().getContentAsString()).read("$.data", Long.class);

        mockMvc.perform(get("/admin-api/oa/collect/task/" + taskId)
                        .header("Authorization", AUTH)
                        .header("X-Tenant-Id", TENANT))
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.method").value("INTERNAL"))
                .andExpect(jsonPath("$.data.source").value("DOUYIN_OPEN_API"));

        mockMvc.perform(post("/admin-api/oa/collect/task/" + taskId + "/run")
                        .header("Authorization", AUTH)
                        .header("X-Tenant-Id", TENANT))
                .andExpect(jsonPath("$.code").value(0));

        mockMvc.perform(get("/admin-api/oa/collect/log/page")
                        .header("Authorization", AUTH)
                        .header("X-Tenant-Id", TENANT)
                        .param("taskId", String.valueOf(taskId)))
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.list[0].status").value("SUCCESS"));

        Long logId = JsonPath.parse(mockMvc.perform(get("/admin-api/oa/collect/log/page")
                        .header("Authorization", AUTH)
                        .header("X-Tenant-Id", TENANT)
                        .param("taskId", String.valueOf(taskId)))
                .andReturn().getResponse().getContentAsString()).read("$.data.list[0].id", Long.class);

        mockMvc.perform(get("/admin-api/oa/collect/log/" + logId)
                        .header("Authorization", AUTH)
                        .header("X-Tenant-Id", TENANT))
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.result.dataType").value(CollectPlatformDefaults.DATA_TYPE_ALL))
                .andExpect(jsonPath("$.data.result.typeResults.length()").value(4));
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
}
