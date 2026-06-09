package cn.iocoder.yudao.module.oa;

import cn.iocoder.yudao.module.oa.dal.dataobject.account.AccountDO;
import cn.iocoder.yudao.module.oa.dal.dataobject.phone.PhoneDO;
import cn.iocoder.yudao.module.oa.dal.dataobject.simcard.SimCardDO;
import cn.iocoder.yudao.module.oa.dal.mysql.account.AccountMapper;
import cn.iocoder.yudao.module.oa.dal.mysql.phone.PhoneMapper;
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
class M4SimCardS05IT extends OaITBase {

    private static final String AUTH = "Bearer dev-token-oa-admin";
    private static final String TENANT = "1";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private SimCardMapper simCardMapper;

    @Autowired
    private PhoneMapper phoneMapper;

    @Autowired
    private AccountMapper accountMapper;

    @Autowired
    private AesUtil aesUtil;

    @Test
    @DisplayName("S-05: 创建手机卡 + 列表脱敏")
    void createAndList() throws Exception {
        createSimCard("13800138100", "89860000000000000100", "MOBILE");

        mockMvc.perform(get("/admin-api/oa/sim-card/list")
                        .header("Authorization", AUTH)
                        .header("X-Tenant-Id", TENANT)
                        .param("iccid", "89860000000000000100"))
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.list[0].phoneNumberMasked").value("138****8100"));
    }

