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
class M10AoDeviceSyncS03IT extends OaITBase {

    private static final String AUTH = "Bearer dev-token-oa-admin";
    private static final String TENANT = "1";
    private static final String BASE = "/admin-api/oa/internal/personal-account";

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
                                  "accountName": "同步测试-%s",
                                  "aochuangAccountId": "%s",
                                  "status": "ENABLED"
                                }
                                """, aochuangAccountId, aochuangAccountId)))
                .andExpect(jsonPath("$.code").value(0))
                .andReturn();
        return JsonPath.parse(aoResult.getResponse().getContentAsString()).read("$.data", Long.class);
    }

    @Test
    @DisplayName("M10-AO-S-03: 设备同步自动绑定 + 待绑定列表")
    void syncDevicesAutoBindAndPending() throws Exception {
        seedAoApi();
        Long aoAccountId = createAoAccount("acct-sync-testa");

        mockMvc.perform(post(BASE + "/create")
                        .header("Authorization", AUTH)
                        .header("X-Tenant-Id", TENANT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "accountName": "可自动匹配",
                                  "wechatId": "wx_auto_matcha",
                                  "status": "ENABLED"
                                }
                                """))
                .andExpect(jsonPath("$.code").value(0));

        mockMvc.perform(post(BASE + "/sync-devices")
                        .header("Authorization", AUTH)
                        .header("X-Tenant-Id", TENANT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(String.format("""
                                {
                                  "aoCreateAccountId": %d
                                }
                                """, aoAccountId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.autoBoundCount").value(1))
                .andExpect(jsonPath("$.data.pendingCount").value(1))
                .andExpect(jsonPath("$.data.pendingDevices[0].aochuangWechatAccountId").value("stub-device-pendinga"));

        mockMvc.perform(get(BASE + "/list")
                        .header("Authorization", AUTH)
                        .header("X-Tenant-Id", TENANT)
                        .param("wechatId", "wx_auto_matcha"))
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.list[0].aochuangBindStatus").value("AUTO"))
                .andExpect(jsonPath("$.data.list[0].aochuangWechatAccountId").value("stub-device-autoa"));
    }

    @Test
    @DisplayName("M10-AO-S-03: 手工绑定待绑定设备")
    void bindPendingDeviceManually() throws Exception {
        seedAoApi();
        Long aoAccountId = createAoAccount("acct-sync-testb");

        mockMvc.perform(post(BASE + "/create")
                        .header("Authorization", AUTH)
                        .header("X-Tenant-Id", TENANT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "accountName": "可自动匹配B",
                                  "wechatId": "wx_auto_matchb",
                                  "status": "ENABLED"
                                }
                                """))
                .andExpect(jsonPath("$.code").value(0));

        mockMvc.perform(post(BASE + "/sync-devices")
                        .header("Authorization", AUTH)
                        .header("X-Tenant-Id", TENANT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(String.format("""
                                {
                                  "aoCreateAccountId": %d
                                }
                                """, aoAccountId)))
                .andExpect(jsonPath("$.code").value(0));

        MvcResult createResult = mockMvc.perform(post(BASE + "/create")
                        .header("Authorization", AUTH)
                        .header("X-Tenant-Id", TENANT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "accountName": "手工绑定目标",
                                  "wechatId": "wx_manual_bind",
                                  "status": "ENABLED"
                                }
                                """))
                .andExpect(jsonPath("$.code").value(0))
                .andReturn();
        Long personalId = JsonPath.parse(createResult.getResponse().getContentAsString()).read("$.data", Long.class);

        mockMvc.perform(post(BASE + "/" + personalId + "/bind-device")
                        .header("Authorization", AUTH)
                        .header("X-Tenant-Id", TENANT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(String.format("""
                                {
                                  "aochuangWechatAccountId": "stub-device-pendingb",
                                  "aochuangAccountRefId": %d,
                                  "bindStatus": "MANUAL",
                                  "aochuangNickname": "Pending Device"
                                }
                                """, aoAccountId)))
                .andExpect(jsonPath("$.code").value(0));

        mockMvc.perform(get(BASE + "/" + personalId)
                        .header("Authorization", AUTH)
                        .header("X-Tenant-Id", TENANT))
                .andExpect(jsonPath("$.data.aochuangBindStatus").value("MANUAL"))
                .andExpect(jsonPath("$.data.aochuangWechatAccountId").value("stub-device-pendingb"));
    }

    @Test
    @DisplayName("M10-AO-S-03: 新建并绑定待绑定设备")
    void createAndBindPendingDevice() throws Exception {
        seedAoApi();
        Long aoAccountId = createAoAccount("acct-sync-testc");

        mockMvc.perform(post(BASE + "/create")
                        .header("Authorization", AUTH)
                        .header("X-Tenant-Id", TENANT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "accountName": "可自动匹配C",
                                  "wechatId": "wx_auto_matchc",
                                  "status": "ENABLED"
                                }
                                """))
                .andExpect(jsonPath("$.code").value(0));

        mockMvc.perform(post(BASE + "/sync-devices")
                        .header("Authorization", AUTH)
                        .header("X-Tenant-Id", TENANT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(String.format("""
                                {
                                  "aoCreateAccountId": %d
                                }
                                """, aoAccountId)))
                .andExpect(jsonPath("$.code").value(0));

        mockMvc.perform(post(BASE + "/create-and-bind")
                        .header("Authorization", AUTH)
                        .header("X-Tenant-Id", TENANT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(String.format("""
                                {
                                  "accountName": "新建绑定个微",
                                  "wechatId": "wx_new_bindc",
                                  "aochuangWechatAccountId": "stub-device-pendingc",
                                  "aochuangAccountRefId": %d,
                                  "aochuangNickname": "Pending Device"
                                }
                                """, aoAccountId)))
                .andExpect(jsonPath("$.code").value(0));

        mockMvc.perform(get(BASE + "/list")
                        .header("Authorization", AUTH)
                        .header("X-Tenant-Id", TENANT)
                        .param("wechatId", "wx_new_bindc"))
                .andExpect(jsonPath("$.data.list[0].aochuangBindStatus").value("MANUAL"))
                .andExpect(jsonPath("$.data.list[0].aochuangWechatAccountId").value("stub-device-pendingc"));
    }
}
