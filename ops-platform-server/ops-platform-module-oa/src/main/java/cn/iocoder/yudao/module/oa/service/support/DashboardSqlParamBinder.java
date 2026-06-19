package cn.iocoder.yudao.module.oa.service.support;

import cn.hutool.core.util.StrUtil;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * 大屏全局查询条件绑定。
 * <ul>
 *   <li>{@code globalFilter}（新版）：全局日期/IP 组 → 业务字段，后端注入 WHERE 条件</li>
 *   <li>{@code filterBind}（旧版）：SQL 占位符 → 全局来源，字符串替换</li>
 * </ul>
 */
public final class DashboardSqlParamBinder {

    private static final DateTimeFormatter DATE_TIME_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private static final Set<String> GLOBAL_FILTER_KEYS = Set.of(
            "startDate", "endDate", "startDateTime", "endDateTime", "ipGroupId", "platformType"
    );

    private DashboardSqlParamBinder() {
    }

    public record BindParams(
            Long tenantId,
            LocalDate startDate,
            LocalDate endDate,
            Long ipGroupId,
            String platformType
    ) {
        public static BindParams of(Long tenantId, LocalDate startDate, LocalDate endDate,
                                    Long ipGroupId, String platformType) {
            LocalDate end = endDate != null ? endDate : LocalDate.now();
            LocalDate start = startDate != null ? startDate : end.minusDays(6);
            return new BindParams(tenantId, start, end, ipGroupId, platformType);
        }
    }

    /** 业务字段 → SQL 列（由 layout.globalFilter 写入） */
    public record GlobalFilterConfig(
            String dateColumn,
            String dateFieldType,
            String ipGroupColumn
    ) {
        public static GlobalFilterConfig empty() {
            return new GlobalFilterConfig("", "date", "");
        }

        public boolean hasDate() {
            return StrUtil.isNotBlank(dateColumn);
        }

        public boolean hasIpGroup() {
            return StrUtil.isNotBlank(ipGroupColumn);
        }

        public boolean isEmpty() {
            return !hasDate() && !hasIpGroup();
        }

        @SuppressWarnings("unchecked")
        public static GlobalFilterConfig fromWidgetDef(Map<String, Object> def) {
            if (def == null) {
                return empty();
            }
            Object gf = def.get("globalFilter");
            if (!(gf instanceof Map<?, ?> map)) {
                return empty();
            }
            String dateColumn = str(map.get("dateColumn"));
            if (StrUtil.isBlank(dateColumn) && StrUtil.isNotBlank(str(map.get("dateField")))) {
                dateColumn = str(map.get("dateField"));
            }
            String dateFieldType = str(map.get("dateFieldType"));
            if (StrUtil.isBlank(dateFieldType)) {
                dateFieldType = "date";
            }
            String ipGroupColumn = str(map.get("ipGroupColumn"));
            if (StrUtil.isBlank(ipGroupColumn) && StrUtil.isNotBlank(str(map.get("ipGroupField")))) {
                ipGroupColumn = str(map.get("ipGroupField"));
            }
            return new GlobalFilterConfig(dateColumn, dateFieldType, ipGroupColumn);
        }

        private static String str(Object val) {
            return val == null ? "" : String.valueOf(val).trim();
        }
    }

