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
 * 内容审核通过后，首页待办应即时消失（缓存失效 + 状态聚合）。
 */
@AutoConfigureMockMvc
class M0TodoContentReviewIT extends OaITBase {

    private static final String ADMIN = "Bearer dev-token-oa-admin";
    private static final String OPERATOR = "Bearer dev-token-oa-operator";
    private static final String LEADER = "Bearer dev-token-oa-leader";
    private static final String TENANT = "1";
    private static final String UNIQUE_TITLE = "IT-待办审核-" + System.currentTimeMillis();

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("M0: 内容二级审核通过后待办消失")
    void contentTodoRemovedAfterFullApprove() throws Exception {
        Long contentId = createAndSubmitContent();

        mockMvc.perform(get("/admin-api/oa/dashboard/home/todos")
                        .header("Authorization", ADMIN)
                        .header("X-Tenant-Id", TENANT))
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data[?(@.title =~ /.*" + UNIQUE_TITLE + ".*/)]").exists());

        approveFirstReview(contentId);
        approveSecondReview(contentId);

        mockMvc.perform(get("/admin-api/oa/content/" + contentId)
                        .header("Authorization", ADMIN)
                        .header("X-Tenant-Id", TENANT))
                .andExpect(jsonPath("$.data.status").value("PENDING_PUBLISH"));

        mockMvc.perform(get("/admin-api/oa/dashboard/home/todos")
                        .header("Authorization", ADMIN)
                        .header("X-Tenant-Id", TENANT))
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data[?(@.title =~ /.*" + UNIQUE_TITLE + ".*/)]").doesNotExist());
    }

    private Long createAndSubmitContent() throws Exception {
        MvcResult createResult = mockMvc.perform(post("/admin-api/oa/content/create")
                        .header("Authorization", OPERATOR)
                        .header("X-Tenant-Id", TENANT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "%s",
                                  "contentType": "SHORT_VIDEO",
                                  "platformType": "DOUYIN",
                                  "accountId": 9006,
                                  "creatorUserId": 1003,
                                  "ipGroupId": 9002,
                                  "body": "IT 正文",
                                  "aiGenerated": 0
                                }
                                """.formatted(UNIQUE_TITLE)))
                .andExpect(jsonPath("$.code").value(0))
                .andReturn();
        Long contentId = JsonPath.parse(createResult.getResponse().getContentAsString()).read("$.data", Long.class);

        mockMvc.perform(post("/admin-api/oa/content/" + contentId + "/submit-review")
                        .header("Authorization", OPERATOR)
                        .header("X-Tenant-Id", TENANT))
                .andExpect(jsonPath("$.code").value(0));
        return contentId;
    }

    private void approveFirstReview(Long contentId) throws Exception {
        mockMvc.perform(post("/admin-api/oa/content/" + contentId + "/review")
                        .header("Authorization", LEADER)
                        .header("X-Tenant-Id", TENANT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"action": "APPROVE", "stage": "FIRST_REVIEW", "comment": "ok"}
                                """))
                .andExpect(jsonPath("$.code").value(0));
    }

    private void approveSecondReview(Long contentId) throws Exception {
        mockMvc.perform(post("/admin-api/oa/content/" + contentId + "/review")
                        .header("Authorization", ADMIN)
                        .header("X-Tenant-Id", TENANT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"action": "APPROVE", "stage": "SECOND_REVIEW", "comment": "ok"}
                                """))
                .andExpect(jsonPath("$.code").value(0));
    }
}
