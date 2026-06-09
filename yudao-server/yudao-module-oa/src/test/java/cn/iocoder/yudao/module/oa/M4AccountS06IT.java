package cn.iocoder.yudao.module.oa;

import cn.iocoder.yudao.module.oa.dal.dataobject.account.AccountDO;
import cn.iocoder.yudao.module.oa.dal.dataobject.company.CompanyDO;
import cn.iocoder.yudao.module.oa.dal.dataobject.phone.PhoneDO;
import cn.iocoder.yudao.module.oa.dal.dataobject.realname.RealnameDO;
import cn.iocoder.yudao.module.oa.dal.dataobject.simcard.SimCardDO;
import cn.iocoder.yudao.module.oa.dal.mysql.account.AccountMapper;
import cn.iocoder.yudao.module.oa.dal.mysql.company.CompanyMapper;
import cn.iocoder.yudao.module.oa.dal.mysql.phone.PhoneMapper;
import cn.iocoder.yudao.module.oa.dal.mysql.realname.RealnameMapper;
import cn.iocoder.yudao.module.oa.dal.mysql.simcard.SimCardMapper;
import cn.iocoder.yudao.module.oa.util.AesUtil;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.LocalDateTime;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

@AutoConfigureMockMvc
class M4AccountS06IT extends OaITBase {

    private static final String AUTH = "Bearer dev-token-oa-admin";
    private static final String TENANT = "1";

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private CompanyMapper companyMapper;
    @Autowired
    private RealnameMapper realnameMapper;
    @Autowired
    private PhoneMapper phoneMapper;
    @Autowired
    private SimCardMapper simCardMapper;
    @Autowired
    private AccountMapper accountMapper;
    @Autowired
    private AesUtil aesUtil;

    @Test
    @DisplayName("S-06: 创建平台账号 + 列表")
    void createAndList() throws Exception {
        TestAssets assets = seedAssets("A06-1");
        createAccount(assets, "wx_a06_001", "联调账号A06-1");

        mockMvc.perform(get("/admin-api/oa/account/list")
                        .header("Authorization", AUTH)
                        .header("X-Tenant-Id", TENANT)
                        .param("platformType", "WECHAT_OFFICIAL")
                        .param("accountName", "联调账号A06-1"))
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.total").value(1));
    }

