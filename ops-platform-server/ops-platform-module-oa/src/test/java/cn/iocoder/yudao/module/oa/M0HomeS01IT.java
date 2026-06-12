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
class M0HomeS01IT extends OaITBase {

    private static final String ADMIN = "Bearer dev-token-oa-admin";
    private static final String TENANT = "1";

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("M0-S-01: metrics/kpi 返回非零作者或账号数")
    void metricsAndKpiNonZero() throws Exception {
        mockMvc.perform(get("/admin-api/oa/dashboard/home/metrics")
                        .header("Authorization", ADMIN)
                        .header("X-Tenant-Id", TENANT)
                        .param("startDate", "2026-05-01")
                        .param("endDate", "2026-06-08"))
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.totalAuthors").value(greaterThan(0)));

        mockMvc.perform(get("/admin-api/oa/dashboard/home/kpi")
                        .header("Authorization", ADMIN)
                        .header("X-Tenant-Id", TENANT))
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.totalAccounts").value(greaterThan(0)));
    }

    @Test
    @DisplayName("M0-S-01: trend 返回非空列表")
    void trendNonEmpty() throws Exception {
        mockMvc.perform(get("/admin-api/oa/dashboard/home/trend")
                        .header("Authorization", ADMIN)
                        .header("X-Tenant-Id", TENANT)
                        .param("startDate", "2026-05-01")
                        .param("endDate", "2026-06-08")
                        .param("type", "CONTENT"))
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.length()").value(greaterThan(0)));
    }

    @Test
    @DisplayName("M0-S-01: todos/todo-list 返回待办项")
    void todosNonEmpty() throws Exception {
        mockMvc.perform(get("/admin-api/oa/dashboard/home/todos")
                        .header("Authorization", ADMIN)
                        .header("X-Tenant-Id", TENANT))
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.length()").value(greaterThanOrEqualTo(1)));

        mockMvc.perform(get("/admin-api/oa/dashboard/home/todo-list")
                        .header("Authorization", ADMIN)
                        .header("X-Tenant-Id", TENANT))
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.length()").value(greaterThanOrEqualTo(1)));
    }

    @Test
    @DisplayName("M0-S-01: 非法 platformType → 1503")
    void invalidPlatformType() throws Exception {
        mockMvc.perform(get("/admin-api/oa/dashboard/home/trend")
                        .header("Authorization", ADMIN)
                        .header("X-Tenant-Id", TENANT)
                        .param("platformType", "INVALID"))
                .andExpect(jsonPath("$.code").value(1503));
    }

    @Test
    @DisplayName("M0-S-01: 跨租户 ipGroupId=8001 → 1504")
    void crossTenantIpGroup() throws Exception {
        mockMvc.perform(get("/admin-api/oa/dashboard/home/metrics")
                        .header("Authorization", ADMIN)
                        .header("X-Tenant-Id", TENANT)
                        .param("ipGroupId", "8001"))
                .andExpect(jsonPath("$.code").value(1504));
    }
}
