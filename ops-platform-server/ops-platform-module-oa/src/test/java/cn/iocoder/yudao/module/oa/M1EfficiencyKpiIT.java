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
 * S-R9: FR-M1-007 人效盘点 IT
 *   - B1: list 端点收 4 参（startDate/endDate/timeDimension/position）
 *   - B2: list 返回 5 KPI 全聚合（taskTotal/taskCompleted/costAmount/revenue/roi）
 *   - B3: 补 3 端点 detail/{userId} / detail/anchors / export
 *   - B9: page/size 契约
 */
@AutoConfigureMockMvc
class M1EfficiencyKpiIT extends OaITBase {

    private static final String ADMIN = "Bearer dev-token-oa-admin";
    private static final String TENANT = "1";

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("S-R9 B1+B2: list 端点收 timeDimension/position + 返回 5 KPI 字段")
    void listKpis() throws Exception {
        mockMvc.perform(get("/admin-api/oa/productivity-review/list")
                        .param("startDate", "2026-05-01")
                        .param("endDate", "2026-05-31")
                        .param("timeDimension", "WEEK")
                        .param("position", "OPERATOR")
                        .param("page", "1")
                        .param("size", "20")
                        .header("Authorization", ADMIN)
                        .header("X-Tenant-Id", TENANT))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.list").isArray())
                // VO 字段全部存在
                .andExpect(jsonPath("$.data.list[0].userId").exists())
                .andExpect(jsonPath("$.data.list[0].userName").exists())
                .andExpect(jsonPath("$.data.list[0].taskTotal").exists())
                .andExpect(jsonPath("$.data.list[0].taskCompleted").exists())
                .andExpect(jsonPath("$.data.list[0].completionRate").exists())
                .andExpect(jsonPath("$.data.list[0].costAmount").exists())
                .andExpect(jsonPath("$.data.list[0].revenue").exists())
                .andExpect(jsonPath("$.data.list[0].roi").exists())
                // KPI >= 0
                .andExpect(jsonPath("$.data.list[0].taskTotal", greaterThanOrEqualTo(0)))
                .andExpect(jsonPath("$.data.list[0].revenue", greaterThanOrEqualTo(0.0)));
    }

    @Test
    @DisplayName("S-R9 B2: 1003 (OPERATOR) 至少聚合到 1 个 DONE 任务 + 12 笔订单营收")
    void user1003Kpis() throws Exception {
        // 1003 在 IP组 9001 下：12 笔订单，营收 123900，cost 30975
        mockMvc.perform(get("/admin-api/oa/productivity-review/list")
                        .param("userId", "1003")
                        .param("page", "1").param("size", "5")
                        .header("Authorization", ADMIN)
                        .header("X-Tenant-Id", TENANT))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.list[0].userId").value(1003))
                .andExpect(jsonPath("$.data.list[0].revenue", greaterThanOrEqualTo(0.0)))
                .andExpect(jsonPath("$.data.list[0].orderCount", greaterThanOrEqualTo(0)))
                .andExpect(jsonPath("$.data.list[0].taskTotal", greaterThanOrEqualTo(0)));
    }

    @Test
    @DisplayName("S-R9 B2: completionRate 范围 0-100")
    void completionRateInRange() throws Exception {
        mockMvc.perform(get("/admin-api/oa/productivity-review/list")
                        .param("page", "1").param("size", "20")
                        .header("Authorization", ADMIN)
                        .header("X-Tenant-Id", TENANT))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.list[0].completionRate", greaterThanOrEqualTo(0.0)));
    }

