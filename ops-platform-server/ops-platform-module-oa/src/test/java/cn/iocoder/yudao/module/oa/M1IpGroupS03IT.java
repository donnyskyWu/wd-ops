package cn.iocoder.yudao.module.oa;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

@AutoConfigureMockMvc
class M1IpGroupS03IT extends OaITBase {

    private static final String ADMIN = "Bearer dev-token-oa-admin";
    private static final String TENANT = "1";

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("M1-S-03: 账号绑定到小组 + 解绑")
    void bindAndUnbindAccount() throws Exception {
        mockMvc.perform(delete("/admin-api/oa/ip-group/9002/accounts/9006")
                        .header("Authorization", ADMIN)
                        .header("X-Tenant-Id", TENANT))
                .andExpect(jsonPath("$.code").value(0));

        mockMvc.perform(post("/admin-api/oa/ip-group/9001/accounts")
                        .header("Authorization", ADMIN)
                        .header("X-Tenant-Id", TENANT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "accountIds": [9006],
                                  "accountRole": "PRIMARY"
                                }
                                """))
                .andExpect(jsonPath("$.code").value(0));

        mockMvc.perform(get("/admin-api/oa/ip-group/9001/accounts")
                        .header("Authorization", ADMIN)
                        .header("X-Tenant-Id", TENANT))
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data[?(@.accountId == 9006)]").exists());

        mockMvc.perform(delete("/admin-api/oa/ip-group/9001/accounts/9006")
                        .header("Authorization", ADMIN)
                        .header("X-Tenant-Id", TENANT))
                .andExpect(jsonPath("$.code").value(0));
    }

    @Test
    @DisplayName("M1-S-03: 账号已属其他 IP 组 (1007)")
    void accountAlreadyBound() throws Exception {
        mockMvc.perform(post("/admin-api/oa/ip-group/9002/accounts")
                        .header("Authorization", ADMIN)
                        .header("X-Tenant-Id", TENANT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "accountIds": [9001],
                                  "accountRole": "PRIMARY"
                                }
                                """))
                .andExpect(jsonPath("$.code").value(1007));
    }

    @Test
    @DisplayName("M1-S-03: 大组不可绑定账号 (1008)")
    void bindToBigGroup() throws Exception {
        mockMvc.perform(post("/admin-api/oa/ip-group/9000/accounts")
                        .header("Authorization", ADMIN)
                        .header("X-Tenant-Id", TENANT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "accountIds": [9006],
                                  "accountRole": "PRIMARY"
                                }
                                """))
                .andExpect(jsonPath("$.code").value(1008));
    }

    @Test
    @DisplayName("M1-S-03: 主播绑定与列表")
    void bindAndListAnchors() throws Exception {
        mockMvc.perform(post("/admin-api/oa/ip-group/9002/anchors")
                        .header("Authorization", ADMIN)
                        .header("X-Tenant-Id", TENANT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "anchorUserIds": [1003],
                                  "anchorType": "VIDEO"
                                }
                                """))
                .andExpect(jsonPath("$.code").value(0));

        MvcResult result = mockMvc.perform(get("/admin-api/oa/ip-group/9002/anchors")
                        .header("Authorization", ADMIN)
                        .header("X-Tenant-Id", TENANT))
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data[?(@.anchorUserId == 1003)]").exists())
                .andReturn();

        mockMvc.perform(delete("/admin-api/oa/ip-group/9002/anchors/1003")
                        .header("Authorization", ADMIN)
                        .header("X-Tenant-Id", TENANT))
                .andExpect(jsonPath("$.code").value(0));

        JsonPath.parse(result.getResponse().getContentAsString());
    }
}