    @Test
    @DisplayName("S-05: 手机号唯一 (2006)")
    void duplicatePhoneFails() throws Exception {
        createSimCard("13800138101", "89860000000000000101", "MOBILE");
        mockMvc.perform(post("/admin-api/oa/sim-card/create")
                        .header("Authorization", AUTH)
                        .header("X-Tenant-Id", TENANT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "phoneNumber": "13800138101",
                                  "operator": "MOBILE",
                                  "assignedUserId": 1001,
                                  "iccid": "89860000000000000102",
                                  "status": "ENABLED"
                                }
                                """))
                .andExpect(jsonPath("$.code").value(2006));
    }

    @Test
    @DisplayName("S-05: 跨平台账号聚合")
    void linkedAccountsAggregation() throws Exception {
        String phone = "13800138102";
        Long phoneId = seedPhone(phone);
        Long simId = createSimCard(phone, "89860000000000000103", "MOBILE");
        seedAccount(phoneId, phone, "WECHAT_OFFICIAL", "联调公众号", "wx_test_1");
        seedAccount(phoneId, phone, "DOUYIN", "联调抖音", "dy_test_1");
        seedAccount(phoneId, phone, "WEWORK", "联调企微", "ww_test_1");

        mockMvc.perform(get("/admin-api/oa/sim-card/" + simId + "/linked-accounts")
                        .header("Authorization", AUTH)
                        .header("X-Tenant-Id", TENANT))
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.totalCount").value(3))
                .andExpect(jsonPath("$.data.accounts.length()").value(3));
    }

    @Test
    @DisplayName("S-05: 按平台筛选 linked-accounts")
    void linkedAccountsFilterByPlatform() throws Exception {
        String phone = "13800138103";
        Long phoneId = seedPhone(phone);
        Long simId = createSimCard(phone, "89860000000000000104", "MOBILE");
        seedAccount(phoneId, phone, "WECHAT_OFFICIAL", "公众号A", "wx_a");
        seedAccount(phoneId, phone, "DOUYIN", "抖音A", "dy_a");

        mockMvc.perform(get("/admin-api/oa/sim-card/" + simId + "/linked-accounts")
                        .header("Authorization", AUTH)
                        .header("X-Tenant-Id", TENANT)
                        .param("platformType", "WECHAT_OFFICIAL"))
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.totalCount").value(1))
                .andExpect(jsonPath("$.data.accounts[0].platformType").value("WECHAT_OFFICIAL"));
    }

    @Test
    @DisplayName("S-05: 按运营商筛选 linked-accounts")
    void linkedAccountsFilterByOperator() throws Exception {
        String phone = "13800138104";
        Long phoneId = seedPhone(phone);
        Long simId = createSimCard(phone, "89860000000000000105", "UNICOM");
        seedAccount(phoneId, phone, "WECHAT_OFFICIAL", "公众号B", "wx_b");

        mockMvc.perform(get("/admin-api/oa/sim-card/" + simId + "/linked-accounts")
                        .header("Authorization", AUTH)
                        .header("X-Tenant-Id", TENANT)
                        .param("operator", "MOBILE"))
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.totalCount").value(0));
    }

    @Test
    @DisplayName("S-05: 被引用时不可删除 (1502)")
    void deleteBoundSimFails() throws Exception {
        Long id = createSimCard("13800138105", "89860000000000000106", "MOBILE");
        SimCardDO entity = simCardMapper.selectById(id);
        entity.setAccountBoundCount(1);
        simCardMapper.updateById(entity);

        mockMvc.perform(delete("/admin-api/oa/sim-card/delete")
                        .header("Authorization", AUTH)
                        .header("X-Tenant-Id", TENANT)
                        .param("id", String.valueOf(id)))
                .andExpect(jsonPath("$.code").value(1502));
    }

    @Test
    @DisplayName("S-05: 更新 + 删除手机卡")
    void updateAndDelete() throws Exception {
        Long id = createSimCard("13800138106", "89860000000000000107", "TELECOM");

        mockMvc.perform(put("/admin-api/oa/sim-card/update")
                        .header("Authorization", AUTH)
                        .header("X-Tenant-Id", TENANT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(String.format("""
                                {"id": %d, "packageName": "199元/月", "status": "ENABLED"}
                                """, id)))
                .andExpect(jsonPath("$.code").value(0));

        mockMvc.perform(delete("/admin-api/oa/sim-card/delete")
                        .header("Authorization", AUTH)
                        .header("X-Tenant-Id", TENANT)
                        .param("id", String.valueOf(id)))
                .andExpect(jsonPath("$.code").value(0));
    }

    private Long createSimCard(String phoneNumber, String iccid, String operator) throws Exception {
        MvcResult result = mockMvc.perform(post("/admin-api/oa/sim-card/create")
                        .header("Authorization", AUTH)
                        .header("X-Tenant-Id", TENANT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(String.format("""
                                {
                                  "phoneNumber": "%s",
                                  "operator": "%s",
                                  "assignedUserId": 1001,
                                  "iccid": "%s",
                                  "isPrimary": "YES",
                                  "status": "ENABLED"
                                }
                                """, phoneNumber, operator, iccid)))
                .andExpect(jsonPath("$.code").value(0))
                .andReturn();
        return JsonPath.parse(result.getResponse().getContentAsString()).read("$.data", Long.class);
    }

    private Long seedPhone(String phoneNumber) {
        PhoneDO phone = new PhoneDO();
        phone.setTenantId(1L);
        phone.setPhoneNumberEncrypted(aesUtil.encrypt(phoneNumber));
        phone.setPhoneNumberHash(cn.hutool.crypto.digest.DigestUtil.sha256Hex(phoneNumber));
        phone.setKeeperId(1001L);
        phone.setStatus("ENABLED");
        phone.setAccountBoundCount(0);
        phone.setCreator("test");
        phone.setUpdater("test");
        phone.setCreateTime(LocalDateTime.now());
        phone.setUpdateTime(LocalDateTime.now());
        phoneMapper.insert(phone);
        return phone.getId();
    }

    private void seedAccount(Long phoneId, String phoneNumber, String platform, String name, String extId) {
        AccountDO account = new AccountDO();
        account.setTenantId(1L);
        account.setPlatformType(platform);
        account.setAccountName(name);
        account.setExternalAccountId(extId);
        account.setPhoneId(phoneId);
        account.setPhoneNumberHash(cn.hutool.crypto.digest.DigestUtil.sha256Hex(phoneNumber));
        account.setStatus("NORMAL");
        account.setLinkedAt(LocalDateTime.now());
        account.setCreator("test");
        account.setUpdater("test");
        account.setCreateTime(LocalDateTime.now());
        account.setUpdateTime(LocalDateTime.now());
        accountMapper.insert(account);
    }
}
