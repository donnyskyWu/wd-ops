package cn.iocoder.yudao.module.oa;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.oa.api.dto.collect.CollectorAccountBindSaveReq;
import cn.iocoder.yudao.module.oa.dal.dataobject.account.AccountDO;
import cn.iocoder.yudao.module.oa.dal.dataobject.collect.CollectorAccountBindDO;
import cn.iocoder.yudao.module.oa.dal.dataobject.dict.SysDictDataDO;
import cn.iocoder.yudao.module.oa.dal.mysql.account.AccountMapper;
import cn.iocoder.yudao.module.oa.dal.mysql.collect.CollectorAccountBindMapper;
import cn.iocoder.yudao.module.oa.dal.mysql.dict.SysDictDataMapper;
import cn.iocoder.yudao.module.oa.service.collect.CollectorAccountBindService;
import cn.iocoder.yudao.module.oa.util.AesUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * M10-API-S-01: V110 迁移 + Collector 账号 bind 服务骨架。
 */
class M10ApiCollectorBindS01IT extends OaITBase {

    private static final long TENANT_1 = 1L;
    private static final long SEED_ACCOUNT_ID = 9001L;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private SysDictDataMapper sysDictDataMapper;

    @Autowired
    private CollectorAccountBindMapper collectorAccountBindMapper;

    @Autowired
    private CollectorAccountBindService collectorAccountBindService;

    @Autowired
    private AccountMapper accountMapper;

    @Autowired
    private AesUtil aesUtil;

    @BeforeEach
    void setTenantContext() {
        TenantContextHolder.setTenantId(TENANT_1);
        TenantContextHolder.setUsername("it-m10-api-s01");
    }

    @AfterEach
    void clearTenantContext() {
        TenantContextHolder.clear();
    }

    @Test
    @DisplayName("M10-API-S-01: V110 迁移 — bind 表与 oa_account 扩展列")
    void migrationApplied() {
        Integer bindTable = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.tables WHERE table_name = 'OA_COLLECTOR_ACCOUNT_BIND'",
                Integer.class);
        assertNotNull(bindTable);
        assertTrue(bindTable >= 1, "oa_collector_account_bind 表应存在");

