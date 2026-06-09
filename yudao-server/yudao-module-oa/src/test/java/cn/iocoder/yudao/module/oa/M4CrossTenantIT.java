package cn.iocoder.yudao.module.oa;

import cn.hutool.crypto.digest.DigestUtil;
import cn.iocoder.yudao.module.oa.dal.dataobject.account.AccountDO;
import cn.iocoder.yudao.module.oa.dal.dataobject.company.CompanyDO;
import cn.iocoder.yudao.module.oa.dal.dataobject.phone.PhoneDO;
import cn.iocoder.yudao.module.oa.dal.dataobject.realname.RealnameDO;
import cn.iocoder.yudao.module.oa.dal.dataobject.realname.RealnameIntermediaryDO;
import cn.iocoder.yudao.module.oa.dal.dataobject.simcard.SimCardDO;
import cn.iocoder.yudao.module.oa.dal.mysql.account.AccountMapper;
import cn.iocoder.yudao.module.oa.dal.mysql.company.CompanyMapper;
import cn.iocoder.yudao.module.oa.dal.mysql.phone.PhoneMapper;
import cn.iocoder.yudao.module.oa.dal.mysql.realname.RealnameIntermediaryMapper;
import cn.iocoder.yudao.module.oa.dal.mysql.realname.RealnameMapper;
import cn.iocoder.yudao.module.oa.dal.mysql.simcard.SimCardMapper;
import cn.iocoder.yudao.module.oa.util.AesUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

/**
 * X-06：M4 模块跨租户隔离（1504）回归。
 * tenant=1 写入数据，tenant-b Token + X-Tenant-Id:2 不可见/不可改。
 */
@AutoConfigureMockMvc
class M4CrossTenantIT extends OaITBase {

    private static final String AUTH_T1 = "Bearer dev-token-oa-admin";
    private static final String AUTH_T2 = "Bearer dev-token-oa-tenantb";
    private static final String TENANT_1 = "1";
    private static final String TENANT_2 = "2";

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private CompanyMapper companyMapper;
    @Autowired
    private RealnameMapper realnameMapper;
    @Autowired
    private RealnameIntermediaryMapper intermediaryMapper;
    @Autowired
    private PhoneMapper phoneMapper;
    @Autowired
    private SimCardMapper simCardMapper;
    @Autowired
    private AccountMapper accountMapper;
    @Autowired
    private AesUtil aesUtil;

    private String uniqueTag;
    private Tenant1Assets assets;

    @BeforeEach
    void seedTenant1Assets() {
        uniqueTag = "XT-" + UUID.randomUUID().toString().substring(0, 8);
        assets = insertTenant1Assets(uniqueTag);
    }

    @Test
    @DisplayName("X-06: tenant-b 列表不可见 tenant-1 公司")
    void tenantBListExcludesTenant1Company() throws Exception {
        mockMvc.perform(get("/admin-api/oa/company/list")
                        .header("Authorization", AUTH_T1)
                        .header("X-Tenant-Id", TENANT_1)
                        .param("companyName", uniqueTag))
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.total").value(1));

        mockMvc.perform(get("/admin-api/oa/company/list")
                        .header("Authorization", AUTH_T2)
                        .header("X-Tenant-Id", TENANT_2)
                        .param("companyName", uniqueTag))
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.total").value(0));
    }

    @Test
    @DisplayName("X-06: tenant-b 访问 tenant-1 公司 mp-stats → 1504")
    void tenantBCannotAccessTenant1CompanyMpStats() throws Exception {
        mockMvc.perform(get("/admin-api/oa/company/" + assets.companyId() + "/mp-stats")
                        .header("Authorization", AUTH_T2)
                        .header("X-Tenant-Id", TENANT_2))
                .andExpect(jsonPath("$.code").value(1504));
    }

    @Test
    @DisplayName("X-06: tenant-b 更新 tenant-1 公司 → 1504")
    void tenantBCannotUpdateTenant1Company() throws Exception {
        mockMvc.perform(put("/admin-api/oa/company/update")
                        .header("Authorization", AUTH_T2)
                        .header("X-Tenant-Id", TENANT_2)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(String.format("""
                                {"id": %d, "companyName": "非法修改", "status": "ENABLED"}
                                """, assets.companyId())))
                .andExpect(jsonPath("$.code").value(1504));
    }

