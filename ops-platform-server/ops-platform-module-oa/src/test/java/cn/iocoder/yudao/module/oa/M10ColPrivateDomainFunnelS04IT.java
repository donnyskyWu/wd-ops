package cn.iocoder.yudao.module.oa;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * M10-COL-S-04: 私域转化预置漏斗 — 桥接表数据驱动 M6 漏斗分析。
 */
@AutoConfigureMockMvc
class M10ColPrivateDomainFunnelS04IT extends OaITBase {

    private static final String AUTH = "Bearer dev-token-oa-admin";
    private static final String TENANT = "1";
    private static final long PRIVATE_DOMAIN_FUNNEL_ID = 99401L;
    private static final String PERSONAL_BASE = "/admin-api/oa/internal/personal-account";
    private static final String BRIDGE_BASE = "/admin-api/oa/collect/private-domain-bridge";

    @Autowired
    private MockMvc mockMvc;

    private void seedAoApi() throws Exception {
        mockMvc.perform(post("/admin-api/oa/config/internal-collect/aocreate")
                        .header("Authorization", AUTH)
                        .header("X-Tenant-Id", TENANT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "apiUrl": "https://ao.stub.local",
                                  "appId": "stub-app-id",
                                  "appSecret": "stub-secret",
                                  "token": "stub-token"
                                }
                                """))
                .andExpect(jsonPath("$.code").value(0));
    }

    private Long createAoAccount(String aochuangAccountId) throws Exception {
        MvcResult aoResult = mockMvc.perform(post("/admin-api/oa/config/internal-collect/aocreate/accounts/create")
                        .header("Authorization", AUTH)
                        .header("X-Tenant-Id", TENANT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(String.format("""
                                {
                                  "accountName": "漏斗测试-%s",
                                  "aochuangAccountId": "%s",
                                  "status": "ENABLED"
                                }
                                """, aochuangAccountId, aochuangAccountId)))
                .andExpect(jsonPath("$.code").value(0))
                .andReturn();
        return JsonPath.parse(aoResult.getResponse().getContentAsString()).read("$.data", Long.class);
    }

    private Long createBoundPersonal(Long aoAccountId, String wechatId, String deviceId) throws Exception {
        MvcResult createResult = mockMvc.perform(post(PERSONAL_BASE + "/create-and-bind")
                        .header("Authorization", AUTH)
                        .header("X-Tenant-Id", TENANT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(String.format("""
                                {
                                  "accountName": "漏斗测试个微",
                                  "wechatId": "%s",
                                  "aochuangWechatAccountId": "%s",
                                  "aochuangAccountRefId": %d,
                                  "aochuangNickname": "Device"
                                }
                                """, wechatId, deviceId, aoAccountId)))
                .andExpect(jsonPath("$.code").value(0))
                .andReturn();
        return JsonPath.parse(createResult.getResponse().getContentAsString()).read("$.data", Long.class);
    }

    private Long syncFriendsAndGetFirstFriendId(Long personalId) throws Exception {
        mockMvc.perform(post(PERSONAL_BASE + "/" + personalId + "/sync-friends")
                        .header("Authorization", AUTH)
                        .header("X-Tenant-Id", TENANT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "fullSync": true
                                }
                                """))
                .andExpect(jsonPath("$.code").value(0));

