package cn.iocoder.yudao.module.oa;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
class M4TripleRelS09IT extends OaITBase {

    private static final String AUTH = "Bearer dev-token-oa-admin";
    private static final String TENANT = "1";
    private static final String BASE = "/admin-api/oa/internal/triple-rel";
    private static final String PERSONAL = "/admin-api/oa/internal/personal-account";
    private static final String WEWORK = "/admin-api/oa/internal/wework";

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("S-09: 创建三方关联 + 图谱查询 + 解绑")
    void createGraphUnbind() throws Exception {
        String wxResp = mockMvc.perform(post(PERSONAL + "/create")
                        .header("Authorization", AUTH)
                        .header("X-Tenant-Id", TENANT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"accountName":"图谱个微","wechatId":"graph_wx_s09","status":"ENABLED"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andReturn().getResponse().getContentAsString();
        long wxId = com.jayway.jsonpath.JsonPath.read(wxResp, "$.data").toString().contains(".")
                ? ((Number) com.jayway.jsonpath.JsonPath.read(wxResp, "$.data")).longValue()
                : Long.parseLong(com.jayway.jsonpath.JsonPath.read(wxResp, "$.data").toString());

        mockMvc.perform(post(WEWORK + "/create")
                        .header("Authorization", AUTH)
                        .header("X-Tenant-Id", TENANT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "accountName": "图谱企微",
                                  "corpId": "corp_s09",
                                  "agentId": "agent_s09",
                                  "secret": "sec",
                                  "status": "ENABLED"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));

        String wwList = mockMvc.perform(get(WEWORK + "/list")
                        .header("Authorization", AUTH)
                        .header("X-Tenant-Id", TENANT)
                        .param("corpId", "corp_s09"))
                .andReturn().getResponse().getContentAsString();
        long wwId = ((Number) com.jayway.jsonpath.JsonPath.read(wwList, "$.data.list[0].id")).longValue();

        String createRel = mockMvc.perform(post(BASE + "/create")
                        .header("Authorization", AUTH)
                        .header("X-Tenant-Id", TENANT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(String.format("""
                                {
                                  "personalWechatId": %d,
                                  "weworkAccountId": %d,
                                  "relationType": "WECHAT_WEWORK"
                                }
                                """, wxId, wwId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andReturn().getResponse().getContentAsString();
        long relId = ((Number) com.jayway.jsonpath.JsonPath.read(createRel, "$.data")).longValue();

        mockMvc.perform(get(BASE + "/graph")
                        .header("Authorization", AUTH)
                        .header("X-Tenant-Id", TENANT)
                        .param("personalWechatId", String.valueOf(wxId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.personalWechat.wechatId").value("graph_wx_s09"))
                .andExpect(jsonPath("$.data.weworkAccount.id").value((int) wwId));

        mockMvc.perform(get(BASE + "/list")
                        .header("Authorization", AUTH)
                        .header("X-Tenant-Id", TENANT))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(org.hamcrest.Matchers.greaterThanOrEqualTo(1)));

        mockMvc.perform(put(BASE + "/unbind")
                        .header("Authorization", AUTH)
                        .header("X-Tenant-Id", TENANT)
                        .param("id", String.valueOf(relId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));
    }
}
