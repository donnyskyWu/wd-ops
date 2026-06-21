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
class M10AoCollectStatusS06IT extends OaITBase {

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
                                  "accountName": "采集状态-%s",
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
                                  "accountName": "采集状态个微",
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
    @DisplayName("M10-AO-S-06: 新建个微默认 PENDING，同步后 SUCCESS + 日统计 + M1 列表")
    void collectStatusAndMetricsFlow() throws Exception {
        seedAoApi();
        Long aoAccountId = createAoAccount("acct-s06");
        Long personalId = createBoundPersonal(aoAccountId, "wx_s06", "stub-device-s06");

        mockMvc.perform(get(BASE + "/" + personalId)
                        .header("Authorization", AUTH)
                        .header("X-Tenant-Id", TENANT))
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.collectStatus").value("PENDING"));

        mockMvc.perform(post(BASE + "/" + personalId + "/sync-friends")
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
                .andExpect(jsonPath("$.data.completed").value(true));

        mockMvc.perform(get(BASE + "/" + personalId)
                        .header("Authorization", AUTH)
                        .header("X-Tenant-Id", TENANT))
                .andExpect(jsonPath("$.data.collectStatus").value("SUCCESS"));

        mockMvc.perform(post(BASE + "/" + personalId + "/sync-messages")
                        .header("Authorization", AUTH)
                        .header("X-Tenant-Id", TENANT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "fullSync": true
                                }
                                """))
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.dailyStatsDays").value(1));

        mockMvc.perform(get(BASE + "/" + personalId + "/daily-stats")
                        .header("Authorization", AUTH)
                        .header("X-Tenant-Id", TENANT))
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data[0].totalFriends").exists())
                .andExpect(jsonPath("$.data[0].messagesSent").exists())
                .andExpect(jsonPath("$.data[0].messagesReceived").exists());

        mockMvc.perform(get("/admin-api/oa/account-analysis/list")
                        .header("Authorization", AUTH)
                        .header("X-Tenant-Id", TENANT)
                        .param("platform", "WECHAT_PERSONAL")
                        .param("keyword", "wx_s06"))
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.list[0].accountId").value(personalId.intValue()))
                .andExpect(jsonPath("$.data.list[0].collectStatus").value("SUCCESS"))
                .andExpect(jsonPath("$.data.list[0].messagesSent").exists())
                .andExpect(jsonPath("$.data.list[0].followerCount").exists());
    }

    @Test
    @DisplayName("M10-AO-S-06: 同步失败时 collect_status=FAILED")
    void collectStatusFailedOnApiError() throws Exception {
        seedAoApi();
        Long aoAccountId = createAoAccount("acct-s06-fail");
        Long personalId = createBoundPersonal(aoAccountId, "wx_s06_fail", "stub-device-api-fail");

        mockMvc.perform(post(BASE + "/" + personalId + "/sync-friends")
                        .header("Authorization", AUTH)
                        .header("X-Tenant-Id", TENANT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(jsonPath("$.code").value(1400));

        mockMvc.perform(get(BASE + "/" + personalId)
                        .header("Authorization", AUTH)
                        .header("X-Tenant-Id", TENANT))
                .andExpect(jsonPath("$.data.collectStatus").value("FAILED"));
    }

    @Test
    @DisplayName("M10-AO-S-06: 采集任务 AOCHUANG_API 路由个微同步")
    void collectTaskRoutesToAochuangSync() throws Exception {
        seedAoApi();
        Long aoAccountId = createAoAccount("acct-s06-task");
        Long personalId = createBoundPersonal(aoAccountId, "wx_s06_task", "stub-device-s06-task");

        MvcResult taskResult = mockMvc.perform(post("/admin-api/oa/collect/task/create")
                        .header("Authorization", AUTH)
                        .header("X-Tenant-Id", TENANT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(String.format("""
                                {
                                  "name": "个微奥创采集",
                                  "platformType": "WECHAT_PERSONAL",
                                  "accountId": %d,
                                  "method": "INTERNAL",
                                  "source": "AOCHUANG_API",
                                  "frequency": "DAILY",
                                  "cron": "0 0 2 * * ?",
                                  "status": "PENDING"
                                }
                                """, personalId)))
                .andExpect(jsonPath("$.code").value(0))
                .andReturn();
        Long taskId = JsonPath.parse(taskResult.getResponse().getContentAsString()).read("$.data", Long.class);

        mockMvc.perform(post("/admin-api/oa/collect/task/" + taskId + "/run")
                        .header("Authorization", AUTH)
                        .header("X-Tenant-Id", TENANT))
                .andExpect(jsonPath("$.code").value(0));

        mockMvc.perform(get(BASE + "/" + personalId)
                        .header("Authorization", AUTH)
                        .header("X-Tenant-Id", TENANT))
                .andExpect(jsonPath("$.data.collectStatus").value("SUCCESS"))
                .andExpect(jsonPath("$.data.lastFriendSyncAt").exists())
                .andExpect(jsonPath("$.data.lastMessageSyncAt").exists());
    }
}