        MvcResult listResult = mockMvc.perform(get(PERSONAL_BASE + "/" + personalId + "/friends")
                        .header("Authorization", AUTH)
                        .header("X-Tenant-Id", TENANT)
                        .param("pageNo", "1")
                        .param("pageSize", "10"))
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.list[0].id").exists())
                .andReturn();
        return JsonPath.parse(listResult.getResponse().getContentAsString())
                .read("$.data.list[0].id", Long.class);
    }

    @Test
    @DisplayName("M10-COL-S-04: 私域预置漏斗存在且含 4 步")
    void privateDomainFunnelSeeded() throws Exception {
        mockMvc.perform(get("/admin-api/oa/funnel/list")
                        .header("Authorization", AUTH)
                        .header("X-Tenant-Id", TENANT)
                        .param("pageSize", "50"))
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.list[?(@.id == 99401)].funnelType").value("PRIVATE_DOMAIN"));

        mockMvc.perform(get("/admin-api/oa/funnel/" + PRIVATE_DOMAIN_FUNNEL_ID + "/data")
                        .header("Authorization", AUTH)
                        .header("X-Tenant-Id", TENANT))
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.funnelId").value(PRIVATE_DOMAIN_FUNNEL_ID))
                .andExpect(jsonPath("$.data.steps.length()").value(4))
                .andExpect(jsonPath("$.data.steps[0].name").value("奥创好友"))
                .andExpect(jsonPath("$.data.steps[1].name").value("已通过桥接"));
    }

    @Test
    @DisplayName("M10-COL-S-04: 好友+已通过桥接驱动漏斗计数")
    void funnelCountsFromApprovedBridge() throws Exception {
        seedAoApi();
        Long aoAccountId = createAoAccount("acct-funnel-s04");
        Long personalId = createBoundPersonal(aoAccountId, "wx_funnel_s04", "stub-device-funnel-s04");
        Long friendId = syncFriendsAndGetFirstFriendId(personalId);

        mockMvc.perform(post(BRIDGE_BASE + "/create")
                        .header("Authorization", AUTH)
                        .header("X-Tenant-Id", TENANT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(String.format("""
                                {
                                  "sourceType": "AOCHUANG_FRIEND",
                                  "sourceId": %d,
                                  "targetType": "PHONE",
                                  "targetId": 9001,
                                  "matchMethod": "MANUAL",
                                  "confidence": 100
                                }
                                """, friendId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));

        MvcResult pageResult = mockMvc.perform(get(BRIDGE_BASE + "/page")
                        .header("Authorization", AUTH)
                        .header("X-Tenant-Id", TENANT)
                        .param("reviewStatus", "PENDING"))
                .andExpect(jsonPath("$.code").value(0))
                .andReturn();
        Long bridgeId = JsonPath.parse(pageResult.getResponse().getContentAsString())
                .read("$.data.list[0].id", Long.class);

        mockMvc.perform(post(BRIDGE_BASE + "/" + bridgeId + "/confirm")
                        .header("Authorization", AUTH)
                        .header("X-Tenant-Id", TENANT))
                .andExpect(jsonPath("$.code").value(0));

        mockMvc.perform(get("/admin-api/oa/funnel/" + PRIVATE_DOMAIN_FUNNEL_ID + "/data")
                        .header("Authorization", AUTH)
                        .header("X-Tenant-Id", TENANT))
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.steps[0].count").value(greaterThanOrEqualTo(1)))
                .andExpect(jsonPath("$.data.steps[1].count").value(greaterThanOrEqualTo(1)))
                .andExpect(jsonPath("$.data.steps[2].count").value(greaterThanOrEqualTo(1)))
                .andExpect(jsonPath("$.data.steps[0].conversionRate").value(100.0));
    }

    @Test
    @DisplayName("M10-COL-S-04: 手机号规则自动桥接计入漏斗")
    void funnelCountsPhoneRuleAutoBridge() throws Exception {
        mockMvc.perform(post("/admin-api/oa/phone/create")
                        .header("Authorization", AUTH)
                        .header("X-Tenant-Id", TENANT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "phoneNumber": "13800138404",
                                  "phoneCode": "FUNNEL-S04",
                                  "keeperId": 1001,
                                  "status": "ENABLED"
                                }
                                """))
                .andExpect(jsonPath("$.code").value(0));

        seedAoApi();
        Long aoAccountId = createAoAccount("acct-funnel-s04b");
        Long personalId = createBoundPersonal(aoAccountId, "wx_funnel_s04b", "stub-device-funnel-s04b");

        mockMvc.perform(post(PERSONAL_BASE + "/" + personalId + "/sync-friends")
                        .header("Authorization", AUTH)
                        .header("X-Tenant-Id", TENANT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "fullSync": true
                                }
                                """))
                .andExpect(jsonPath("$.code").value(0));

        mockMvc.perform(get("/admin-api/oa/funnel/" + PRIVATE_DOMAIN_FUNNEL_ID + "/data")
                        .header("Authorization", AUTH)
                        .header("X-Tenant-Id", TENANT))
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.steps[0].count").value(greaterThanOrEqualTo(1)))
                .andExpect(jsonPath("$.data.steps[1].count").value(greaterThanOrEqualTo(1)))
                .andExpect(jsonPath("$.data.steps[2].count").value(greaterThanOrEqualTo(1)));
    }
}
