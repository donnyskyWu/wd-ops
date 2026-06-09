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
class M5FinanceRoiS03IT extends OaITBase {

    private static final String ADMIN = "Bearer dev-token-oa-admin";
    private static final String TENANT = "1";

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("M5-S-03: ROI 分析")
    void analysis() throws Exception {
        mockMvc.perform(get("/admin-api/oa/finance/roi/analysis")
                        .header("Authorization", ADMIN)
                        .header("X-Tenant-Id", TENANT)
                        .param("startDate", "2026-06-01")
                        .param("endDate", "2026-06-08"))
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.totalRevenue").value(greaterThan(0.0)))
                .andExpect(jsonPath("$.data.roi").value(greaterThan(0.0)));
    }

    @Test
    @DisplayName("M5-S-03: ROI 趋势")
    void trend() throws Exception {
        mockMvc.perform(get("/admin-api/oa/finance/roi/trend")
                        .header("Authorization", ADMIN)
                        .header("X-Tenant-Id", TENANT)
                        .param("startDate", "2026-06-01")
                        .param("endDate", "2026-06-08"))
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.points.length()").value(greaterThan(0)));
    }
}