    public record FilterBindConfig(
            String startDateParam,
            String endDateParam,
            String startDateTimeParam,
            String endDateTimeParam,
            String ipGroupIdParam,
            String platformTypeParam
    ) {
        public static FilterBindConfig legacyAll() {
            return new FilterBindConfig("startDate", "endDate", "startDateTime", "endDateTime", "ipGroupId", "platformType");
        }

        public static FilterBindConfig none() {
            return new FilterBindConfig("", "", "", "", "", "");
        }

        @SuppressWarnings("unchecked")
        public static FilterBindConfig fromWidgetDef(Map<String, Object> def) {
            GlobalFilterConfig gf = GlobalFilterConfig.fromWidgetDef(def);
            FilterBindConfig base = resolveBaseConfig(def, gf);
            if (gf.isEmpty()) {
                return base;
            }
            return new FilterBindConfig(
                    gf.hasDate() ? "" : base.startDateParam(),
                    gf.hasDate() ? "" : base.endDateParam(),
                    gf.hasDate() ? "" : base.startDateTimeParam(),
                    gf.hasDate() ? "" : base.endDateTimeParam(),
                    gf.hasIpGroup() ? "" : base.ipGroupIdParam(),
                    base.platformTypeParam()
            );
        }

        @SuppressWarnings("unchecked")
        private static FilterBindConfig resolveBaseConfig(Map<String, Object> def, GlobalFilterConfig gf) {
            if (def == null) {
                return legacyAll();
            }
            Object fb = def.get("filterBind");
            if (!(fb instanceof Map<?, ?> map)) {
                return gf.isEmpty() ? legacyAll() : none();
            }
            if (map.isEmpty()) {
                return none();
            }
            boolean legacyFormat = map.keySet().stream()
                    .map(k -> String.valueOf(k))
                    .anyMatch(GLOBAL_FILTER_KEYS::contains);
            return legacyFormat ? fromLegacyMap(map) : fromPlaceholderMap(map);
        }

        private static FilterBindConfig fromLegacyMap(Map<?, ?> map) {
            return new FilterBindConfig(
                    str(map.get("startDate")),
                    str(map.get("endDate")),
                    str(map.get("startDateTime")),
                    str(map.get("endDateTime")),
                    str(map.get("ipGroupId")),
                    str(map.get("platformType"))
            );
        }

        private static FilterBindConfig fromPlaceholderMap(Map<?, ?> map) {
            String startDate = "";
            String endDate = "";
            String startDateTime = "";
            String endDateTime = "";
            String ipGroupId = "";
            String platformType = "";
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                String placeholder = str(entry.getKey());
                String source = str(entry.getValue());
                if (StrUtil.isBlank(placeholder) || StrUtil.isBlank(source)) {
                    continue;
                }
                switch (source) {
                    case "startDate" -> startDate = placeholder;
                    case "endDate" -> endDate = placeholder;
                    case "startDateTime" -> startDateTime = placeholder;
                    case "endDateTime" -> endDateTime = placeholder;
                    case "ipGroupId" -> ipGroupId = placeholder;
                    case "platformType" -> platformType = placeholder;
                    default -> { /* ignore unknown source */ }
                }
            }
            return new FilterBindConfig(startDate, endDate, startDateTime, endDateTime, ipGroupId, platformType);
        }

