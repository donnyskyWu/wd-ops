package cn.iocoder.yudao.module.oa;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

@AutoConfigureMockMvc
class M2ContentS06IT extends OaITBase {

    private static final String ADMIN = "Bearer dev-token-oa-admin";
    private static final String OPERATOR = "Bearer dev-token-oa-operator";
    private static final String LEADER = "Bearer dev-token-oa-leader";
    private static final String TENANT = "1";

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("M2-S-06: 创建内容 → 提交审核 → 初审通过")
    void contentReviewFlow() throws Exception {
        MvcResult createResult = mockMvc.perform(post("/admin-api/oa/content/create")
                        .header("Authorization", OPERATOR)
                        .header("X-Tenant-Id", TENANT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "IT-测试短视频",
                                  "contentType": "SHORT_VIDEO",
                                  "platformType": "DOUYIN",
                                  "accountId": 9006,
                                  "creatorUserId": 1003,
                                  "body": "IT 正文内容",
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
                                  "action": "APPROVE",
                                  "stage": "FIRST_REVIEW",
                                  "comment": "初审通过"
                                }
                                """))
                .andExpect(jsonPath("$.code").value(0));

        mockMvc.perform(get("/admin-api/oa/content/list")
                        .header("Authorization", ADMIN)
                        .header("X-Tenant-Id", TENANT)
                        .param("title", "IT-测试短视频"))
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.list[0].status").value("PENDING_SECOND_REVIEW"));
    }

    @Test
    @DisplayName("M2-S-06: 账号平台不匹配 (2006)")
    void platformMismatch() throws Exception {
        mockMvc.perform(post("/admin-api/oa/content/create")
                        .header("Authorization", ADMIN)
                        .header("X-Tenant-Id", TENANT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "IT-平台不匹配",
                                  "contentType": "SHORT_VIDEO",
                                  "platformType": "DOUYIN",
                                  "accountId": 9001,
                                  "creatorUserId": 1001,
                                  "body": "正文"
                                }
                                """))
                .andExpect(jsonPath("$.code").value(2022));
    }

    @Test
    @DisplayName("M2-S-06: SEED 生产内容多状态")
    void seedContentStatuses() throws Exception {
        mockMvc.perform(get("/admin-api/oa/content/list")
                        .header("Authorization", ADMIN)
                        .header("X-Tenant-Id", TENANT)
                        .param("pageSize", "50"))
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.total").value(greaterThanOrEqualTo(5)));
    }
}
