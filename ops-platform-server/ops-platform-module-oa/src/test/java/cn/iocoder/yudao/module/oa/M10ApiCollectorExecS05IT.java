package cn.iocoder.yudao.module.oa;

import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.oa.dal.dataobject.account.AccountDO;
import cn.iocoder.yudao.module.oa.dal.dataobject.collect.CollectTaskDO;
import cn.iocoder.yudao.module.oa.dal.dataobject.collect.WechatMpFollowerDO;
import cn.iocoder.yudao.module.oa.dal.mysql.account.AccountMapper;
import cn.iocoder.yudao.module.oa.dal.mysql.collect.WechatMpFollowerMapper;
import cn.iocoder.yudao.module.oa.service.collect.CollectExecutionService;
import cn.iocoder.yudao.module.oa.service.collect.CollectExecutionResult;
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
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * M10-API-S-05: WECHAT_MP_API + INTERNAL 经 UnifiedCollectorAdapter 执行 follower-list。
 */
@AutoConfigureMockMvc
class M10ApiCollectorExecS05IT extends OaITBase {

    private static final long TENANT_1 = 1L;
    private static final long SEED_ACCOUNT_ID = 9001L;
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
    private WechatMpFollowerMapper wechatMpFollowerMapper;

    @Autowired
    private AesUtil aesUtil;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        TenantContextHolder.setTenantId(TENANT_1);
        TenantContextHolder.setUsername("it-m10-api-s05");
        cleanupCollectArtifacts();
        jdbcTemplate.update("DELETE FROM oa_wechat_mp_follower WHERE account_id = ?", SEED_ACCOUNT_ID);
        jdbcTemplate.update("DELETE FROM oa_collector_account_bind WHERE oa_account_id = ?", SEED_ACCOUNT_ID);
        seedWechatOfficialCredentials();
    }

    @AfterEach
    void tearDown() {
        cleanupCollectArtifacts();
        TenantContextHolder.clear();
    }

    private void cleanupCollectArtifacts() {
        jdbcTemplate.update(
                "DELETE FROM oa_collect_log WHERE task_id IN (SELECT id FROM oa_collect_task WHERE account_id = ?)",
                SEED_ACCOUNT_ID);
        jdbcTemplate.update("DELETE FROM oa_collect_task WHERE account_id = ?", SEED_ACCOUNT_ID);
    }

    private void seedWechatOfficialCredentials() {
        AccountDO account = accountMapper.selectById(SEED_ACCOUNT_ID);
        account.setCookieEncrypted(aesUtil.encrypt("bizuin=123; data_bizuin=123"));
        account.setMpTokenEncrypted(aesUtil.encrypt("1234567890"));
        accountMapper.updateById(account);
    }

    @Test
    @DisplayName("M10-API-S-05: adapter 同步 stub follower-list 落库")
    void adapterSyncFollowersStub() {
        unifiedCollectorAdapter.bindAccount(SEED_ACCOUNT_ID);

        int count = unifiedCollectorAdapter.executeWechatMpFollowerCollect(SEED_ACCOUNT_ID);
        assertEquals(2, count);

        Long stored = wechatMpFollowerMapper.selectCount(new LambdaQueryWrapper<WechatMpFollowerDO>()
                .eq(WechatMpFollowerDO::getTenantId, TENANT_1)
                .eq(WechatMpFollowerDO::getAccountId, SEED_ACCOUNT_ID));
        assertEquals(2L, stored);

        WechatMpFollowerDO first = wechatMpFollowerMapper.selectOne(new LambdaQueryWrapper<WechatMpFollowerDO>()
                .eq(WechatMpFollowerDO::getOpenid, "oStubFollower001"));
        assertEquals("Stub粉丝A", first.getNickname());
        assertEquals("uStub001", first.getUnionid());
    }

    @Test
    @DisplayName("M10-API-S-05: CollectExecutionService 路由 WECHAT_MP_API")
    void executionServiceRoutesWechatMpApi() {
        unifiedCollectorAdapter.bindAccount(SEED_ACCOUNT_ID);

        CollectTaskDO task = new CollectTaskDO();
        task.setTenantId(TENANT_1);
        task.setPlatformType("WECHAT_OFFICIAL");
        task.setAccountId(SEED_ACCOUNT_ID);
        task.setMethod("INTERNAL");
        task.setSource("WECHAT_MP_API");

        CollectExecutionResult result = collectExecutionService.execute(task);
        assertTrue(result.isSuccess());
        assertTrue(result.isMultiType());
        assertEquals(9, result.getRecordCount());
    }

    @Test
    @DisplayName("M10-API-S-05: 任务 run 写入 SUCCESS 日志与粉丝快照")
    void taskRunPersistsFollowers() throws Exception {
        unifiedCollectorAdapter.bindAccount(SEED_ACCOUNT_ID);
        Long taskId = createTask("公众号粉丝采集S05");

        mockMvc.perform(post("/admin-api/oa/collect/task/" + taskId + "/run")
                        .header("Authorization", AUTH)
                        .header("X-Tenant-Id", TENANT))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));

        mockMvc.perform(get("/admin-api/oa/collect/task/" + taskId)
                        .header("Authorization", AUTH)
                        .header("X-Tenant-Id", TENANT))
                .andExpect(jsonPath("$.data.status").value("PENDING"))
                .andExpect(jsonPath("$.data.runCount").value(1));

        mockMvc.perform(get("/admin-api/oa/collect/log/page")
                        .header("Authorization", AUTH)
                        .header("X-Tenant-Id", TENANT)
                        .param("taskId", String.valueOf(taskId)))
                .andExpect(jsonPath("$.data.list[0].status").value("SUCCESS"))
                .andExpect(jsonPath("$.data.list[0].recordCount").value(9));

        Long stored = wechatMpFollowerMapper.selectCount(new LambdaQueryWrapper<WechatMpFollowerDO>()
                .eq(WechatMpFollowerDO::getAccountId, SEED_ACCOUNT_ID));
        assertEquals(2L, stored);
    }

    @Test
    @DisplayName("M10-API-S-05: 未绑定 Collector 执行失败")
    void executionFailsWithoutBind() {
        CollectTaskDO task = new CollectTaskDO();
        task.setTenantId(TENANT_1);
        task.setPlatformType("WECHAT_OFFICIAL");
        task.setAccountId(SEED_ACCOUNT_ID);
        task.setMethod("INTERNAL");
        task.setSource("WECHAT_MP_API");

        CollectExecutionResult result = collectExecutionService.execute(task);
        assertTrue(!result.isSuccess());
        assertTrue(result.getErrorMessage().contains("绑定"));
    }

    private Long createTask(String name) throws Exception {
        MvcResult createResult = mockMvc.perform(post("/admin-api/oa/collect/task/create")
                        .header("Authorization", AUTH)
                        .header("X-Tenant-Id", TENANT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(String.format("""
                                {
                                  "name": "%s",
                                  "platformType": "WECHAT_OFFICIAL",
                                  "accountId": %d,
                                  "method": "INTERNAL",
                                  "source": "WECHAT_MP_API",
                                  "frequency": "DAILY",
                                  "cron": "0 0 2 * * ?",
                                  "status": "PENDING"
                                }
                                """, name, SEED_ACCOUNT_ID)))
                .andExpect(jsonPath("$.code").value(0))
                .andReturn();
        return JsonPath.parse(createResult.getResponse().getContentAsString()).read("$.data", Long.class);
    }
}
