package cn.iocoder.yudao.module.oa;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

@AutoConfigureMockMvc
class M6ReportS02IT extends OaITBase {

    private static final String ADMIN = "Bearer dev-token-oa-admin";
    private static final String TENANT = "1";

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("M6-S-02: 全平台账号 stats 非空")
    void unifiedAccountStats() throws Exception {
        mockMvc.perform(get("/admin-api/oa/report/unified-account/stats")
                        .header("Authorization", ADMIN)
                        .header("X-Tenant-Id", TENANT)
                        .param("startDate", "2026-03-11")
                        .param("endDate", "2026-06-08"))
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.totalAccounts").value(greaterThan(0)))
                .andExpect(jsonPath("$.data.totalFollowers").value(greaterThan(0)));
    }

    @Test
    @DisplayName("M6-S-02: stats.totalAccounts 与 list.total 一致（全量 oa_account，非 pageSize）")
    void unifiedAccountStatsMatchesListTotal() throws Exception {
        var statsResult = mockMvc.perform(get("/admin-api/oa/report/unified-account/stats")
                        .header("Authorization", ADMIN)
                        .header("X-Tenant-Id", TENANT))
                .andExpect(jsonPath("$.code").value(0))
                .andReturn();
        var listResult = mockMvc.perform(get("/admin-api/oa/report/unified-account/list")
                        .header("Authorization", ADMIN)
                        .header("X-Tenant-Id", TENANT)
                        .param("pageNum", "1")
                        .param("pageSize", "20"))
                .andExpect(jsonPath("$.code").value(0))
                .andReturn();

        long statsTotal = com.jayway.jsonpath.JsonPath.read(
                statsResult.getResponse().getContentAsString(), "$.data.totalAccounts");
        long listTotal = com.jayway.jsonpath.JsonPath.read(
                listResult.getResponse().getContentAsString(), "$.data.total");
        org.junit.jupiter.api.Assertions.assertEquals(listTotal, statsTotal,
                "stats.totalAccounts 应等于 list.total（oa_account 全量计数）");
        org.junit.jupiter.api.Assertions.assertTrue(statsTotal > 20 || listTotal <= 20,
                "当 oa_account 超过 20 条时，totalAccounts 不得等于 pageSize");
    }

    @Test
    @DisplayName("M6-S-02: 账号状态 trend 非空")
    void accountStatusTrend() throws Exception {
        mockMvc.perform(get("/admin-api/oa/report/account-status/trend")
                        .header("Authorization", ADMIN)
                        .header("X-Tenant-Id", TENANT)
                        .param("accountId", "9001")
                        .param("startDate", "2026-03-11")
                        .param("endDate", "2026-06-08"))
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.length()").value(greaterThanOrEqualTo(90)));
    }
}
