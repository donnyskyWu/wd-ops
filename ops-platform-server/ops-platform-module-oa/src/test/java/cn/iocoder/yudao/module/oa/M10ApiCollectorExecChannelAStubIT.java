package cn.iocoder.yudao.module.oa;

import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.oa.dal.dataobject.account.AccountDO;
import cn.iocoder.yudao.module.oa.dal.dataobject.analytics.AccountStatusLogDO;
import cn.iocoder.yudao.module.oa.dal.dataobject.collect.CollectTaskDO;
import cn.iocoder.yudao.module.oa.dal.mysql.account.AccountMapper;
import cn.iocoder.yudao.module.oa.dal.mysql.analytics.AccountStatusLogMapper;
import cn.iocoder.yudao.module.oa.service.collect.CollectExecutionResult;
import cn.iocoder.yudao.module.oa.service.collect.CollectExecutionService;
import cn.iocoder.yudao.module.oa.service.collect.unified.UnifiedCollectorAdapter;
import cn.iocoder.yudao.module.oa.util.AesUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * M10-API-S-06: WECHAT_VIDEO / XIAOHONGSHU / BILIBILI Channel-A stub 执行路由。
 */
class M10ApiCollectorExecChannelAStubIT extends OaITBase {

    private static final long TENANT_1 = 1L;
    private static final long WECHAT_VIDEO_ACCOUNT_ID = 9004L;
    private static final long XIAOHONGSHU_ACCOUNT_ID = 9009L;
    private static final long BILIBILI_ACCOUNT_ID = 9011L;

    @Autowired
    private UnifiedCollectorAdapter unifiedCollectorAdapter;

    @Autowired
    private CollectExecutionService collectExecutionService;

    @Autowired
    private AccountMapper accountMapper;

    @Autowired
    private AccountStatusLogMapper accountStatusLogMapper;