        Integer mpTokenCol = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.columns "
                        + "WHERE table_name = 'OA_ACCOUNT' AND column_name = 'MP_TOKEN_ENCRYPTED'",
                Integer.class);
        assertNotNull(mpTokenCol);
        assertTrue(mpTokenCol >= 1, "oa_account.mp_token_encrypted 列应存在");

        Long dictCount = sysDictDataMapper.selectCount(new LambdaQueryWrapper<SysDictDataDO>()
                .eq(SysDictDataDO::getDictType, "dict_collector_bind_status"));
        assertTrue(dictCount != null && dictCount >= 3, "dict_collector_bind_status 应含 BOUND/PENDING/FAILED");
    }

    @Test
    @DisplayName("M10-API-S-01: bind 保存与按 oa_account_id 查询")
    void bindSaveAndGet() {
        CollectorAccountBindSaveReq req = new CollectorAccountBindSaveReq();
        req.setOaAccountId(SEED_ACCOUNT_ID);
        req.setCollectorAccountId("acc_wechat_mp_test001");
        req.setPlatformType("WECHAT_OFFICIAL");
        req.setBindStatus("BOUND");
        req.setConnStatus("CONNECTED");

        Long id = collectorAccountBindService.saveOrUpdate(req);
        assertNotNull(id);

        var resp = collectorAccountBindService.getByOaAccountId(SEED_ACCOUNT_ID);
        assertNotNull(resp);
        assertEquals("acc_wechat_mp_test001", resp.getCollectorAccountId());
        assertEquals("BOUND", resp.getBindStatus());
        assertEquals("CONNECTED", resp.getConnStatus());
        assertNotNull(resp.getLastBindAt());

        req.setCollectorAccountId("acc_wechat_mp_test002");
        req.setBindStatus("BOUND");
        collectorAccountBindService.saveOrUpdate(req);

        resp = collectorAccountBindService.getByOaAccountId(SEED_ACCOUNT_ID);
        assertEquals("acc_wechat_mp_test002", resp.getCollectorAccountId());
    }

    @Test
    @DisplayName("M10-API-S-01: 重复 collector_account_id → 2021")
    void duplicateCollectorAccountIdFails() {
        CollectorAccountBindSaveReq first = new CollectorAccountBindSaveReq();
        first.setOaAccountId(SEED_ACCOUNT_ID);
        first.setCollectorAccountId("acc_wechat_mp_dup");
        first.setPlatformType("WECHAT_OFFICIAL");
        first.setBindStatus("BOUND");
        collectorAccountBindService.saveOrUpdate(first);

        CollectorAccountBindSaveReq second = new CollectorAccountBindSaveReq();
        second.setOaAccountId(9002L);
        second.setCollectorAccountId("acc_wechat_mp_dup");
        second.setPlatformType("WECHAT_OFFICIAL");
        second.setBindStatus("BOUND");

        ServiceException ex = assertThrows(ServiceException.class, () -> collectorAccountBindService.saveOrUpdate(second));
        assertEquals(2021, ex.getCode());
    }

    @Test
    @DisplayName("M10-API-S-01: 账号不存在 → 1500")
    void missingAccountFails() {
        CollectorAccountBindSaveReq req = new CollectorAccountBindSaveReq();
        req.setOaAccountId(99999999L);
        req.setCollectorAccountId("acc_missing");
        req.setPlatformType("WECHAT_OFFICIAL");

        ServiceException ex = assertThrows(ServiceException.class, () -> collectorAccountBindService.saveOrUpdate(req));
        assertEquals(1500, ex.getCode());
    }

    @Test
    @DisplayName("M10-API-S-01: 未绑定账号查询返回 null")
    void unboundAccountReturnsNull() {
        long unboundCount = collectorAccountBindMapper.selectCount(new LambdaQueryWrapper<CollectorAccountBindDO>()
                .eq(CollectorAccountBindDO::getTenantId, TENANT_1)
                .eq(CollectorAccountBindDO::getOaAccountId, 9010L));
        if (unboundCount == 0) {
            assertNull(collectorAccountBindService.getByOaAccountId(9010L));
        }
    }

    @Test
    @DisplayName("M10-API-S-01: V119 迁移 — 凭证列不再限制 512 字符")
    void credentialColumnsAreText() {
        assertCredentialColumnCapacity("COOKIE_ENCRYPTED");
        assertCredentialColumnCapacity("MP_TOKEN_ENCRYPTED");
        assertCredentialColumnCapacity("AUTH_TOKEN_ENCRYPTED");
    }

    private void assertCredentialColumnCapacity(String columnName) {
        String dataType = jdbcTemplate.queryForObject(
                "SELECT DATA_TYPE FROM information_schema.columns "
                        + "WHERE table_name = 'OA_ACCOUNT' AND column_name = ?",
                String.class, columnName);
        Integer maxLen = jdbcTemplate.queryForObject(
                "SELECT CHARACTER_MAXIMUM_LENGTH FROM information_schema.columns "
                        + "WHERE table_name = 'OA_ACCOUNT' AND column_name = ?",
                Integer.class, columnName);
        assertNotNull(dataType);
        String typeUpper = dataType.toUpperCase();
        boolean unbounded = typeUpper.contains("TEXT") || typeUpper.contains("CLOB")
                || maxLen == null || maxLen > 512;
        assertTrue(unbounded,
                columnName + " 应支持长凭证，type=" + dataType + " maxLen=" + maxLen);
    }

    @Test
    @DisplayName("M10-API-S-01: oa_account 长 Cookie 凭证列可持久化 AES")
    void accountLongCookieCredentialPersists() {
        AccountDO account = accountMapper.selectById(SEED_ACCOUNT_ID);
        assertNotNull(account);

        String longCookie = "bizuin=999;" + "C".repeat(2500);
        account.setCookieEncrypted(aesUtil.encrypt(longCookie));
        account.setMpTokenEncrypted(aesUtil.encrypt("mp-" + "D".repeat(600)));
        accountMapper.updateById(account);

        AccountDO loaded = accountMapper.selectById(SEED_ACCOUNT_ID);
        assertEquals(longCookie, aesUtil.decrypt(loaded.getCookieEncrypted()));
    }

    @Test
    @DisplayName("M10-API-S-01: oa_account 凭证列可持久化 AES")
    void accountCredentialColumnsPersist() {
        AccountDO account = accountMapper.selectById(SEED_ACCOUNT_ID);
        assertNotNull(account);

        account.setMpTokenEncrypted(aesUtil.encrypt("mp-token-plain"));
        account.setAuthTokenEncrypted(aesUtil.encrypt("auth-token-plain"));
        account.setAppSecretEncrypted(aesUtil.encrypt("app-secret-plain"));
        account.setAppId("wx-app-id-test");
        account.setFieldMapping("{\"follower\":\"fans\"}");
        accountMapper.updateById(account);

        AccountDO loaded = accountMapper.selectById(SEED_ACCOUNT_ID);
        assertEquals("wx-app-id-test", loaded.getAppId());
        assertEquals("{\"follower\":\"fans\"}", loaded.getFieldMapping());
        assertEquals("mp-token-plain", aesUtil.decrypt(loaded.getMpTokenEncrypted()));
        assertEquals("auth-token-plain", aesUtil.decrypt(loaded.getAuthTokenEncrypted()));
        assertEquals("app-secret-plain", aesUtil.decrypt(loaded.getAppSecretEncrypted()));
    }
}
