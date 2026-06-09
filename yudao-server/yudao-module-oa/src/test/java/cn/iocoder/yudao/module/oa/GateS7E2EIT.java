package cn.iocoder.yudao.module.oa;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

/**
 * GATE-S7 E2E API 冒烟（E2E-01 ~ E2E-09）。
 */
@AutoConfigureMockMvc
class GateS7E2EIT extends OaITBase {

    private static final String ADMIN = "Bearer dev-token-oa-admin";
    private static final String TENANT_B = "Bearer dev-token-oa-tenantb";
    private static final String TENANT_1 = "1";
    private static final String TENANT_2 = "2";

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("E2E-01: 资产链路 seed 存在 + 公司创建")
    void assetChainSeedAndCreate() throws Exception {
        mockMvc.perform(get("/admin-api/oa/company/list")
                        .header("Authorization", ADMIN)
                        .header("X-Tenant-Id", TENANT_1)
                        .param("companyName", "SEED"))
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.total").value(greaterThan(0)));

        mockMvc.perform(get("/admin-api/oa/account/list")
                        .header("Authorization", ADMIN)
                        .header("X-Tenant-Id", TENANT_1))
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.total").value(greaterThanOrEqualTo(10)));

        mockMvc.perform(post("/admin-api/oa/company/create")
                        .header("Authorization", ADMIN)
                        .header("X-Tenant-Id", TENANT_1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "companyName": "E2E-链路测试公司",
                                  "creditCode": "91330100MA2HE2E001",
                                  "industry": "传媒",
                                  "legalName": "测试",
                                  "legalIdCard": "330101199001011234",
                                  "mpCapacityStandard": 10,
                                  "status": "ENABLED"
                                }
                                """))
                .andExpect(jsonPath("$.code").value(0));
    }

    @Test
    @DisplayName("E2E-02: ip-group + author 列表非空")
    void ipGroupAndAuthorList() throws Exception {
        mockMvc.perform(get("/admin-api/oa/ip-group/tree")
                        .header("Authorization", ADMIN)
                        .header("X-Tenant-Id", TENANT_1))
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.length()").value(greaterThan(0)));

        mockMvc.perform(get("/admin-api/oa/author/list")
                        .header("Authorization", ADMIN)
                        .header("X-Tenant-Id", TENANT_1))
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.total").value(greaterThan(0)));
    }

    @Test
    @DisplayName("E2E-03: SOP 模板 + 任务 + 审核 API")
    void sopTaskReviewFlow() throws Exception {
        mockMvc.perform(get("/admin-api/oa/sop/template/list")
                        .header("Authorization", ADMIN)
                        .header("X-Tenant-Id", TENANT_1))
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.total").value(greaterThan(0)));

        mockMvc.perform(get("/admin-api/oa/task/list")
                        .header("Authorization", ADMIN)
                        .header("X-Tenant-Id", TENANT_1))
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.total").value(greaterThan(0)));

        mockMvc.perform(get("/admin-api/oa/sop/review/pending")
                        .header("Authorization", ADMIN)
                        .header("X-Tenant-Id", TENANT_1)
                        .param("reviewerId", "1002"))
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.length()").value(greaterThanOrEqualTo(1)));
    }

    @Test
    @DisplayName("E2E-04: 绩效记录算分")
    void perfRecordCalculate() throws Exception {
        mockMvc.perform(post("/admin-api/oa/perf/record/calculate")
                        .header("Authorization", ADMIN)
                        .header("X-Tenant-Id", TENANT_1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"recordId\": 9534}"))
                .andExpect(jsonPath("$.code").value(0));
    }

    @Test
    @DisplayName("E2E-05: 订单归因 ROI 非空")
    void orderAttributionRoi() throws Exception {
        mockMvc.perform(get("/admin-api/oa/order-attribution/roi")
                        .header("Authorization", ADMIN)
                        .header("X-Tenant-Id", TENANT_1)
                        .param("startDate", "2026-06-01")
                        .param("endDate", "2026-06-08"))
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.byIpGroup.length()").value(greaterThanOrEqualTo(1)));
    }

    @Test
    @DisplayName("E2E-06: 报表 stats + 分析大屏非空")
    void reportStatsAndDashboard() throws Exception {
        mockMvc.perform(get("/admin-api/oa/report/unified-account/stats")
                        .header("Authorization", ADMIN)
                        .header("X-Tenant-Id", TENANT_1)
                        .param("startDate", "2026-05-01")
                        .param("endDate", "2026-06-08"))
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.totalAccounts").value(greaterThan(0)));

        mockMvc.perform(get("/admin-api/oa/dashboard/9851")
                        .header("Authorization", ADMIN)
                        .header("X-Tenant-Id", TENANT_1))
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.dashboardName").exists());
    }

    @Test
    @DisplayName("E2E-07: 首页 alert-list 非空")
    void homeAlertList() throws Exception {
        mockMvc.perform(get("/admin-api/oa/dashboard/home/alert-list")
                        .header("Authorization", ADMIN)
                        .header("X-Tenant-Id", TENANT_1))
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.length()").value(greaterThanOrEqualTo(1)));
    }

    @Test
    @DisplayName("E2E-08: tenant-b 跨租户 1504")
    void tenantBCrossTenant() throws Exception {
        mockMvc.perform(get("/admin-api/oa/company/list")
                        .header("Authorization", TENANT_B)
                        .header("X-Tenant-Id", TENANT_2)
                        .param("companyName", "SEED-T1"))
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.total").value(0));

        mockMvc.perform(get("/admin-api/oa/dashboard/home/metrics")
                        .header("Authorization", ADMIN)
                        .header("X-Tenant-Id", TENANT_1)
                        .param("ipGroupId", "8001"))
                .andExpect(jsonPath("$.code").value(1504));
    }

    @Test
    @DisplayName("E2E-09: 首页 kpi + todos 非空")
    void homeKpiAndTodos() throws Exception {
        mockMvc.perform(get("/admin-api/oa/dashboard/home/kpi")
                        .header("Authorization", ADMIN)
                        .header("X-Tenant-Id", TENANT_1))
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.totalAccounts").value(greaterThan(0)));

        mockMvc.perform(get("/admin-api/oa/dashboard/home/todos")
                        .header("Authorization", ADMIN)
                        .header("X-Tenant-Id", TENANT_1))
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.length()").value(greaterThanOrEqualTo(1)));
    }
}
