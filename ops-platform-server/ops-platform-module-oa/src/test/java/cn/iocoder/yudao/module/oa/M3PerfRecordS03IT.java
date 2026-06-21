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
class M3PerfRecordS03IT extends OaITBase {

    private static final String ADMIN = "Bearer dev-token-oa-admin";
    private static final String TENANT = "1";

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("M3-S-03: 创建考核记录")
    void createRecord() throws Exception {
        mockMvc.perform(post("/admin-api/oa/perf/record/create")
                        .header("Authorization", ADMIN)
                        .header("X-Tenant-Id", TENANT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "targetUserId": 1003,
                                  "periodType": "MONTH",
                                  "periodStart": "2026-04-01",
                                  "periodEnd": "2026-04-30"
                                }
                                """))
                .andExpect(jsonPath("$.code").value(0));
    }

    @Test
    @DisplayName("M3-S-03: 手动指定模板创建（岗位无匹配模板）")
    void createRecordWithManualTemplate() throws Exception {
        mockMvc.perform(post("/admin-api/oa/perf/record/create")
                        .header("Authorization", ADMIN)
                        .header("X-Tenant-Id", TENANT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "targetUserId": 1004,
                                  "templateId": 9512,
                                  "periodType": "MONTH",
                                  "periodStart": "2026-07-01",
                                  "periodEnd": "2026-07-31"
                                }
                                """))
                .andExpect(jsonPath("$.code").value(0));
    }

    @Test
    @DisplayName("M3-S-03: 岗位无模板且未指定 templateId 拒绝 (2013)")
    void createRecordWithoutTemplateFails() throws Exception {
        mockMvc.perform(post("/admin-api/oa/perf/record/create")
                        .header("Authorization", ADMIN)
                        .header("X-Tenant-Id", TENANT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "targetUserId": 1004,
                                  "periodType": "MONTH",
                                  "periodStart": "2026-08-01",
                                  "periodEnd": "2026-08-31"
                                }
                                """))
                .andExpect(jsonPath("$.code").value(2013));
    }

    @Test
    @DisplayName("M3-S-03: 重复周期拒绝 (2008)")
    void duplicatePeriod() throws Exception {
        mockMvc.perform(post("/admin-api/oa/perf/record/create")
                        .header("Authorization", ADMIN)
                        .header("X-Tenant-Id", TENANT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "targetUserId": 1003,
                                  "periodType": "MONTH",
                                  "periodStart": "2026-05-01",
                                  "periodEnd": "2026-05-31"
                                }
                                """))
                .andExpect(jsonPath("$.code").value(2025));
    }

    @Test
    @DisplayName("M3-S-03: 算分 → 调整 → 确认")
    void calculateAdjustConfirm() throws Exception {
        Long recordId = 9534L;

        mockMvc.perform(post("/admin-api/oa/perf/record/calculate")
                        .header("Authorization", ADMIN)
                        .header("X-Tenant-Id", TENANT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"recordId\": %d}".formatted(recordId)))
                .andExpect(jsonPath("$.code").value(0));

        MvcResult detailResult = mockMvc.perform(get("/admin-api/oa/perf/record/detail")
                        .header("Authorization", ADMIN)
                        .header("X-Tenant-Id", TENANT)
                        .param("id", String.valueOf(recordId)))
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.items[0].score").exists())
                .andReturn();

        Long itemRecordId = JsonPath.parse(detailResult.getResponse().getContentAsString())
                .read("$.data.items[0].id", Long.class);
        Number score = JsonPath.parse(detailResult.getResponse().getContentAsString())
                .read("$.data.items[0].score", Number.class);
        double adjustOk = score.doubleValue() * 0.1;

        mockMvc.perform(put("/admin-api/oa/perf/record/adjust")
                        .header("Authorization", ADMIN)
                        .header("X-Tenant-Id", TENANT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "itemRecordId": %d,
                                  "manualAdjustment": %.2f,
                                  "remark": "IT-微调"
                                }
                                """.formatted(itemRecordId, adjustOk)))
                .andExpect(jsonPath("$.code").value(0));

        mockMvc.perform(put("/admin-api/oa/perf/record/adjust")
                        .header("Authorization", ADMIN)
                        .header("X-Tenant-Id", TENANT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "itemRecordId": %d,
                                  "manualAdjustment": %.2f
                                }
                                """.formatted(itemRecordId, score.doubleValue() * 0.5)))
                .andExpect(jsonPath("$.code").value(2026));

        mockMvc.perform(post("/admin-api/oa/perf/record/confirm")
                        .header("Authorization", ADMIN)
                        .header("X-Tenant-Id", TENANT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"id\": %d}".formatted(recordId)))
                .andExpect(jsonPath("$.code").value(0));
    }
}
