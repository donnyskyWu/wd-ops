package cn.iocoder.yudao.module.oa;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * S-R27a-Mike：M3 绩效详情 + 趋势 userInfo 字段 enrich
 */
@AutoConfigureMockMvc
class M3PerfDetailEnrichIT extends OaITBase {

    private static final String ADMIN = "Bearer dev-token-oa-admin";
    private static final String TENANT = "1";

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("S-R27a: CONFIRMED 考核详情含 templateName/position/审批流字段")
    void recordDetailEnriched() throws Exception {
        mockMvc.perform(get("/admin-api/oa/perf/record/detail")
                        .header("Authorization", ADMIN)
                        .header("X-Tenant-Id", TENANT)
                        .param("id", "9532"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.targetUserId").value(1003))
                .andExpect(jsonPath("$.data.templateName").value("SEED-运营专员考核-2026"))
                .andExpect(jsonPath("$.data.position").value("OPERATOR"))
                .andExpect(jsonPath("$.data.workflowStatus").value(3))
                .andExpect(jsonPath("$.data.submittedAt", notNullValue()))
                .andExpect(jsonPath("$.data.publishedAt", notNullValue()))
                .andExpect(jsonPath("$.data.reviewer1", notNullValue()))
                .andExpect(jsonPath("$.data.items[0].metricCode").value("POST_COUNT"))
                .andExpect(jsonPath("$.data.items[0].actualValue").value(45))
                .andExpect(jsonPath("$.data.items[0].target", greaterThan(0)));
    }

    @Test
    @DisplayName("S-R27a: DRAFT 考核详情 workflowStatus=0 且无审批时间")
    void draftRecordDetailWorkflow() throws Exception {
        mockMvc.perform(get("/admin-api/oa/perf/record/detail")
                        .header("Authorization", ADMIN)
                        .header("X-Tenant-Id", TENANT)
                        .param("id", "9534"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.workflowStatus").value(0))
                .andExpect(jsonPath("$.data.submittedAt").value(nullValue()))
                .andExpect(jsonPath("$.data.reviewer1").value(nullValue()));
    }

    @Test
    @DisplayName("S-R27a: 用户趋势含 userInfo + points 明细")
    void userTrendEnriched() throws Exception {
        mockMvc.perform(get("/admin-api/oa/perf/result/1003/trend")
                        .header("Authorization", ADMIN)
                        .header("X-Tenant-Id", TENANT)
                        .param("month", "6"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.userId").value(1003))
                .andExpect(jsonPath("$.data.userName").value("运营专员"))
                .andExpect(jsonPath("$.data.position").value("OPERATOR"))
                .andExpect(jsonPath("$.data.userInfo.name").value("运营专员"))
                .andExpect(jsonPath("$.data.userInfo.dept", notNullValue()))
                .andExpect(jsonPath("$.data.points[0].period").value("2026-05"))
                .andExpect(jsonPath("$.data.points[0].templateName").value("SEED-运营专员考核-2026"))
                .andExpect(jsonPath("$.data.points[0].totalScore").value(75))
                .andExpect(jsonPath("$.data.points[0].status").value("已发布"));
    }
}
