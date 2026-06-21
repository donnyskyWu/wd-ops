package cn.iocoder.yudao.module.oa;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
class M10AoPrivateDomainBridgeS07IT extends OaITBase {

    private static final String AUTH = "Bearer dev-token-oa-admin";
    private static final String TENANT = "1";
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
                                  "accountName": "桥接测试-%s",
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
                                  "accountName": "桥接测试个微",
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
    @DisplayName("M10-AO-S-07: 人工创建桥接 + 待审核列表 + 确认")
    void manualBridgeCreateAndConfirm() throws Exception {
        seedAoApi();
        Long aoAccountId = createAoAccount("acct-bridge-s07a");
        Long personalId = createBoundPersonal(aoAccountId, "wx_bridge_s07a", "stub-device-bridge-a");
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

        mockMvc.perform(get(BRIDGE_BASE + "/page")
                        .header("Authorization", AUTH)
                        .header("X-Tenant-Id", TENANT)
                        .param("reviewStatus", "PENDING")
                        .param("sourceType", "AOCHUANG_FRIEND"))
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.list[0].reviewStatus").value("PENDING"))
                .andExpect(jsonPath("$.data.list[0].matchMethod").value("MANUAL"));

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

        mockMvc.perform(get(BRIDGE_BASE + "/" + bridgeId)
                        .header("Authorization", AUTH)
                        .header("X-Tenant-Id", TENANT))
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.reviewStatus").value("APPROVED"))
                .andExpect(jsonPath("$.data.linkedBy").exists());
    }

    @Test
    @DisplayName("M10-AO-S-07: 驳回待审核桥接")
    void rejectPendingBridge() throws Exception {
        seedAoApi();
        Long aoAccountId = createAoAccount("acct-bridge-s07b");
        Long personalId = createBoundPersonal(aoAccountId, "wx_bridge_s07b", "stub-device-bridge-b");
        Long friendId = syncFriendsAndGetFirstFriendId(personalId);

        MvcResult createResult = mockMvc.perform(post(BRIDGE_BASE + "/create")
                        .header("Authorization", AUTH)
                        .header("X-Tenant-Id", TENANT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(String.format("""
                                {
                                  "sourceType": "AOCHUANG_FRIEND",
                                  "sourceId": %d,
                                  "targetType": "PHONE",
                                  "targetId": 9002,
                                  "matchMethod": "MANUAL"
                                }
                                """, friendId)))
                .andExpect(jsonPath("$.code").value(0))
                .andReturn();
        Long bridgeId = JsonPath.parse(createResult.getResponse().getContentAsString()).read("$.data", Long.class);

        mockMvc.perform(post(BRIDGE_BASE + "/" + bridgeId + "/reject")
                        .header("Authorization", AUTH)
                        .header("X-Tenant-Id", TENANT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "reason": "误匹配"
                                }
                                """))
                .andExpect(jsonPath("$.code").value(0));

        mockMvc.perform(get(BRIDGE_BASE + "/" + bridgeId)
                        .header("Authorization", AUTH)
                        .header("X-Tenant-Id", TENANT))
                .andExpect(jsonPath("$.data.reviewStatus").value("REJECTED"));
    }

    @Test
    @DisplayName("M10-AO-S-07: 手机号规则自动桥接（备注含手机号）")
    void phoneRuleAutoBridgeFromFriendRemark() throws Exception {
        mockMvc.perform(post("/admin-api/oa/phone/create")
                        .header("Authorization", AUTH)
                        .header("X-Tenant-Id", TENANT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "phoneNumber": "13800138707",
                                  "phoneCode": "BRIDGE-S07",
                                  "keeperId": 1001,
                                  "status": "ENABLED"
                                }
                                """))
                .andExpect(jsonPath("$.code").value(0));

        seedAoApi();
        Long aoAccountId = createAoAccount("acct-bridge-s07c");
        Long personalId = createBoundPersonal(aoAccountId, "wx_bridge_s07c", "stub-device-bridge-c");

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

        mockMvc.perform(get(BRIDGE_BASE + "/page")
                        .header("Authorization", AUTH)
                        .header("X-Tenant-Id", TENANT)
                        .param("matchMethod", "PHONE")
                        .param("reviewStatus", "APPROVED"))
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.list[0].matchMethod").value("PHONE"))
                .andExpect(jsonPath("$.data.list[0].reviewStatus").value("APPROVED"))
                .andExpect(jsonPath("$.data.list[0].confidence").value(95.0));
    }
}
