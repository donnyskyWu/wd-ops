package cn.iocoder.yudao.module.oa;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * S-R21-Mike / ADR-008：oa_content.author_id 回填后人效内容 KPI 可聚合
 */
@AutoConfigureMockMvc
class M1ContentAuthorIdIT extends OaITBase {

    private static final String ADMIN = "Bearer dev-token-oa-admin";
    private static final String TENANT = "1";

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("S-R21: user 1003 聚合到 seed 内容 9303（author_id 回填）")
    void user1003ContentKpis() throws Exception {
        mockMvc.perform(get("/admin-api/oa/productivity-review/list")
                        .param("userId", "1003")
                        .param("page", "1")
                        .param("size", "5")
                        .header("Authorization", ADMIN)
                        .header("X-Tenant-Id", TENANT))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.list[0].userId").value(1003))
                .andExpect(jsonPath("$.data.list[0].contentOutput", greaterThanOrEqualTo(1)))
                .andExpect(jsonPath("$.data.list[0].hitCount", greaterThanOrEqualTo(1)))
                .andExpect(jsonPath("$.data.list[0].avgRead", greaterThanOrEqualTo(1)));
    }

    @Test
    @DisplayName("S-R21: detail 内容指标非占位 0")
    void detailContentMetrics() throws Exception {
        mockMvc.perform(get("/admin-api/oa/productivity-review/detail/1002")
                        .header("Authorization", ADMIN)
                        .header("X-Tenant-Id", TENANT))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.summary.contentOutput", greaterThanOrEqualTo(2)))
                .andExpect(jsonPath("$.data.contentMetrics.publishCount", greaterThanOrEqualTo(2)));
    }
}