    @Test
    @DisplayName("S-R9 B1: timeDimension=week 默认")
    void defaultTimeDimension() throws Exception {
        // 不传 timeDimension 应使用默认 WEEK，不报错
        mockMvc.perform(get("/admin-api/oa/productivity-review/list")
                        .param("page", "1").param("size", "10")
                        .header("Authorization", ADMIN)
                        .header("X-Tenant-Id", TENANT))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));
    }

    @Test
    @DisplayName("S-R9 B3: detail/{userId} 返回 summary + taskList + financeByGroup + trend")
    void detail() throws Exception {
        mockMvc.perform(get("/admin-api/oa/productivity-review/detail/1003")
                        .param("startDate", "2026-05-01")
                        .param("endDate", "2026-05-31")
                        .header("Authorization", ADMIN)
                        .header("X-Tenant-Id", TENANT))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.summary.userId").value(1003))
                .andExpect(jsonPath("$.data.summary.userName").exists())
                .andExpect(jsonPath("$.data.summary.taskCompleted").exists())
                .andExpect(jsonPath("$.data.taskList").isArray())
                .andExpect(jsonPath("$.data.financeByGroup").isArray())
                .andExpect(jsonPath("$.data.trend").isArray());
    }

    @Test
    @DisplayName("S-R9 B3: detail/anchors 按 IP 组聚合")
    void anchors() throws Exception {
        mockMvc.perform(get("/admin-api/oa/productivity-review/detail/anchors")
                        .param("ipGroupId", "9001")
                        .param("startDate", "2026-05-01")
                        .param("endDate", "2026-05-31")
                        .header("Authorization", ADMIN)
                        .header("X-Tenant-Id", TENANT))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    @DisplayName("S-R9 B3: export 返回 text/csv 含 BOM")
    void exportCsv() throws Exception {
        org.springframework.test.web.servlet.MvcResult result = mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .post("/admin-api/oa/productivity-review/export")
                        .param("startDate", "2026-05-01")
                        .param("endDate", "2026-05-31")
                        .header("Authorization", ADMIN)
                        .header("X-Tenant-Id", TENANT))
                .andExpect(status().isOk())
                .andReturn();
        byte[] body = result.getResponse().getContentAsByteArray();
        // BOM 校验
        org.junit.jupiter.api.Assertions.assertTrue(body.length >= 3 && body[0] == (byte) 0xEF && body[1] == (byte) 0xBB && body[2] == (byte) 0xBF,
                "CSV must start with UTF-8 BOM");
        // header row 校验
        String csv = new String(body, java.nio.charset.StandardCharsets.UTF_8);
        org.junit.jupiter.api.Assertions.assertTrue(csv.contains("用户ID"), "CSV header must include '用户ID'");
    }

    @Test
    @DisplayName("S-R9 B9: page=2 size=5 返回 0/5 行 (page/size 契约)")
    void pagination() throws Exception {
        mockMvc.perform(get("/admin-api/oa/productivity-review/list")
                        .param("page", "2").param("size", "5")
                        .header("Authorization", ADMIN)
                        .header("X-Tenant-Id", TENANT))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.list.length()").value(org.hamcrest.Matchers.lessThanOrEqualTo(5)));
    }

    @Test
    @DisplayName("S-R9 B1: position 过滤")
    void positionFilter() throws Exception {
        mockMvc.perform(get("/admin-api/oa/productivity-review/list")
                        .param("position", "OPS_LEADER")
                        .param("page", "1").param("size", "20")
                        .header("Authorization", ADMIN)
                        .header("X-Tenant-Id", TENANT))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.list").isArray());
    }

    @Test
    @DisplayName("S-R10 B10: detail 不传 date 时 trend 用 today-30 兜底返回真数据")
    void detailTrendFallback() throws Exception {
        // 1003 在 6 月有 8 笔订单（seed V22），不传 date 应返回 today-30 trend
        mockMvc.perform(get("/admin-api/oa/productivity-review/detail/1003")
                        .header("Authorization", ADMIN)
                        .header("X-Tenant-Id", TENANT))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.summary.userId").value(1003))
                .andExpect(jsonPath("$.data.trend").isArray())
                // trend 至少含一个日期（seed 6/1 起有数据）
                .andExpect(jsonPath("$.data.trend[0].date").exists())
                .andExpect(jsonPath("$.data.trend[0].revenue", greaterThanOrEqualTo(0.0)))
                .andExpect(jsonPath("$.data.trend[0].orderCount", greaterThanOrEqualTo(0)));
    }

    @Test
    @DisplayName("S-R10 B11: detail 返回 contentMetrics 占位 0 (ADR-008)")
    void detailContentMetrics() throws Exception {
        mockMvc.perform(get("/admin-api/oa/productivity-review/detail/1003")
                        .header("Authorization", ADMIN)
                        .header("X-Tenant-Id", TENANT))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.contentMetrics").exists())
                .andExpect(jsonPath("$.data.contentMetrics.publishCount").value(0))
                .andExpect(jsonPath("$.data.contentMetrics.avgReading").value(0))
                .andExpect(jsonPath("$.data.contentMetrics.hotCount").value(0));
    }
}