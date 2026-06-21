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
class M10AoMessageSyncS05IT extends OaITBase {

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
                                  "accountName": "消息同步-%s",
                                  "aochuangAccountId": "%s",
                                  "status": "ENABLED"
                                }
                                """, aochuangAccountId, aochuangAccountId)))
                .andExpect(jsonPath("$.code").value(0))
                .andReturn();
        return JsonPath.parse(aoResult.getResponse().getContentAsString()).read("$.data", Long.class);
    }

    private Long createBoundPersonal(Long aoAccountId, String wechatId, String deviceId) throws Exception {
        MvcResult createResult = mockMvc.perform(post(BASE + "/create-and-bind")
                        .header("Authorization", AUTH)
                        .header("X-Tenant-Id", TENANT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(String.format("""
                                {
                                  "accountName": "消息同步个微",
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

    @Test
    @DisplayName("M10-AO-S-05: 消息全量同步 + 列表查询 + 日统计")
    void syncMessagesFullAndList() throws Exception {
        seedAoApi();
        Long aoAccountId = createAoAccount("acct-msg-full");
        Long personalId = createBoundPersonal(aoAccountId, "wx_msg_full", "stub-device-msg-full");

        mockMvc.perform(post(BASE + "/" + personalId + "/sync-messages")
                        .header("Authorization", AUTH)
                        .header("X-Tenant-Id", TENANT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "fullSync": true
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.syncedCount").value(2))
                .andExpect(jsonPath("$.data.createdCount").value(2))
                .andExpect(jsonPath("$.data.dailyStatsDays").value(1))
                .andExpect(jsonPath("$.data.completed").value(true));

        mockMvc.perform(get(BASE + "/" + personalId + "/messages")
                        .header("Authorization", AUTH)
                        .header("X-Tenant-Id", TENANT))
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.total").value(2))
                .andExpect(jsonPath("$.data.list[0].aochuangWechatAccountId").value("stub-device-msg-full"))
                .andExpect(jsonPath("$.data.list[0].direction").exists());

        mockMvc.perform(get(BASE + "/" + personalId)
                        .header("Authorization", AUTH)
                        .header("X-Tenant-Id", TENANT))
                .andExpect(jsonPath("$.data.lastMessageSyncAt").exists());
    }

    @Test
    @DisplayName("M10-AO-S-05: 游标分页增量同步")
    void syncMessagesPagedIncremental() throws Exception {
        seedAoApi();
        Long aoAccountId = createAoAccount("acct-msg-paged");
        Long personalId = createBoundPersonal(aoAccountId, "wx_msg_paged", "stub-device-msg-paged");

        mockMvc.perform(post(BASE + "/" + personalId + "/sync-messages")
                        .header("Authorization", AUTH)
                        .header("X-Tenant-Id", TENANT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.syncedCount").value(4))
                .andExpect(jsonPath("$.data.createdCount").value(4))
                .andExpect(jsonPath("$.data.dailyStatsDays").value(2))
                .andExpect(jsonPath("$.data.completed").value(true));

        mockMvc.perform(get(BASE + "/" + personalId + "/messages")
                        .header("Authorization", AUTH)
                        .header("X-Tenant-Id", TENANT)
                        .param("pageSize", "10"))
                .andExpect(jsonPath("$.data.total").value(4));

        mockMvc.perform(post(BASE + "/" + personalId + "/sync-messages")
                        .header("Authorization", AUTH)
                        .header("X-Tenant-Id", TENANT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.syncedCount").value(4))
                .andExpect(jsonPath("$.data.createdCount").value(0))
                .andExpect(jsonPath("$.data.updatedCount").value(4));
    }

    @Test
    @DisplayName("M10-AO-S-05: 未绑定设备拒绝同步")
    void syncMessagesRequiresBoundDevice() throws Exception {
        seedAoApi();
        MvcResult createResult = mockMvc.perform(post(BASE + "/create")
                        .header("Authorization", AUTH)
                        .header("X-Tenant-Id", TENANT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "accountName": "未绑定",
                                  "wechatId": "wx_unbound_msg",
                                  "status": "ENABLED"
                                }
                                """))
                .andExpect(jsonPath("$.code").value(0))
                .andReturn();
        Long personalId = JsonPath.parse(createResult.getResponse().getContentAsString()).read("$.data", Long.class);

        mockMvc.perform(post(BASE + "/" + personalId + "/sync-messages")
                        .header("Authorization", AUTH)
                        .header("X-Tenant-Id", TENANT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(jsonPath("$.code").value(1400));
    }
}
