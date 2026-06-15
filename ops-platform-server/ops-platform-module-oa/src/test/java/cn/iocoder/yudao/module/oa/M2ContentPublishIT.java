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

/**
 * ADR-022：审核通过 → 待发布 → 手动发布 → 已发布
 */
@AutoConfigureMockMvc
class M2ContentPublishIT extends OaITBase {

    private static final String ADMIN = "Bearer dev-token-oa-admin";
    private static final String OPERATOR = "Bearer dev-token-oa-operator";
    private static final String LEADER = "Bearer dev-token-oa-leader";
    private static final String TENANT = "1";

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("ADR-022: 二级审核通过 → 待发布 → 发布 → 已发布")
    void publishAfterSecondReview() throws Exception {
        MvcResult createResult = mockMvc.perform(post("/admin-api/oa/content/create")
                        .header("Authorization", OPERATOR)
                        .header("X-Tenant-Id", TENANT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "IT-发布流程",
                                  "contentType": "SHORT_VIDEO",
                                  "platformType": "DOUYIN",
                                  "accountId": 9006,
                                  "creatorUserId": 1003,
                                  "ipGroupId": 9002,
                                  "body": "IT 正文",
                                  "aiGenerated": 0
                                }
                                """))
                .andExpect(jsonPath("$.code").value(0))
                .andReturn();
        Long contentId = JsonPath.parse(createResult.getResponse().getContentAsString()).read("$.data", Long.class);

        mockMvc.perform(post("/admin-api/oa/content/" + contentId + "/submit-review")
                        .header("Authorization", OPERATOR)
                        .header("X-Tenant-Id", TENANT))
                .andExpect(jsonPath("$.code").value(0));

        mockMvc.perform(post("/admin-api/oa/content/" + contentId + "/review")
                        .header("Authorization", LEADER)
                        .header("X-Tenant-Id", TENANT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"action": "APPROVE", "stage": "FIRST_REVIEW", "comment": "ok"}
                                """))
                .andExpect(jsonPath("$.code").value(0));

        mockMvc.perform(get("/admin-api/oa/content/" + contentId)
                        .header("Authorization", ADMIN)
                        .header("X-Tenant-Id", TENANT))
                .andExpect(jsonPath("$.data.status").value("PENDING_SECOND_REVIEW"));

        mockMvc.perform(post("/admin-api/oa/content/" + contentId + "/review")
                        .header("Authorization", ADMIN)
                        .header("X-Tenant-Id", TENANT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"action": "APPROVE", "stage": "SECOND_REVIEW", "comment": "ok"}
                                """))
                .andExpect(jsonPath("$.code").value(0));

        mockMvc.perform(get("/admin-api/oa/content/" + contentId)
                        .header("Authorization", ADMIN)
                        .header("X-Tenant-Id", TENANT))
                .andExpect(jsonPath("$.data.status").value("PENDING_PUBLISH"));

        mockMvc.perform(get("/admin-api/oa/content/" + contentId + "/publish-options")
                        .header("Authorization", ADMIN)
                        .header("X-Tenant-Id", TENANT))
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.platforms").isArray());

        mockMvc.perform(post("/admin-api/oa/content/" + contentId + "/publish")
                        .header("Authorization", ADMIN)
                        .header("X-Tenant-Id", TENANT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"platformType": "DOUYIN", "accountIds": [9006]}
                                """))
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.status").value("PUBLISHED"))
                .andExpect(jsonPath("$.data.mock").value(true));

        mockMvc.perform(get("/admin-api/oa/content/" + contentId)
                        .header("Authorization", ADMIN)
                        .header("X-Tenant-Id", TENANT))
                .andExpect(jsonPath("$.data.status").value("PUBLISHED"));
    }
}
