package cn.iocoder.yudao.module.oa;

import cn.iocoder.yudao.module.oa.service.support.DashboardSqlParamBinder;
import cn.iocoder.yudao.module.oa.service.support.MetricSqlBuilder;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MetricSqlBuilderTest {

    private static final String REVENUE_PARAMS = """
            {"builder":{"dataSource":"oa_order_attribution","calcMethod":"SUM","calcField":"revenue","groupByFields":[],"joinTables":[],"conditions":[{"field":"ops_user_id","operator":"=","value":"","asParameter":true,"queryConditionType":"USER_SELECT","paramKey":"p_user_id"},{"field":"stat_date","operator":">=","value":"","asParameter":true,"queryConditionType":"DATE_RANGE","paramKey":"p_period"}]}}
            """;

    @Test
    @DisplayName("buildRuntimeMetricSql 注入人员与时间参数占位符")
    void runtimeSqlIncludesPersonAndPeriod() {
        Map<String, String> bindParams = Map.of(
                "p_user_id", "1003",
                "p_period_start", "2026-05-01",
                "p_period_end", "2026-05-31"
        );
        String saveFormula = "SELECT COALESCE(SUM(t.revenue), 0) AS metric_value FROM oa_order_attribution t WHERE t.tenant_id = :tenantId AND t.deleted = 0";
        String sql = MetricSqlBuilder.buildRuntimeMetricSql(saveFormula, REVENUE_PARAMS, bindParams);

        assertTrue(sql.contains("ops_user_id = :p_user_id"));
        assertTrue(sql.contains("stat_date >= :p_period_start"));
        assertTrue(sql.contains("stat_date <= :p_period_end"));
        assertTrue(sql.contains("SUM(t.revenue)"));
    }

    @Test
    @DisplayName("buildPerfBindParams 从考核记录映射绑定参数")
    void perfBindParamsFromRecord() {
        Map<String, String> bindParams = MetricSqlBuilder.buildPerfBindParams(
                REVENUE_PARAMS,
                1003L,
                java.time.LocalDate.of(2026, 5, 1),
                java.time.LocalDate.of(2026, 5, 31));

        assertTrue(DashboardSqlParamBinder.isActiveCustomParamValue(bindParams.get("p_user_id")));
        assertTrue(DashboardSqlParamBinder.isActiveCustomParamValue(bindParams.get("p_period_start")));
        assertTrue(DashboardSqlParamBinder.isActiveCustomParamValue(bindParams.get("p_period_end")));
    }

    @Test
    @DisplayName("save 模式 SQL 不含参数化条件")
    void saveModeStripsParameters() {
        MetricSqlBuilder.BuilderConfig config = MetricSqlBuilder.unpackBuilder(REVENUE_PARAMS);
        String sql = MetricSqlBuilder.buildMetricSqlFromConfig(config, MetricSqlBuilder.BuildMode.SAVE, Map.of());

        assertFalse(sql.contains(":p_user_id"));
        assertFalse(sql.contains(":p_period_start"));
        assertTrue(sql.contains("tenant_id = :tenantId"));
    }
}
