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
 * M10-API-S-06: DOUYIN/KUAISHOU + INTERNAL 经 UnifiedCollectorAdapter 执行 follower-stats。
 */
class M10ApiCollectorExecDouyinIT extends OaITBase {

    private static final long TENANT_1 = 1L;
    private static final long DOUYIN_ACCOUNT_ID = 9006L;
    private static final long KUAISHOU_ACCOUNT_ID = 9008L;

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
        TenantContextHolder.setUsername("it-m10-api-s06");
        cleanupArtifacts(DOUYIN_ACCOUNT_ID);
        cleanupArtifacts(KUAISHOU_ACCOUNT_ID);
        seedDouyinCredentials();
        seedKuaishouCredentials();
    }

    @AfterEach
    void tearDown() {
        cleanupArtifacts(DOUYIN_ACCOUNT_ID);
        cleanupArtifacts(KUAISHOU_ACCOUNT_ID);
        TenantContextHolder.clear();
    }

    private void cleanupArtifacts(long accountId) {
        jdbcTemplate.update(
                "DELETE FROM oa_collect_log WHERE task_id IN (SELECT id FROM oa_collect_task WHERE account_id = ?)",
                accountId);
        jdbcTemplate.update("DELETE FROM oa_collect_task WHERE account_id = ?", accountId);
        jdbcTemplate.update("DELETE FROM oa_account_status_log WHERE account_id = ? AND stat_date = CURRENT_DATE",
                accountId);
        jdbcTemplate.update("DELETE FROM oa_collector_account_bind WHERE oa_account_id = ?", accountId);
    }

    private void seedDouyinCredentials() {
        AccountDO account = accountMapper.selectById(DOUYIN_ACCOUNT_ID);
        account.setCookieEncrypted(aesUtil.encrypt("sessionid=stub_douyin_session; sid_guard=stub"));
        accountMapper.updateById(account);
    }

    private void seedKuaishouCredentials() {
        AccountDO account = accountMapper.selectById(KUAISHOU_ACCOUNT_ID);
        account.setCookieEncrypted(aesUtil.encrypt("kuaishou.web.cp.api_ph=stub"));
        account.setAuthTokenEncrypted(aesUtil.encrypt("stub_ks_auth_token"));
        account.setFieldMapping("{\"play_count\":\"playCount\",\"like_count\":\"likeCount\"}");
        accountMapper.updateById(account);
    }

    @Test
    @DisplayName("M10-API-S-06: DOUYIN adapter 同步 stub follower-stats 落库")
    void adapterSyncDouyinFollowerStatsStub() {
        unifiedCollectorAdapter.bindAccount(DOUYIN_ACCOUNT_ID);

        int count = unifiedCollectorAdapter.executeDouyinFollowerStatsCollect(DOUYIN_ACCOUNT_ID);
        assertEquals(1, count);

        AccountStatusLogDO log = accountStatusLogMapper.selectOne(new LambdaQueryWrapper<AccountStatusLogDO>()
                .eq(AccountStatusLogDO::getTenantId, TENANT_1)
                .eq(AccountStatusLogDO::getAccountId, DOUYIN_ACCOUNT_ID)
                .eq(AccountStatusLogDO::getStatDate, LocalDate.now()));
        assertNotNull(log);
        assertEquals(125_000L, log.getFollowerCount());
        assertEquals("NORMAL", log.getStatus());
    }

    @Test
    @DisplayName("M10-API-S-06: CollectExecutionService 路由 DOUYIN_OPEN_API")
    void executionServiceRoutesDouyinOpenApi() {
        unifiedCollectorAdapter.bindAccount(DOUYIN_ACCOUNT_ID);

        CollectTaskDO task = new CollectTaskDO();
        task.setTenantId(TENANT_1);
        task.setPlatformType("DOUYIN");
        task.setAccountId(DOUYIN_ACCOUNT_ID);
        task.setMethod("INTERNAL");
        task.setSource("DOUYIN_OPEN_API");

        CollectExecutionResult result = collectExecutionService.execute(task);
        assertTrue(result.isSuccess());
        assertTrue(result.isMultiType());
        assertEquals(7, result.getRecordCount());
    }

    @Test
    @DisplayName("M10-API-S-06: KUAISHOU adapter 同步 stub follower-stats 落库")
    void adapterSyncKuaishouFollowerStatsStub() {
        unifiedCollectorAdapter.bindAccount(KUAISHOU_ACCOUNT_ID);

        int count = unifiedCollectorAdapter.executeKuaishouFollowerStatsCollect(KUAISHOU_ACCOUNT_ID);
        assertEquals(1, count);

        AccountStatusLogDO log = accountStatusLogMapper.selectOne(new LambdaQueryWrapper<AccountStatusLogDO>()
                .eq(AccountStatusLogDO::getTenantId, TENANT_1)
                .eq(AccountStatusLogDO::getAccountId, KUAISHOU_ACCOUNT_ID)
                .eq(AccountStatusLogDO::getStatDate, LocalDate.now()));
        assertNotNull(log);
        assertEquals(88_800L, log.getFollowerCount());
    }

    @Test
    @DisplayName("M10-API-S-06: CollectExecutionService 路由 KUAISHOU_OPEN_API")
    void executionServiceRoutesKuaishouOpenApi() {
        unifiedCollectorAdapter.bindAccount(KUAISHOU_ACCOUNT_ID);

        CollectTaskDO task = new CollectTaskDO();
        task.setTenantId(TENANT_1);
        task.setPlatformType("KUAISHOU");
        task.setAccountId(KUAISHOU_ACCOUNT_ID);
        task.setMethod("INTERNAL");
        task.setSource("KUAISHOU_OPEN_API");

        CollectExecutionResult result = collectExecutionService.execute(task);
        assertTrue(result.isSuccess());
        assertTrue(result.isMultiType());
        assertEquals(3, result.getRecordCount());
    }

    @Test
    @DisplayName("M10-API-S-06: 未绑定 Collector 执行失败")
    void executionFailsWithoutBind() {
        CollectTaskDO task = new CollectTaskDO();
        task.setTenantId(TENANT_1);
        task.setPlatformType("DOUYIN");
        task.setAccountId(DOUYIN_ACCOUNT_ID);
        task.setMethod("INTERNAL");
        task.setSource("DOUYIN_OPEN_API");

        CollectExecutionResult result = collectExecutionService.execute(task);
        assertTrue(!result.isSuccess());
        assertTrue(result.getErrorMessage().contains("绑定"));
    }
}
