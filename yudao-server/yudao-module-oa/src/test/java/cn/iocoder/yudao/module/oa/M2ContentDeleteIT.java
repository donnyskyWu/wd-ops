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

/**
 * S-R22-Mike / D-7：生产内容 delete 端点
 */
@AutoConfigureMockMvc
class M2ContentDeleteIT extends OaITBase {

    private static final String ADMIN = "Bearer dev-token-oa-admin";
    private static final String OPERATOR = "Bearer dev-token-oa-operator";
    private static final String LEADER = "Bearer dev-token-oa-leader";
    private static final String TENANT = "1";

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("S-R22: 新建 DRAFT 内容可删除")
    void deleteDraftContent() throws Exception {
        MvcResult createResult = mockMvc.perform(post("/admin-api/oa/content/create")
                        .header("Authorization", OPERATOR)
                        .header("X-Tenant-Id", TENANT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "IT-待删除草稿",
                                  "contentType": "SHORT_VIDEO",
                                  "platformType": "DOUYIN",
                                  "accountId": 9006,
                                  "creatorUserId": 1003,
                                  "body": "删除测试正文",
                                  "aiGenerated": 0
                                }
                                """))
                .andExpect(jsonPath("$.code").value(0))
                .andReturn();

        Long contentId = JsonPath.parse(createResult.getResponse().getContentAsString()).read("$.data", Long.class);

        mockMvc.perform(delete("/admin-api/oa/content/" + contentId)
                        .header("Authorization", ADMIN)
                        .header("X-Tenant-Id", TENANT))
                .andExpect(jsonPath("$.code").value(0));

        mockMvc.perform(get("/admin-api/oa/content/list")
                        .param("title", "IT-待删除草稿")
                        .header("Authorization", ADMIN)
                        .header("X-Tenant-Id", TENANT))
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.total").value(0));
    }

    @Test
    @DisplayName("S-R22: REJECTED 内容可删除")
    void deleteRejectedContent() throws Exception {
        MvcResult createResult = mockMvc.perform(post("/admin-api/oa/content/create")
                        .header("Authorization", OPERATOR)
                        .header("X-Tenant-Id", TENANT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "IT-待删除驳回稿",
                                  "contentType": "ARTICLE",
                                  "platformType": "WECHAT_OFFICIAL",
                                  "accountId": 9003,
                                  "creatorUserId": 1003,
                                  "body": "驳回后删除",
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
                                {
                                  "action": "REJECT",
                                  "stage": "FIRST_REVIEW",
                                  "comment": "驳回"
                                }
                                """))
                .andExpect(jsonPath("$.code").value(0));

        mockMvc.perform(delete("/admin-api/oa/content/" + contentId)
                        .header("Authorization", ADMIN)
                        .header("X-Tenant-Id", TENANT))
                .andExpect(jsonPath("$.code").value(0));
    }

    @Test
    @DisplayName("S-R22: PUBLISHED 内容不可删除 (2010)")
    void deletePublishedForbidden() throws Exception {
        mockMvc.perform(delete("/admin-api/oa/content/9434")
                        .header("Authorization", ADMIN)
                        .header("X-Tenant-Id", TENANT))
                .andExpect(jsonPath("$.code").value(2010));
    }
}
