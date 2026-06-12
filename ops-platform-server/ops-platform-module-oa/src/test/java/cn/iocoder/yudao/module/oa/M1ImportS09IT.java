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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

@AutoConfigureMockMvc
class M1ImportS09IT extends OaITBase {

    private static final String ADMIN = "Bearer dev-token-oa-admin";
    private static final String TENANT = "1";

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("M1-S-09: 补录提交与审核")
    void submitAndReviewImport() throws Exception {
        MvcResult submitResult = mockMvc.perform(post("/admin-api/oa/internal-content/import")
                        .header("Authorization", ADMIN)
                        .header("X-Tenant-Id", TENANT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "contentId": 9302,
                                  "statDate": "2026-06-01",
                                  "importType": "API_EXCEPTION",
                                  "readCount": 8000,
                                  "likeCount": 200,
                                  "commentCount": 50,
                                  "forwardCount": 30,
                                  "remark": "IT-补录测试"
                                }
                                """))
                .andExpect(jsonPath("$.code").value(0))
                .andReturn();

        Long importId = JsonPath.parse(submitResult.getResponse().getContentAsString()).read("$.data", Long.class);

        mockMvc.perform(get("/admin-api/oa/internal-content/import/list")
                        .header("Authorization", ADMIN)
                        .header("X-Tenant-Id", TENANT)
                        .param("reviewStatus", "0"))
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.list[?(@.id == " + importId + ")].reviewStatus").value(0));

        mockMvc.perform(put("/admin-api/oa/internal-content/import/" + importId + "/review")
                        .header("Authorization", ADMIN)
                        .header("X-Tenant-Id", TENANT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "reviewStatus": 1,
                                  "remark": "审核通过"
                                }
                                """))
                .andExpect(jsonPath("$.code").value(0));
    }

    @Test
    @DisplayName("M1-S-09: 超过 90 天补录 (1301)")
    void importDateTooOld() throws Exception {
        mockMvc.perform(post("/admin-api/oa/internal-content/import")
                        .header("Authorization", ADMIN)
                        .header("X-Tenant-Id", TENANT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "contentId": 9302,
                                  "statDate": "2025-01-01",
                                  "importType": "API_EXCEPTION",
                                  "readCount": 100
                                }
                                """))
                .andExpect(jsonPath("$.code").value(1301));
    }
}
