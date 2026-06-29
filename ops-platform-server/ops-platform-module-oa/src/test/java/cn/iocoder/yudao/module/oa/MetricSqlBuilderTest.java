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
        MetricSqlBuilder.BuilderConfig config = MetricSqlBuilder.unpackBuilder(REVENUE_PARAMS);
        String saveFormula = MetricSqlBuilder.buildMetricSqlFromConfig(config, MetricSqlBuilder.BuildMode.SAVE, Map.of());
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

    @Test
    @DisplayName("自定义 SQL 含 left() 时优先执行 stored formula")
    void customSqlWithLeftExpressionUsesStoredFormula() {
        String customSql = """
                SELECT t.platform_type, left(t.publish_time,10), COUNT(*) AS metric_value
                FROM oa_content t
                WHERE t.tenant_id = :tenantId AND t.deleted = 0
                GROUP BY left(t.publish_time,10), t.platform_type
                """;
        String paramsJson = """
                {"builder":{"dataSource":"oa_content","calcMethod":"COUNT","calcField":"","groupByFields":["publish_time","platform_type"],"joinTables":[],"conditions":[]}}
                """;
        String sql = MetricSqlBuilder.buildRuntimeMetricSql(customSql, paramsJson, Map.of());

        assertTrue(sql.contains("left(t.publish_time,10)"));
        assertFalse(sql.contains("GROUP BY t.publish_time, t.platform_type"));
    }

    @Test
    @DisplayName("自定义 SQL 含 left() 时仍注入运行时参数化条件")
    void customSqlWithLeftExpressionInjectsRuntimeParams() {
        String customSql = """
                SELECT t.platform_type, left(t.publish_time,10), COUNT(*) AS metric_value
                FROM oa_content t
                WHERE t.tenant_id = :tenantId AND t.deleted = 0
                GROUP BY left(t.publish_time,10), t.platform_type
                """;
        String paramsJson = """
                {"builder":{"dataSource":"oa_content","calcMethod":"COUNT","calcField":"","groupByFields":["publish_time","platform_type"],"joinTables":[],"conditions":[{"field":"publish_time","operator":">=","value":"","asParameter":true,"queryConditionType":"DATE_RANGE","paramKey":"p_period"},{"field":"platform_type","operator":"=","value":"","asParameter":true,"paramKey":"p_platform"}]}}
                """;
        Map<String, String> bindParams = Map.of(
                "p_period_start", "2026-05-01",
                "p_period_end", "2026-05-31",
                "p_platform", "DOUYIN"
        );
        String sql = MetricSqlBuilder.buildRuntimeMetricSql(customSql, paramsJson, bindParams);

        assertTrue(sql.contains("left(t.publish_time,10)"));
        assertTrue(sql.contains("t.publish_time >= :p_period_start"));
        assertTrue(sql.contains("t.publish_time <= :p_period_end"));
        assertTrue(sql.contains("t.platform_type = :p_platform"));
        assertFalse(sql.contains("GROUP BY t.publish_time, t.platform_type"));
    }

    @Test
    @DisplayName("groupBy 支持关联表 alias.field")
    void groupByJoinTableFieldUsesAlias() {
        String paramsJson = """
                {"builder":{"dataSource":"oa_content","calcMethod":"COUNT","calcField":"","groupByFields":["account_id","j_accoun.platform_type"],"joinTables":["oa_account"],"conditions":[]}}
                """;
        MetricSqlBuilder.BuilderConfig config = MetricSqlBuilder.unpackBuilder(paramsJson);
        String sql = MetricSqlBuilder.buildMetricSqlFromConfig(config, MetricSqlBuilder.BuildMode.SAVE, Map.of());

        assertTrue(sql.contains("j_accoun.platform_type"));
        assertTrue(sql.contains("GROUP BY t.account_id, j_accoun.platform_type"));
        assertFalse(sql.contains("t.j_accoun.platform_type"));
    }
}