    @Autowired
    private AesUtil aesUtil;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        TenantContextHolder.setTenantId(TENANT_1);
        TenantContextHolder.setUsername("it-m10-api-channel-a");
        ensureBilibiliSeedAccount();
        cleanupArtifacts(WECHAT_VIDEO_ACCOUNT_ID, XIAOHONGSHU_ACCOUNT_ID, BILIBILI_ACCOUNT_ID);
        seedCredentials(WECHAT_VIDEO_ACCOUNT_ID, "sessionid=stub_wv_exec");
        seedCredentials(XIAOHONGSHU_ACCOUNT_ID, "web_session=stub_xhs");
        seedCredentials(BILIBILI_ACCOUNT_ID, "SESSDATA=stub_bili");
    }

    @AfterEach
    void tearDown() {
        cleanupArtifacts(WECHAT_VIDEO_ACCOUNT_ID, XIAOHONGSHU_ACCOUNT_ID, BILIBILI_ACCOUNT_ID);
        TenantContextHolder.clear();
    }

    private void ensureBilibiliSeedAccount() {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM oa_account WHERE id = ? AND deleted = 0", Integer.class, BILIBILI_ACCOUNT_ID);
        if (count != null && count > 0) {
            return;
        }
        jdbcTemplate.update("""
                INSERT INTO oa_account (
                    id, tenant_id, platform_type, account_type, account_name, external_account_id,
                    company_id, realname_id, phone_id, sim_card_id, phone_number_hash,
                    status, creator, updater, deleted
                ) VALUES (9011, 1, 'BILIBILI', 'PERSONAL_ACCOUNT', 'SEED-B站1', 'seed_bili1',
                    9002, 9005, 9005, NULL,
                    '8577eb3aa6cc35852a17fe93c50e0f3dd6ede7b133cbdec150a67bec0711c688',
                    'NORMAL', 'it-m10', 'it-m10', 0)
                """);
    }

    private void cleanupArtifacts(long... accountIds) {
        for (long accountId : accountIds) {
            jdbcTemplate.update(
                    "DELETE FROM oa_collect_log WHERE task_id IN (SELECT id FROM oa_collect_task WHERE account_id = ?)",
                    accountId);
            jdbcTemplate.update("DELETE FROM oa_collect_task WHERE account_id = ?", accountId);
            jdbcTemplate.update("DELETE FROM oa_account_status_log WHERE account_id = ? AND stat_date = CURRENT_DATE",
                    accountId);
            jdbcTemplate.update("DELETE FROM oa_collector_account_bind WHERE oa_account_id = ?", accountId);
        }
    }

    private void seedCredentials(long accountId, String cookiePlain) {
        AccountDO account = accountMapper.selectById(accountId);
        account.setCookieEncrypted(aesUtil.encrypt(cookiePlain));
        accountMapper.updateById(account);
    }

    @Test
    @DisplayName("M10-API-S-06: WECHAT_VIDEO stub follower-stats 落库")
    void wechatVideoFollowerStatsStub() {
        unifiedCollectorAdapter.bindAccount(WECHAT_VIDEO_ACCOUNT_ID);

        int count = unifiedCollectorAdapter.executeWechatVideoFollowerStatsCollect(WECHAT_VIDEO_ACCOUNT_ID);
        assertEquals(1, count);

        AccountStatusLogDO log = loadTodayLog(WECHAT_VIDEO_ACCOUNT_ID);
        assertEquals(45_600L, log.getFollowerCount());
    }

    @Test
    @DisplayName("M10-API-S-06: CollectExecutionService 路由 WECHAT_CHANNELS_API")
    void executionRoutesWechatChannelsApi() {
        unifiedCollectorAdapter.bindAccount(WECHAT_VIDEO_ACCOUNT_ID);

        CollectExecutionResult result = collectExecutionService.execute(buildTask(
                "WECHAT_VIDEO", WECHAT_VIDEO_ACCOUNT_ID, "WECHAT_CHANNELS_API"));
        assertTrue(result.isSuccess());
        assertEquals(1, result.getRecordCount());
    }

    @Test
    @DisplayName("M10-API-S-06: XIAOHONGSHU stub follower-stats 落库")
    void xiaohongshuFollowerStatsStub() {
        unifiedCollectorAdapter.bindAccount(XIAOHONGSHU_ACCOUNT_ID);

        int count = unifiedCollectorAdapter.executeXiaohongshuFollowerStatsCollect(XIAOHONGSHU_ACCOUNT_ID);
        assertEquals(1, count);

        AccountStatusLogDO log = loadTodayLog(XIAOHONGSHU_ACCOUNT_ID);
        assertEquals(12_500L, log.getFollowerCount());
    }

    @Test
    @DisplayName("M10-API-S-06: CollectExecutionService 路由 XIAOHONGSHU_OPEN_API")
    void executionRoutesXiaohongshuOpenApi() {
        unifiedCollectorAdapter.bindAccount(XIAOHONGSHU_ACCOUNT_ID);

        CollectExecutionResult result = collectExecutionService.execute(buildTask(
                "XIAOHONGSHU", XIAOHONGSHU_ACCOUNT_ID, "XIAOHONGSHU_OPEN_API"));
        assertTrue(result.isSuccess());
        assertEquals(1, result.getRecordCount());
    }

    @Test
    @DisplayName("M10-API-S-06: BILIBILI stub follower-stats 落库")
    void bilibiliFollowerStatsStub() {
        unifiedCollectorAdapter.bindAccount(BILIBILI_ACCOUNT_ID);

        int count = unifiedCollectorAdapter.executeBilibiliFollowerStatsCollect(BILIBILI_ACCOUNT_ID);
        assertEquals(1, count);

        AccountStatusLogDO log = loadTodayLog(BILIBILI_ACCOUNT_ID);
        assertEquals(99_000L, log.getFollowerCount());
    }

    @Test
    @DisplayName("M10-API-S-06: CollectExecutionService 路由 BILIBILI_OPEN_API")
    void executionRoutesBilibiliOpenApi() {
        unifiedCollectorAdapter.bindAccount(BILIBILI_ACCOUNT_ID);

        CollectExecutionResult result = collectExecutionService.execute(buildTask(
                "BILIBILI", BILIBILI_ACCOUNT_ID, "BILIBILI_OPEN_API"));
        assertTrue(result.isSuccess());
        assertEquals(1, result.getRecordCount());
    }

    private CollectTaskDO buildTask(String platformType, long accountId, String source) {
        CollectTaskDO task = new CollectTaskDO();
        task.setTenantId(TENANT_1);
        task.setPlatformType(platformType);
        task.setAccountId(accountId);
        task.setMethod("INTERNAL");
        task.setSource(source);
        return task;
    }

    private AccountStatusLogDO loadTodayLog(long accountId) {
        AccountStatusLogDO log = accountStatusLogMapper.selectOne(new LambdaQueryWrapper<AccountStatusLogDO>()
                .eq(AccountStatusLogDO::getTenantId, TENANT_1)
                .eq(AccountStatusLogDO::getAccountId, accountId)
                .eq(AccountStatusLogDO::getStatDate, LocalDate.now()));
        assertNotNull(log);
        assertEquals("NORMAL", log.getStatus());
        return log;
    }
}
