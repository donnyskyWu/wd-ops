package cn.iocoder.yudao.module.oa;

import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.oa.dal.dataobject.account.AccountDO;
import cn.iocoder.yudao.module.oa.dal.dataobject.collect.CollectorAccountBindDO;
import cn.iocoder.yudao.module.oa.dal.mysql.account.AccountMapper;
import cn.iocoder.yudao.module.oa.dal.mysql.collect.CollectorAccountBindMapper;
import cn.iocoder.yudao.module.oa.util.AesUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * V113 后批量 bind：凭证齐全且无 bind 行的 Channel-A 账号经 stub collector import。
 */
@AutoConfigureMockMvc
class M10ApiCollectorBatchBindIT extends OaITBase {

    private static final long TENANT_1 = 1L;
    private static final long WECHAT_VIDEO_ACCOUNT_ID = 9004L;
    private static final long DOUYIN_ACCOUNT_ID = 9006L;
    private static final String AUTH = "Bearer dev-token-oa-admin";
    private static final String TENANT = "1";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AccountMapper accountMapper;

    @Autowired
    private CollectorAccountBindMapper collectorAccountBindMapper;

    @Autowired
    private AesUtil aesUtil;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        TenantContextHolder.setTenantId(TENANT_1);
        TenantContextHolder.setUsername("it-m10-api-batch-bind");
        cleanupBinds(WECHAT_VIDEO_ACCOUNT_ID, DOUYIN_ACCOUNT_ID);
        seedCredentials(WECHAT_VIDEO_ACCOUNT_ID, "sessionid=stub_wv_channels");
        seedCredentials(DOUYIN_ACCOUNT_ID, "sessionid=stub_douyin_batch; sid_guard=stub");
    }

    @AfterEach
    void tearDown() {
        cleanupBinds(WECHAT_VIDEO_ACCOUNT_ID, DOUYIN_ACCOUNT_ID);
        TenantContextHolder.clear();
    }

    private void cleanupBinds(long... accountIds) {
        for (long accountId : accountIds) {
            jdbcTemplate.update("DELETE FROM oa_collector_account_bind WHERE oa_account_id = ?", accountId);
        }
    }

    private void seedCredentials(long accountId, String cookiePlain) {
        AccountDO account = accountMapper.selectById(accountId);
        account.setCookieEncrypted(aesUtil.encrypt(cookiePlain));
        accountMapper.updateById(account);
    }

    @Test
    @DisplayName("M10-API batch-import: 2 个 seed 账号 stub 绑定成功")
    void batchImportBindsTwoSeedAccounts() throws Exception {
        mockMvc.perform(post("/admin-api/oa/collector-bind/batch-import")
                        .header("Authorization", AUTH)
                        .header("X-Tenant-Id", TENANT))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.failed").value(0))
                .andExpect(jsonPath("$.data.imported").value(org.hamcrest.Matchers.greaterThanOrEqualTo(2)));

        CollectorAccountBindDO wvBind = collectorAccountBindMapper.selectOne(
                new LambdaQueryWrapper<CollectorAccountBindDO>()
                        .eq(CollectorAccountBindDO::getTenantId, TENANT_1)
                        .eq(CollectorAccountBindDO::getOaAccountId, WECHAT_VIDEO_ACCOUNT_ID));
        assertNotNull(wvBind);
        assertEquals("BOUND", wvBind.getBindStatus());
        assertTrue(wvBind.getCollectorAccountId().startsWith("acc_"));

        CollectorAccountBindDO dyBind = collectorAccountBindMapper.selectOne(
                new LambdaQueryWrapper<CollectorAccountBindDO>()
                        .eq(CollectorAccountBindDO::getTenantId, TENANT_1)
                        .eq(CollectorAccountBindDO::getOaAccountId, DOUYIN_ACCOUNT_ID));
        assertNotNull(dyBind);
        assertEquals("BOUND", dyBind.getBindStatus());
    }
}
