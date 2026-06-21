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
class M10AoFriendSyncS04IT extends OaITBase {

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
                                  "accountName": "好友同步-%s",
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
                                  "accountName": "好友同步个微",
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
    @DisplayName("M10-AO-S-04: 好友全量同步 + 列表查询")
    void syncFriendsFullAndList() throws Exception {
        seedAoApi();
        Long aoAccountId = createAoAccount("acct-friend-full");
        Long personalId = createBoundPersonal(aoAccountId, "wx_friend_full", "stub-device-friend-full");

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
                .andExpect(jsonPath("$.data.syncedCount").value(2))
                .andExpect(jsonPath("$.data.createdCount").value(2))
                .andExpect(jsonPath("$.data.completed").value(true));

        mockMvc.perform(get(BASE + "/" + personalId + "/friends")
                        .header("Authorization", AUTH)
                        .header("X-Tenant-Id", TENANT))
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.total").value(2))
                .andExpect(jsonPath("$.data.list[0].aochuangWechatAccountId").value("stub-device-friend-full"))
                .andExpect(jsonPath("$.data.list[0].nickname").exists());

        mockMvc.perform(get(BASE + "/" + personalId)
                        .header("Authorization", AUTH)
                        .header("X-Tenant-Id", TENANT))
                .andExpect(jsonPath("$.data.lastFriendSyncAt").exists());
    }

    @Test
    @DisplayName("M10-AO-S-04: 游标分页增量同步")
    void syncFriendsPagedIncremental() throws Exception {
        seedAoApi();
        Long aoAccountId = createAoAccount("acct-friend-paged");
        Long personalId = createBoundPersonal(aoAccountId, "wx_friend_paged", "stub-device-paged");

        mockMvc.perform(post(BASE + "/" + personalId + "/sync-friends")
                        .header("Authorization", AUTH)
                        .header("X-Tenant-Id", TENANT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.syncedCount").value(3))
                .andExpect(jsonPath("$.data.createdCount").value(3))
                .andExpect(jsonPath("$.data.completed").value(true));

        mockMvc.perform(get(BASE + "/" + personalId + "/friends")
                        .header("Authorization", AUTH)
                        .header("X-Tenant-Id", TENANT)
                        .param("pageSize", "10"))
                .andExpect(jsonPath("$.data.total").value(3));

        mockMvc.perform(post(BASE + "/" + personalId + "/sync-friends")
                        .header("Authorization", AUTH)
                        .header("X-Tenant-Id", TENANT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.syncedCount").value(3))
                .andExpect(jsonPath("$.data.createdCount").value(0))
                .andExpect(jsonPath("$.data.updatedCount").value(3));
    }

    @Test
    @DisplayName("M10-AO-S-04: 未绑定设备拒绝同步")
    void syncFriendsRequiresBoundDevice() throws Exception {
        seedAoApi();
        MvcResult createResult = mockMvc.perform(post(BASE + "/create")
                        .header("Authorization", AUTH)
                        .header("X-Tenant-Id", TENANT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "accountName": "未绑定",
                                  "wechatId": "wx_unbound_friend",
                                  "status": "ENABLED"
                                }
                                """))
                .andExpect(jsonPath("$.code").value(0))
                .andReturn();
        Long personalId = JsonPath.parse(createResult.getResponse().getContentAsString()).read("$.data", Long.class);

        mockMvc.perform(post(BASE + "/" + personalId + "/sync-friends")
                        .header("Authorization", AUTH)
                        .header("X-Tenant-Id", TENANT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(jsonPath("$.code").value(1400));
    }
}