        private static String str(Object val) {
            return val == null ? "" : String.valueOf(val).trim();
        }
    }

    /** 先注入 globalFilter WHERE，再替换占位符 */
    public static String prepareSql(String sqlText, BindParams params, Map<String, Object> widgetDef) {
        if (sqlText == null || sqlText.isBlank() || params == null) {
            return sqlText;
        }
        GlobalFilterConfig gf = GlobalFilterConfig.fromWidgetDef(widgetDef);
        String sql = applyGlobalFilter(sqlText, gf, params);
        FilterBindConfig cfg = FilterBindConfig.fromWidgetDef(widgetDef);
        return bind(sql, params, cfg);
    }

    public static String applyGlobalFilter(String sqlText, GlobalFilterConfig gf, BindParams params) {
        if (sqlText == null || sqlText.isBlank() || gf == null || gf.isEmpty() || params == null) {
            return sqlText;
        }
        List<String> conditions = new ArrayList<>();
        if (gf.hasDate()) {
            if ("datetime".equalsIgnoreCase(gf.dateFieldType())) {
                LocalDateTime startDt = params.startDate().atStartOfDay();
                LocalDateTime endDt = params.endDate().atTime(23, 59, 59);
                conditions.add(gf.dateColumn() + " >= " + quoteDateTime(startDt));
                conditions.add(gf.dateColumn() + " <= " + quoteDateTime(endDt));
            } else {
                conditions.add(gf.dateColumn() + " >= " + quoteDate(params.startDate()));
                conditions.add(gf.dateColumn() + " <= " + quoteDate(params.endDate()));
            }
        }
        if (gf.hasIpGroup() && params.ipGroupId() != null) {
            conditions.add(gf.ipGroupColumn() + " = " + params.ipGroupId());
        }
        if (conditions.isEmpty()) {
            return sqlText;
        }
        return injectAndConditions(sqlText.trim(), String.join(" AND ", conditions));
    }

    public static String bind(String sqlText, BindParams params) {
        return bind(sqlText, params, FilterBindConfig.legacyAll());
    }

    public static String bind(String sqlText, BindParams params, FilterBindConfig cfg) {
        if (sqlText == null || sqlText.isBlank() || params == null) {
            return sqlText;
        }
        FilterBindConfig effective = cfg != null ? cfg : FilterBindConfig.legacyAll();
        String sql = sqlText.trim();
        sql = replaceParam(sql, "tenantId", String.valueOf(params.tenantId()));

        LocalDateTime startDt = params.startDate().atStartOfDay();
        LocalDateTime endDt = params.endDate().atTime(23, 59, 59);

        sql = bindMapped(sql, effective.startDateParam(), quoteDate(params.startDate()));
        sql = bindMapped(sql, effective.endDateParam(), quoteDate(params.endDate()));
        sql = bindMapped(sql, effective.startDateTimeParam(), quoteDateTime(startDt));
        sql = bindMapped(sql, effective.endDateTimeParam(), quoteDateTime(endDt));
        sql = bindMapped(sql, effective.ipGroupIdParam(),
                params.ipGroupId() != null ? String.valueOf(params.ipGroupId()) : "0");
        sql = bindMapped(sql, effective.platformTypeParam(),
                quoteString(StrUtil.blankToDefault(params.platformType(), "")));
        return sql;
    }

    /** 绑定指标构建器自定义参数占位符（:p_xxx） */
    public static String bindCustomParams(String sqlText, Map<String, String> customParams) {
        if (sqlText == null || sqlText.isBlank() || customParams == null || customParams.isEmpty()) {
            return sqlText;
        }
        String sql = sqlText;
        for (Map.Entry<String, String> entry : customParams.entrySet()) {
            String key = entry.getKey();
            if (StrUtil.isBlank(key) || !isActiveCustomParamValue(entry.getValue())) {
                continue;
            }
            sql = replaceParam(sql, key, quoteLiteral(entry.getValue()));
        }
        return sql;
    }

    /** 空值或「全部」(ALL) 不参与指标参数绑定 */
    public static boolean isActiveCustomParamValue(String value) {
        if (StrUtil.isBlank(value)) {
            return false;
        }
        return !"ALL".equalsIgnoreCase(value.trim());
    }

    private static String quoteLiteral(String value) {
        if (value == null) {
            return "NULL";
        }
        String trimmed = value.trim();
        if (trimmed.isEmpty()) {
            return "''";
        }
        if (trimmed.matches("-?\\d+(\\.\\d+)?")) {
            return trimmed;
        }
        return quoteString(trimmed);
    }

    private static String injectAndConditions(String sql, String fragment) {
        String upper = sql.toUpperCase(Locale.ROOT);
        int limitIdx = upper.lastIndexOf(" LIMIT ");
        String core = limitIdx >= 0 ? sql.substring(0, limitIdx).trim() : sql.trim();
        String limitSuffix = limitIdx >= 0 ? sql.substring(limitIdx) : "";

        int insertAt = findClauseInsertPoint(core);
        String coreUpper = core.toUpperCase(Locale.ROOT);
        if (coreUpper.contains(" WHERE ")) {
            core = core.substring(0, insertAt) + " AND " + fragment + core.substring(insertAt);
        } else {
            core = core.substring(0, insertAt) + " WHERE " + fragment + core.substring(insertAt);
        }
        return core + limitSuffix;
    }

    private static int findClauseInsertPoint(String sql) {
        String upper = sql.toUpperCase(Locale.ROOT);
        int groupBy = upper.indexOf(" GROUP BY ");
        int orderBy = upper.indexOf(" ORDER BY ");
        int having = upper.indexOf(" HAVING ");
        int end = sql.length();
        if (groupBy >= 0) {
            end = Math.min(end, groupBy);
        }
        if (orderBy >= 0) {
            end = Math.min(end, orderBy);
        }
        if (having >= 0) {
            end = Math.min(end, having);
        }
        return end;
    }

    private static String bindMapped(String sql, String paramName, String value) {
        if (StrUtil.isBlank(paramName)) {
            return sql;
        }
        return replaceParam(sql, paramName, value);
    }

    private static String replaceParam(String sql, String paramName, String value) {
        return sql.replace(":" + paramName, value);
    }

    private static String quoteDate(LocalDate date) {
        return "'" + date + "'";
    }

    private static String quoteDateTime(LocalDateTime dateTime) {
        return "'" + dateTime.format(DATE_TIME_FMT) + "'";
    }

    private static String quoteString(String value) {
        return "'" + value.replace("'", "''") + "'";
    }
}
