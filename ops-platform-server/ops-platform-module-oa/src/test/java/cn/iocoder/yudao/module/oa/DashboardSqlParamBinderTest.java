package cn.iocoder.yudao.module.oa;

import cn.iocoder.yudao.module.oa.service.support.DashboardSqlParamBinder;
import cn.iocoder.yudao.module.oa.service.support.DashboardSqlParamBinder.BindParams;
import cn.iocoder.yudao.module.oa.service.support.DashboardSqlParamBinder.FilterBindConfig;
import cn.iocoder.yudao.module.oa.service.support.DashboardSqlParamBinder.GlobalFilterConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DashboardSqlParamBinderTest {

    @Test
    @DisplayName("globalFilter：日期字段注入 WHERE")
    void globalFilterDateFieldInjection() {
        Map<String, Object> def = Map.of("globalFilter", Map.of(
                "dateField", "publish_time",
                "dateColumn", "t.publish_time",
                "dateFieldType", "datetime"
        ));
        BindParams params = BindParams.of(1L, LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 7), null, null);
        String sql = """
                SELECT COUNT(*) FROM oa_content t
                WHERE t.tenant_id = :tenantId AND t.deleted = 0
                """;
        String bound = DashboardSqlParamBinder.prepareSql(sql, params, def);
        assertTrue(bound.contains("t.publish_time >= '2026-06-01 00:00:00'"));
        assertTrue(bound.contains("t.publish_time <= '2026-06-07 23:59:59'"));
        assertFalse(bound.contains(":tenantId"));
    }

    @Test
    @DisplayName("globalFilter：IP 组字段注入 WHERE")
    void globalFilterIpGroupFieldInjection() {
        Map<String, Object> def = Map.of("globalFilter", Map.of(
                "ipGroupField", "ip_group_id",
                "ipGroupColumn", "t.ip_group_id"
        ));
        BindParams params = BindParams.of(1L, LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 7), 42L, null);
        String sql = "SELECT 1 FROM oa_order t WHERE t.tenant_id = :tenantId";
        String bound = DashboardSqlParamBinder.prepareSql(sql, params, def);
        assertTrue(bound.contains("t.ip_group_id = 42"));
    }

    @Test
    @DisplayName("globalFilter 存在时不重复绑定标准日期占位符")
    void globalFilterSuppressesPlaceholderDateBind() {
        Map<String, Object> def = Map.of("globalFilter", Map.of(
                "dateField", "stat_date",
                "dateColumn", "t.stat_date",
                "dateFieldType", "date"
        ));
        BindParams params = BindParams.of(1L, LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 7), null, null);
        String sql = "SELECT 1 WHERE d >= :startDate AND s >= :startDate";
        String bound = DashboardSqlParamBinder.prepareSql(sql, params, def);
        assertTrue(bound.contains("t.stat_date >= '2026-06-01'"));
        assertTrue(bound.contains(":startDate"), "globalFilter 已处理日期时不应替换遗留占位符");
    }

    @Test
    @DisplayName("placeholderBind：自定义占位符映射到全局日期（旧版兼容）")
    void placeholderBindCustomDateParams() {
        Map<String, Object> def = Map.of("filterBind", Map.of(
                "publish_start", "startDate",
                "publish_end", "endDate"
        ));
        FilterBindConfig cfg = FilterBindConfig.fromWidgetDef(def);
        BindParams params = BindParams.of(1L, LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 7), null, null);
        String sql = """
                SELECT 1 FROM oa_content
                WHERE tenant_id = :tenantId
                  AND publish_date >= :publish_start
                  AND publish_date <= :publish_end
                  AND ip_group_id = :group_id
                """;
        String bound = DashboardSqlParamBinder.bind(sql, params, cfg);
        assertTrue(bound.contains("publish_date >= '2026-06-01'"));
        assertTrue(bound.contains("publish_date <= '2026-06-07'"));
        assertTrue(bound.contains("tenant_id = 1") || bound.contains("tenant_id = '1'") || bound.contains("= 1"));
        assertTrue(bound.contains(":group_id"), "未映射的占位符应保持原样");
    }

    @Test
    @DisplayName("placeholderBind：仅映射 IP 组与平台")
    void placeholderBindIpAndPlatform() {
        Map<String, Object> def = Map.of("filterBind", Map.of(
                "group_id", "ipGroupId",
                "plat", "platformType"
        ));
        FilterBindConfig cfg = FilterBindConfig.fromWidgetDef(def);
        BindParams params = BindParams.of(1L, LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 7), 99L, "DOUYIN");
        String sql = "SELECT 1 WHERE g = :group_id AND p = :plat AND d >= :publish_start";
        String bound = DashboardSqlParamBinder.bind(sql, params, cfg);
        assertTrue(bound.contains("g = 99"));
        assertTrue(bound.contains("p = 'DOUYIN'"));
        assertTrue(bound.contains(":publish_start"), "日期未映射时不应替换");
    }

    @Test
    @DisplayName("空 filterBind 仅绑定 tenantId")
    void emptyFilterBindOnlyTenant() {
        Map<String, Object> def = Map.of("globalFilter", Map.of(), "filterBind", Map.of());
        BindParams params = BindParams.of(1L, LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 7), 5L, "XHS");
        String sql = "SELECT 1 WHERE tenant_id = :tenantId AND d >= :startDate AND g = :ipGroupId";
        String bound = DashboardSqlParamBinder.prepareSql(sql, params, def);
        assertFalse(bound.contains(":tenantId"));
        assertTrue(bound.contains(":startDate"));
        assertTrue(bound.contains(":ipGroupId"));
    }

    @Test
    @DisplayName("兼容旧版 global→placeholder 结构")
    void legacyFilterBindFormat() {
        Map<String, Object> def = Map.of("filterBind", Map.of(
                "startDate", "publish_start",
                "endDate", "publish_end"
        ));
        FilterBindConfig cfg = FilterBindConfig.fromWidgetDef(def);
        BindParams params = BindParams.of(1L, LocalDate.of(2026, 6, 2), LocalDate.of(2026, 6, 5), null, null);
        String sql = "SELECT 1 WHERE d >= :publish_start AND d <= :publish_end";
        String bound = DashboardSqlParamBinder.bind(sql, params, cfg);
        assertTrue(bound.contains("d >= '2026-06-02'"));
        assertTrue(bound.contains("d <= '2026-06-05'"));
    }

    @Test
    @DisplayName("未配置 filterBind / globalFilter 时使用标准占位符")
    void missingFilterBindUsesLegacyAll() {
        FilterBindConfig cfg = FilterBindConfig.fromWidgetDef(Map.of("id", "w1"));
        BindParams params = BindParams.of(1L, LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 7), 3L, "WB");
        String sql = "SELECT 1 WHERE d >= :startDate AND g = :ipGroupId AND p = :platformType";
        String bound = DashboardSqlParamBinder.bind(sql, params, cfg);
        assertEquals(
                "SELECT 1 WHERE d >= '2026-06-01' AND g = 3 AND p = 'WB'",
                bound
        );
    }

    @Test
    @DisplayName("GlobalFilterConfig 可从 dateField 回退解析 dateColumn")
    void globalFilterConfigFallbackColumn() {
        GlobalFilterConfig cfg = GlobalFilterConfig.fromWidgetDef(Map.of(
                "globalFilter", Map.of("dateField", "stat_date")
        ));
        assertTrue(cfg.hasDate());
        assertEquals("stat_date", cfg.dateColumn());
    }
}