    @Test
    @DisplayName("S-06: 同平台 accountId 唯一 (2006)")
    void duplicateExternalIdFails() throws Exception {
        TestAssets assets = seedAssets("A06-2");
        createAccount(assets, "wx_a06_dup");
        mockMvc.perform(post("/admin-api/oa/account/create")
                        .header("Authorization", AUTH)
                        .header("X-Tenant-Id", TENANT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(String.format("""
                                {
                                  "platformType": "WECHAT_OFFICIAL",
                                  "accountName": "重复账号",
                                  "externalAccountId": "wx_a06_dup",
                                  "companyId": %d,
                                  "realnameId": %d,
                                  "status": "NORMAL"
                                }
                                """, assets.companyId(), assets.realnameId())))
                .andExpect(jsonPath("$.code").value(2006));
    }

    @Test
    @DisplayName("S-06: 手机已被引用 (1502)")
    void phoneAlreadyBoundFails() throws Exception {
        TestAssets assets = seedAssets("A06-3");
        createAccount(assets, "wx_a06_b1");
        mockMvc.perform(post("/admin-api/oa/account/create")
                        .header("Authorization", AUTH)
                        .header("X-Tenant-Id", TENANT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(String.format("""
                                {
                                  "platformType": "DOUYIN",
                                  "accountName": "冲突账号",
                                  "externalAccountId": "dy_a06_b2",
                                  "companyId": %d,
                                  "realnameId": %d,
                                  "phoneId": %d,
                                  "status": "NORMAL"
                                }
                                """, assets.companyId(), assets.realnameId(), assets.phoneId())))
                .andExpect(jsonPath("$.code").value(1502));
    }

    @Test
    @DisplayName("S-06: 强制替换手机绑定")
    void forceReplacePhone() throws Exception {
        TestAssets assets = seedAssets("A06-4");
        Long account1 = createAccount(assets, "wx_a06_fr1");
        mockMvc.perform(post("/admin-api/oa/account/create")
                        .header("Authorization", AUTH)
                        .header("X-Tenant-Id", TENANT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(String.format("""
                                {
                                  "platformType": "DOUYIN",
                                  "accountName": "替换账号",
                                  "externalAccountId": "dy_a06_fr2",
                                  "companyId": %d,
                                  "realnameId": %d,
                                  "phoneId": %d,
                                  "forceReplace": true,
                                  "reason": "联调测试强制替换手机绑定",
                                  "status": "NORMAL"
                                }
                                """, assets.companyId(), assets.realnameId(), assets.phoneId())))
                .andExpect(jsonPath("$.code").value(0));

        AccountDO old = accountMapper.selectById(account1);
        org.junit.jupiter.api.Assertions.assertNull(old.getPhoneId());
    }

    @Test
    @DisplayName("S-06: replace 接口")
    void replaceEndpoint() throws Exception {
        TestAssets assets = seedAssets("A06-5");
        Long accountId = createAccount(assets, "wx_a06_rp");
        RealnameDO newRealname = seedRealname(assets.companyId(), "替换实名人A06");
        mockMvc.perform(post("/admin-api/oa/account/" + accountId + "/replace")
                        .header("Authorization", AUTH)
                        .header("X-Tenant-Id", TENANT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(String.format("""
                                {"realnameId": %d, "reason": "联调测试 replace 接口替换实名人"}
                                """, newRealname.getId())))
                .andExpect(jsonPath("$.code").value(0));
    }

    @Test
    @DisplayName("S-06: 删除账号")
    void deleteAccount() throws Exception {
        TestAssets assets = seedAssets("A06-6");
        Long id = createAccount(assets, "wx_a06_del");
        mockMvc.perform(delete("/admin-api/oa/account/delete")
                        .header("Authorization", AUTH)
                        .header("X-Tenant-Id", TENANT)
                        .param("id", String.valueOf(id)))
                .andExpect(jsonPath("$.code").value(0));
    }

    private Long createAccount(TestAssets assets, String externalId) throws Exception {
        return createAccount(assets, externalId, "联调账号A06");
    }

    private Long createAccount(TestAssets assets, String externalId, String accountName) throws Exception {
        MvcResult result = mockMvc.perform(post("/admin-api/oa/account/create")
                        .header("Authorization", AUTH)
                        .header("X-Tenant-Id", TENANT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(String.format("""
                                {
                                  "platformType": "WECHAT_OFFICIAL",
                                  "accountName": "%s",
                                  "externalAccountId": "%s",
                                  "accountType": "OFFICIAL_ACCOUNT",
                                  "companyId": %d,
                                  "realnameId": %d,
                                  "phoneId": %d,
                                  "simCardId": %d,
                                  "status": "NORMAL"
                                }
                                """, accountName, externalId, assets.companyId(), assets.realnameId(),
                                assets.phoneId(), assets.simCardId())))
                .andExpect(jsonPath("$.code").value(0))
                .andReturn();
        return JsonPath.parse(result.getResponse().getContentAsString()).read("$.data", Long.class);
    }

    private TestAssets seedAssets(String suffix) {
        CompanyDO company = new CompanyDO();
        company.setTenantId(1L);
        company.setCompanyName("联调公司" + suffix);
        company.setCreditCode(String.format("91330100MA2H%05d", Math.abs(suffix.hashCode()) % 100000));
        company.setStatus("ENABLED");
        company.setMpCapacityStandard(10);
        company.setMpRegisteredCount(0);
        company.setCreator("test");
        company.setUpdater("test");
        company.setCreateTime(LocalDateTime.now());
        company.setUpdateTime(LocalDateTime.now());
        companyMapper.insert(company);

        RealnameDO realname = seedRealname(company.getId(), "联调实名人" + suffix);

        String phoneNum = ("138" + String.format("%08d", Math.abs(suffix.hashCode()) % 100_000_000L)).substring(0, 11);
        PhoneDO phone = new PhoneDO();
        phone.setTenantId(1L);
        phone.setRealnameId(realname.getId());
        phone.setPhoneNumberEncrypted(aesUtil.encrypt(phoneNum));
        phone.setPhoneNumberHash(cn.hutool.crypto.digest.DigestUtil.sha256Hex(phoneNum));
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

        return new TestAssets(company.getId(), realname.getId(), phone.getId(), sim.getId());
    }

    private RealnameDO seedRealname(Long companyId, String name) {
        RealnameDO realname = new RealnameDO();
        realname.setTenantId(1L);
        realname.setCompanyId(companyId);
        realname.setRealName(name);
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
        return realname;
    }

    private record TestAssets(Long companyId, Long realnameId, Long phoneId, Long simCardId) {
    }
}