    @Test
    @DisplayName("X-06: tenant-b 访问 tenant-1 实名人详情 → 1504")
    void tenantBCannotGetTenant1Realname() throws Exception {
        mockMvc.perform(get("/admin-api/oa/realname/" + assets.realnameId())
                        .header("Authorization", AUTH_T2)
                        .header("X-Tenant-Id", TENANT_2))
                .andExpect(jsonPath("$.code").value(1504));
    }

    @Test
    @DisplayName("X-06: tenant-b 访问 tenant-1 实名人中介列表 → 1504")
    void tenantBCannotListTenant1Intermediaries() throws Exception {
        mockMvc.perform(get("/admin-api/oa/realname/" + assets.realnameId() + "/intermediaries")
                        .header("Authorization", AUTH_T2)
                        .header("X-Tenant-Id", TENANT_2))
                .andExpect(jsonPath("$.code").value(1504));
    }

    @Test
    @DisplayName("X-06: tenant-b 列表不可见 tenant-1 手机")
    void tenantBListExcludesTenant1Phone() throws Exception {
        mockMvc.perform(get("/admin-api/oa/phone/list")
                        .header("Authorization", AUTH_T1)
                        .header("X-Tenant-Id", TENANT_1)
                        .param("phoneNumber", assets.phonePlain()))
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.total").value(1));

        mockMvc.perform(get("/admin-api/oa/phone/list")
                        .header("Authorization", AUTH_T2)
                        .header("X-Tenant-Id", TENANT_2)
                        .param("phoneNumber", assets.phonePlain()))
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.total").value(0));
    }

    @Test
    @DisplayName("X-06: tenant-b 删除 tenant-1 手机 → 1504")
    void tenantBCannotDeleteTenant1Phone() throws Exception {
        mockMvc.perform(delete("/admin-api/oa/phone/delete")
                        .header("Authorization", AUTH_T2)
                        .header("X-Tenant-Id", TENANT_2)
                        .param("id", String.valueOf(assets.phoneId())))
                .andExpect(jsonPath("$.code").value(1504));
    }

    @Test
    @DisplayName("X-06: tenant-b 访问 tenant-1 手机卡 linked-accounts → 1504")
    void tenantBCannotAccessTenant1SimCardLinkedAccounts() throws Exception {
        mockMvc.perform(get("/admin-api/oa/sim-card/" + assets.simCardId() + "/linked-accounts")
                        .header("Authorization", AUTH_T2)
                        .header("X-Tenant-Id", TENANT_2))
                .andExpect(jsonPath("$.code").value(1504));
    }

    @Test
    @DisplayName("X-06: tenant-b 获取 tenant-1 平台账号 → 1504")
    void tenantBCannotGetTenant1Account() throws Exception {
        mockMvc.perform(get("/admin-api/oa/account/get")
                        .header("Authorization", AUTH_T2)
                        .header("X-Tenant-Id", TENANT_2)
                        .param("id", String.valueOf(assets.accountId())))
                .andExpect(jsonPath("$.code").value(1504));
    }

    @Test
    @DisplayName("X-06: tenant-b 创建账号引用 tenant-1 公司 → 1504")
    void tenantBCannotCreateAccountWithTenant1Company() throws Exception {
        mockMvc.perform(post("/admin-api/oa/account/create")
                        .header("Authorization", AUTH_T2)
                        .header("X-Tenant-Id", TENANT_2)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(String.format("""
                                {
                                  "platformType": "WECHAT_OFFICIAL",
                                  "accountName": "跨租户非法账号",
                                  "externalAccountId": "wx_xt_%s",
                                  "companyId": %d,
                                  "realnameId": %d,
                                  "status": "NORMAL"
                                }
                                """, uniqueTag.replace("-", ""), assets.companyId(), assets.realnameId())))
                .andExpect(jsonPath("$.code").value(1504));
    }

    @Test
    @DisplayName("X-06: tenant-1 Token 配 tenant-2 Header → 1504")
    void tenant1TokenWithTenant2HeaderReturns1504() throws Exception {
        mockMvc.perform(get("/admin-api/oa/company/" + assets.companyId() + "/mp-stats")
                        .header("Authorization", AUTH_T1)
                        .header("X-Tenant-Id", TENANT_2))
                .andExpect(jsonPath("$.code").value(1504));
    }

