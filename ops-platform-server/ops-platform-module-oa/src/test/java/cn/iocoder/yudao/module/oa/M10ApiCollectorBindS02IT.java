package cn.iocoder.yudao.module.oa;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.oa.api.dto.collect.CollectorAccountBindRespVO;
import cn.iocoder.yudao.module.oa.dal.dataobject.account.AccountDO;
import cn.iocoder.yudao.module.oa.dal.mysql.account.AccountMapper;
import cn.iocoder.yudao.module.oa.service.collect.unified.UnifiedCollectorAdapter;
import cn.iocoder.yudao.module.oa.util.AesUtil;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * M10-API-S-02: UnifiedCollectorAdapter + bind REST API。
 */
@AutoConfigureMockMvc
class M10ApiCollectorBindS02IT extends OaITBase {

    private static final long TENANT_1 = 1L;
    private static final long SEED_ACCOUNT_ID = 9001L;
    private static final String AUTH = "Bearer dev-token-oa-admin";
    private static final String TENANT = "1";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UnifiedCollectorAdapter unifiedCollectorAdapter;

    @Autowired
    private AccountMapper accountMapper;

    @Autowired
    private AesUtil aesUtil;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setTenantContext() {
        TenantContextHolder.setTenantId(TENANT_1);
        TenantContextHolder.setUsername("it-m10-api-s02");
        jdbcTemplate.update("DELETE FROM oa_collector_account_bind WHERE oa_account_id = ?", SEED_ACCOUNT_ID);
        seedWechatOfficialCredentials();
    }

    @AfterEach
    void clearTenantContext() {
        TenantContextHolder.clear();
    }

    private void seedWechatOfficialCredentials() {
        AccountDO account = accountMapper.selectById(SEED_ACCOUNT_ID);
        account.setCookieEncrypted(aesUtil.encrypt("bizuin=123; data_bizuin=123"));
        account.setMpTokenEncrypted(aesUtil.encrypt("1234567890"));
        accountMapper.updateById(account);
    }

    @Test
    @DisplayName("M10-API-S-02: stub bind 写入 BOUND 映射")
    void bindAccountViaAdapterStub() {
        CollectorAccountBindRespVO resp = unifiedCollectorAdapter.bindAccount(SEED_ACCOUNT_ID);
        assertNotNull(resp);
        assertEquals(SEED_ACCOUNT_ID, resp.getOaAccountId());
        assertEquals("acc_wechat_mp_stub001", resp.getCollectorAccountId());
        assertEquals("BOUND", resp.getBindStatus());
        assertEquals("WECHAT_OFFICIAL", resp.getPlatformType());
        assertEquals("CONNECTED", resp.getConnStatus());
    }

    @Test
    @DisplayName("M10-API-S-02: REST bind + GET + test-connection")
    void bindAndTestViaRestStub() throws Exception {
        mockMvc.perform(post("/admin-api/oa/account/" + SEED_ACCOUNT_ID + "/collector-bind")
                        .header("Authorization", AUTH)
                        .header("X-Tenant-Id", TENANT))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.collectorAccountId").value("acc_wechat_mp_stub001"))
                .andExpect(jsonPath("$.data.bindStatus").value("BOUND"));

        mockMvc.perform(get("/admin-api/oa/account/" + SEED_ACCOUNT_ID + "/collector-bind")
                        .header("Authorization", AUTH)
                        .header("X-Tenant-Id", TENANT))
                .andExpect(jsonPath("$.data.oaAccountId").value(SEED_ACCOUNT_ID));

        mockMvc.perform(post("/admin-api/oa/account/" + SEED_ACCOUNT_ID + "/collector-bind/test-connection")
                        .header("Authorization", AUTH)
                        .header("X-Tenant-Id", TENANT))
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.success").value(true))
                .andExpect(jsonPath("$.data.connStatus").value("CONNECTED"));
    }

    @Test
    @DisplayName("M10-API-S-02: sync 凭证重新 import")
    void syncCredentialsViaRestStub() throws Exception {
        unifiedCollectorAdapter.bindAccount(SEED_ACCOUNT_ID);

        mockMvc.perform(post("/admin-api/oa/account/" + SEED_ACCOUNT_ID + "/collector-bind/sync")
                        .header("Authorization", AUTH)
                        .header("X-Tenant-Id", TENANT))
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.bindStatus").value("BOUND"))
                .andExpect(jsonPath("$.data.collectorAccountId").value("acc_wechat_mp_stub001"));
    }

    @Test
    @DisplayName("M10-API-S-02: 账号不存在 → 1500")
    void missingAccountReturns1500() {
        ServiceException ex = assertThrows(ServiceException.class,
                () -> unifiedCollectorAdapter.bindAccount(99999999L));
        assertEquals(1500, ex.getCode());
    }

    @Test
    @DisplayName("M10-API-S-02: 凭证不完整 → 1500")
    void incompleteCredentialsReturns1500() {
        accountMapper.update(null, new LambdaUpdateWrapper<AccountDO>()
                .eq(AccountDO::getId, SEED_ACCOUNT_ID)
                .set(AccountDO::getMpTokenEncrypted, null));

        ServiceException ex = assertThrows(ServiceException.class,
                () -> unifiedCollectorAdapter.bindAccount(SEED_ACCOUNT_ID));
        assertEquals(1500, ex.getCode());
    }

    @Test
    @DisplayName("M10-API-S-02: 未绑定 test-connection → 1500")
    void testConnectionWithoutBindReturns1500() {
        ServiceException ex = assertThrows(ServiceException.class,
                () -> unifiedCollectorAdapter.testConnection(9010L));
        assertEquals(1500, ex.getCode());
    }
}