    private Tenant1Assets insertTenant1Assets(String tag) {
        CompanyDO company = new CompanyDO();
        company.setTenantId(1L);
        company.setCompanyName("跨租户公司" + tag);
        company.setCreditCode(String.format("91330100XT%06d", Math.abs(tag.hashCode()) % 1_000_000));
        company.setStatus("ENABLED");
        company.setMpCapacityStandard(10);
        company.setMpRegisteredCount(0);
        company.setCreator("test");
        company.setUpdater("test");
        company.setCreateTime(LocalDateTime.now());
        company.setUpdateTime(LocalDateTime.now());
        companyMapper.insert(company);

        RealnameDO realname = new RealnameDO();
        realname.setTenantId(1L);
        realname.setCompanyId(company.getId());
        realname.setRealName("跨租户实名人" + tag);
        realname.setIdType("ID_CARD");
        realname.setIdCardEncrypted(aesUtil.encrypt("330101199001011234"));
        realname.setPhoneEncrypted(aesUtil.encrypt("13900001111"));
        realname.setStatus("ENABLED");
        realname.setAccountBoundCount(0);
        realname.setCreator("test");
        realname.setUpdater("test");
        realname.setCreateTime(LocalDateTime.now());
        realname.setUpdateTime(LocalDateTime.now());
        realnameMapper.insert(realname);

        RealnameIntermediaryDO intermediary = new RealnameIntermediaryDO();
        intermediary.setTenantId(1L);
        intermediary.setRealnameId(realname.getId());
        intermediary.setIntermediaryName("跨租户中介" + tag);
        intermediary.setIntermediaryPhoneEncrypted(aesUtil.encrypt("13900002222"));
        intermediary.setRelationType("INTERMEDIARY");
        intermediary.setCreator("test");
        intermediary.setUpdater("test");
        intermediary.setCreateTime(LocalDateTime.now());
        intermediary.setUpdateTime(LocalDateTime.now());
        intermediaryMapper.insert(intermediary);

        String phonePlain = ("138" + String.format("%08d", Math.abs(tag.hashCode()) % 100_000_000L)).substring(0, 11);
        PhoneDO phone = new PhoneDO();
        phone.setTenantId(1L);
        phone.setRealnameId(realname.getId());
        phone.setPhoneNumberEncrypted(aesUtil.encrypt(phonePlain));
        phone.setPhoneNumberHash(DigestUtil.sha256Hex(phonePlain));
        phone.setKeeperId(1001L);
        phone.setStatus("ENABLED");
        phone.setAccountBoundCount(0);
        phone.setCreator("test");
        phone.setUpdater("test");
        phone.setCreateTime(LocalDateTime.now());
        phone.setUpdateTime(LocalDateTime.now());
        phoneMapper.insert(phone);

        SimCardDO sim = new SimCardDO();
        sim.setTenantId(1L);
        sim.setPhoneId(phone.getId());
        sim.setPhoneNumberEncrypted(phone.getPhoneNumberEncrypted());
        sim.setPhoneNumberHash(phone.getPhoneNumberHash());
        sim.setOperator("MOBILE");
        sim.setAssignedUserId(1001L);
        sim.setIsPrimary("YES");
        sim.setStatus("ENABLED");
        sim.setAccountBoundCount(0);
        sim.setCreator("test");
        sim.setUpdater("test");
        sim.setCreateTime(LocalDateTime.now());
        sim.setUpdateTime(LocalDateTime.now());
        simCardMapper.insert(sim);

        AccountDO account = new AccountDO();
        account.setTenantId(1L);
        account.setPlatformType("WECHAT_OFFICIAL");
        account.setAccountName("跨租户账号" + tag);
        account.setExternalAccountId("wx_xt_" + tag.replace("-", ""));
        account.setAccountType("OFFICIAL_ACCOUNT");
        account.setCompanyId(company.getId());
        account.setRealnameId(realname.getId());
        account.setPhoneId(phone.getId());
        account.setSimCardId(sim.getId());
        account.setStatus("NORMAL");
        account.setCreator("test");
        account.setUpdater("test");
        account.setCreateTime(LocalDateTime.now());
        account.setUpdateTime(LocalDateTime.now());
        accountMapper.insert(account);

        return new Tenant1Assets(
                company.getId(),
                realname.getId(),
                intermediary.getId(),
                phone.getId(),
                sim.getId(),
                account.getId(),
                phonePlain);
    }

    private record Tenant1Assets(
            Long companyId,
            Long realnameId,
            Long intermediaryId,
            Long phoneId,
            Long simCardId,
            Long accountId,
            String phonePlain) {
    }
}
