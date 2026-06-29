package cn.iocoder.yudao.module.oa.service.support;

import cn.hutool.core.util.StrUtil;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * 指标构建器 SQL 生成（与前端 metricSchema.buildRuntimeMetricSql 对齐）。
 */
public final class MetricSqlBuilder {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final String MAIN_ALIAS = "t";

    private static final Map<String, List<JoinMeta>> TABLE_JOINS = Map.of(
            "oa_content", List.of(new JoinMeta("oa_account", "account_id", "id")),
            "oa_content_daily", List.of(new JoinMeta("oa_content", "content_id", "id")),
            "oa_follower_daily", List.of(new JoinMeta("oa_account", "account_id", "id")),
            "oa_order", List.of(new JoinMeta("oa_account", "account_id", "id")),
            "oa_order_attribution", List.of(
                    new JoinMeta("oa_order", "order_id", "id"),
                    new JoinMeta("oa_account", "account_id", "id")),
            "oa_account_cost", List.of(new JoinMeta("oa_account", "account_id", "id")),
            "oa_task", List.of()
    );

    private MetricSqlBuilder() {
    }

    public enum BuildMode {
        SAVE, RUNTIME
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class BuilderConfig {
        private String dataSource;
        private String calcMethod = "COUNT";
        private String calcField = "";
        private List<String> groupByFields = List.of();
        private List<String> joinTables = List.of();
        private List<FilterCondition> conditions = List.of();
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class FilterCondition {
        private String field;
        private String operator;
        private String value = "";
        private Boolean asParameter;
        private String queryConditionType;
        private String paramKey;
    }

    private record JoinMeta(String table, String localKey, String foreignKey) {
    }

    public static BuilderConfig unpackBuilder(String paramsJson) {
        if (StrUtil.isBlank(paramsJson)) {
            return null;
        }
        try {
            JsonNode root = MAPPER.readTree(paramsJson);
            JsonNode builder = root.get("builder");
            if (builder == null || builder.isNull()) {
                return null;
            }
            return MAPPER.treeToValue(builder, BuilderConfig.class);
        } catch (Exception ignored) {
            return null;
        }
    }

    /**
     * 运行时 SQL：优先执行已保存的 {@code metricFormula}（保留 left() 等定制表达式）；
     * 若公式与构建器 SAVE 输出不一致，则从 {@code paramsJson} 注入参数化 WHERE 条件；
     * 若公式已含 {@code :p_xxx} 占位符则不再重复注入。
     */
    public static String buildRuntimeMetricSql(String savedFormula, String paramsJson, Map<String, String> bindParams) {
        String trimmed = StrUtil.trim(savedFormula);
        BuilderConfig config = unpackBuilder(paramsJson);

        if (StrUtil.isNotBlank(trimmed) && shouldUseStoredFormula(trimmed, config)) {
            return injectRuntimeParameterConditions(trimmed, config, bindParams);
        }

        if (config != null && StrUtil.isNotBlank(config.getDataSource())) {
            String runtimeSql = buildMetricSqlFromConfig(config, BuildMode.RUNTIME, bindParams);
            if (StrUtil.isNotBlank(runtimeSql)) {
                return runtimeSql;
            }
        }
        return StrUtil.blankToDefault(trimmed, "");
    }

    /** 已保存公式含自定义占位符，或与构建器生成 SQL 不一致时，视为用户定制 SQL。 */
    static boolean shouldUseStoredFormula(String savedFormula, BuilderConfig config) {
        if (metricFormulaHasCustomParams(savedFormula)) {
            return true;
        }
        if (config == null || StrUtil.isBlank(config.getDataSource())) {
            return true;
        }
        String saveSql = buildMetricSqlFromConfig(config, BuildMode.SAVE, Map.of());
        return !savedFormula.trim().equals(StrUtil.trim(saveSql));
    }

    /** 与前端 metricFormulaHasCustomParams 对齐 */
    static boolean metricFormulaHasCustomParams(String formula) {
        return formula != null && formula.matches("(?s).*:[pP]_[a-zA-Z0-9_]+.*");
    }

    /**
     * 已保存的定制 SQL 不含参数化 WHERE 时，从 {@code paramsJson} 注入运行时查询条件占位符。
     */
    static String injectRuntimeParameterConditions(String storedFormula, BuilderConfig config,
                                                   Map<String, String> bindParams) {
        if (metricFormulaHasCustomParams(storedFormula) || config == null) {
            return storedFormula;
        }
        String paramWhere = buildRuntimeParameterWhereClause(config, bindParams);
        if (StrUtil.isBlank(paramWhere)) {
            return storedFormula;
        }
        return DashboardSqlParamBinder.injectAndConditions(storedFormula, paramWhere);
    }

    /** 仅提取 asParameter 条件，生成 AND 连接的 WHERE 片段（含 :p_xxx 占位符）。 */
    static String buildRuntimeParameterWhereClause(BuilderConfig config, Map<String, String> bindParams) {
        if (config == null || config.getConditions() == null || config.getConditions().isEmpty()) {
            return "";
        }
        List<String> parts = new ArrayList<>();
        for (FilterCondition c : config.getConditions()) {
            if (!Boolean.TRUE.equals(c.getAsParameter()) || !conditionIsActive(c)) {
                continue;
            }
            if (bindParams != null && !paramHasBindValue(c, bindParams)) {
                continue;
            }
            parts.add(buildConditionSql(c, bindParams));
        }
        return String.join(" AND ", parts);
    }

    public static String buildMetricSqlFromConfig(BuilderConfig config, BuildMode mode, Map<String, String> bindParams) {
        if (config == null || StrUtil.isBlank(config.getDataSource())) {
            return "";
        }
        String selectExpr = buildAggExpr(config.getCalcMethod(), config.getCalcField());
        List<String> selectCols = new ArrayList<>();
        selectCols.add(selectExpr + " AS metric_value");
        if (config.getGroupByFields() != null) {
            config.getGroupByFields().forEach(f -> selectCols.add(0, resolveSqlColumn(f)));
        }

        FromWhere fromWhere = buildFromWhereClause(
                config.getDataSource(),
                config.getJoinTables() != null ? config.getJoinTables() : List.of(),
                config.getConditions() != null ? config.getConditions() : List.of(),
                mode,
                bindParams);

        StringBuilder sql = new StringBuilder("SELECT ")
                .append(String.join(", ", selectCols))
                .append(" FROM ")
                .append(fromWhere.fromClause())
                .append(" WHERE ")
                .append(fromWhere.whereClause());

        if (config.getGroupByFields() != null && !config.getGroupByFields().isEmpty()) {
            sql.append(" GROUP BY ")
                    .append(String.join(", ", config.getGroupByFields().stream()
                            .map(MetricSqlBuilder::resolveSqlColumn)
                            .toList()));
        }
        return sql.toString();
    }

    /** 从 params_json 提取参数化条件（去重） */
    public static List<FilterCondition> extractParameters(String paramsJson) {
        BuilderConfig config = unpackBuilder(paramsJson);
        if (config == null || config.getConditions() == null) {
            return List.of();
        }
        List<FilterCondition> result = new ArrayList<>();
        Set<String> seen = new java.util.HashSet<>();
        for (FilterCondition cond : config.getConditions()) {
            if (!Boolean.TRUE.equals(cond.getAsParameter()) || StrUtil.isBlank(cond.getField())
                    || StrUtil.isBlank(cond.getOperator())) {
                continue;
            }
            String key = resolveParamKey(cond);
            if (seen.add(key)) {
                result.add(cond);
            }
        }
        return result;
    }

    public static Map<String, String> buildPerfBindParams(String paramsJson, Long targetUserId,
                                                          java.time.LocalDate periodStart,
                                                          java.time.LocalDate periodEnd) {
        Map<String, String> bindParams = new LinkedHashMap<>();
        for (FilterCondition cond : extractParameters(paramsJson)) {
            String pk = resolveParamKey(cond);
            if ("DATE_RANGE".equals(cond.getQueryConditionType())) {
                if (periodStart != null) {
                    bindParams.put(pk + "_start", periodStart.toString());
                }
                if (periodEnd != null) {
                    bindParams.put(pk + "_end", periodEnd.toString());
                }
                continue;
            }
            if ("USER_SELECT".equals(cond.getQueryConditionType()) && targetUserId != null) {
                bindParams.put(pk, String.valueOf(targetUserId));
            }
        }
        return bindParams;
    }

    public static String resolveParamKey(FilterCondition cond) {
        if (cond.getParamKey() != null && !cond.getParamKey().isBlank()) {
            return cond.getParamKey().trim();
        }
        return "p_" + cond.getField().replace(".", "_");
    }

    private static String buildAggExpr(String calcMethod, String calcField) {
        String method = StrUtil.blankToDefault(calcMethod, "COUNT").toUpperCase(Locale.ROOT);
        return switch (method) {
            case "COUNT" -> "COUNT(*)";
            case "DISTINCT_COUNT" -> StrUtil.isNotBlank(calcField)
                    ? "COUNT(DISTINCT " + MAIN_ALIAS + "." + calcField + ")"
                    : "COUNT(*)";
            default -> StrUtil.isNotBlank(calcField)
                    ? method + "(" + MAIN_ALIAS + "." + calcField + ")"
                    : method + "(*)";
        };
    }

    private record FromWhere(String fromClause, String whereClause) {
    }

    private static FromWhere buildFromWhereClause(String dataSource, List<String> joinTables,
                                                  List<FilterCondition> conditions,
                                                  BuildMode mode, Map<String, String> bindParams) {
        List<String> fromParts = new ArrayList<>();
        fromParts.add(dataSource + " " + MAIN_ALIAS);

        List<JoinMeta> joins = TABLE_JOINS.getOrDefault(dataSource, List.of());
        Set<String> joined = new java.util.HashSet<>();
        for (String joinTable : joinTables) {
            if (!joined.add(joinTable)) {
                continue;
            }
            JoinMeta joinMeta = joins.stream().filter(j -> j.table().equals(joinTable)).findFirst().orElse(null);
            if (joinMeta == null) {
                continue;
            }
            String joinAlias = getJoinAlias(joinTable);
            fromParts.add("LEFT JOIN " + joinTable + " " + joinAlias
                    + " ON " + MAIN_ALIAS + "." + joinMeta.localKey() + " = " + joinAlias + "." + joinMeta.foreignKey()
                    + " AND " + joinAlias + ".tenant_id = " + MAIN_ALIAS + ".tenant_id"
                    + " AND " + joinAlias + ".deleted = 0");
        }

        List<String> whereParts = new ArrayList<>();
        whereParts.add(MAIN_ALIAS + ".tenant_id = :tenantId");
        whereParts.add(MAIN_ALIAS + ".deleted = 0");

        filterConditions(conditions, mode, bindParams).forEach(c ->
                whereParts.add(buildConditionSql(c, bindParams)));

        return new FromWhere(String.join(" ", fromParts), String.join(" AND ", whereParts));
    }

    private static List<FilterCondition> filterConditions(List<FilterCondition> conditions, BuildMode mode,
                                                          Map<String, String> bindParams) {
        List<FilterCondition> result = new ArrayList<>();
        for (FilterCondition c : conditions) {
            if (!conditionIsActive(c)) {
                continue;
            }
            if (mode == BuildMode.SAVE && Boolean.TRUE.equals(c.getAsParameter())) {
                continue;
            }
            if (mode == BuildMode.RUNTIME && Boolean.TRUE.equals(c.getAsParameter()) && bindParams != null
                    && !paramHasBindValue(c, bindParams)) {
                continue;
            }
            result.add(c);
        }
        return result;
    }

    private static boolean conditionIsActive(FilterCondition cond) {
        if (StrUtil.isBlank(cond.getField()) || StrUtil.isBlank(cond.getOperator())) {
            return false;
        }
        if ("IS NULL".equals(cond.getOperator()) || "IS NOT NULL".equals(cond.getOperator())) {
            return true;
        }
        if (Boolean.TRUE.equals(cond.getAsParameter())) {
            return true;
        }
        return StrUtil.isNotBlank(cond.getValue()) || "0".equals(cond.getValue());
    }

    private static boolean paramHasBindValue(FilterCondition cond, Map<String, String> bindParams) {
        String pk = resolveParamKey(cond);
        if ("DATE_RANGE".equals(cond.getQueryConditionType())) {
            String raw = bindParams.get(pk);
            if (StrUtil.isNotBlank(raw)) {
                String[] parts = raw.split(",");
                return DashboardSqlParamBinder.isActiveCustomParamValue(parts.length > 0 ? parts[0] : null)
                        || DashboardSqlParamBinder.isActiveCustomParamValue(parts.length > 1 ? parts[1] : null);
            }
            return DashboardSqlParamBinder.isActiveCustomParamValue(bindParams.get(pk + "_start"))
                    || DashboardSqlParamBinder.isActiveCustomParamValue(bindParams.get(pk + "_end"));
        }
        return DashboardSqlParamBinder.isActiveCustomParamValue(bindParams.get(pk));
    }

    private static String buildConditionSql(FilterCondition cond, Map<String, String> bindParams) {
        String col = cond.getField().contains(".") ? cond.getField() : MAIN_ALIAS + "." + cond.getField();
        if ("IS NULL".equals(cond.getOperator()) || "IS NOT NULL".equals(cond.getOperator())) {
            return col + " " + cond.getOperator();
        }
        if (Boolean.TRUE.equals(cond.getAsParameter())) {
            String pk = resolveParamKey(cond);
            if ("DATE_RANGE".equals(cond.getQueryConditionType())) {
                List<String> rangeParts = new ArrayList<>();
                if (bindParams != null) {
                    String raw = bindParams.get(pk);
                    String start = StrUtil.blankToDefault(bindParams.get(pk + "_start"), "");
                    String end = StrUtil.blankToDefault(bindParams.get(pk + "_end"), "");
                    if (StrUtil.isNotBlank(raw)) {
                        String[] parts = raw.split(",");
                        if (StrUtil.isBlank(start) && parts.length > 0) {
                            start = parts[0].trim();
                        }
                        if (StrUtil.isBlank(end) && parts.length > 1) {
                            end = parts[1].trim();
                        }
                    }
                    if (DashboardSqlParamBinder.isActiveCustomParamValue(start)) {
                        rangeParts.add(col + " >= :" + pk + "_start");
                    }
                    if (DashboardSqlParamBinder.isActiveCustomParamValue(end)) {
                        rangeParts.add(col + " <= :" + pk + "_end");
                    }
                    if (!rangeParts.isEmpty()) {
                        return String.join(" AND ", rangeParts);
                    }
                }
                return col + " >= :" + pk + "_start AND " + col + " <= :" + pk + "_end";
            }
            if ("LIKE".equals(cond.getOperator())) {
                return col + " LIKE CONCAT('%', :" + pk + ", '%')";
            }
            if ("IN".equals(cond.getOperator())) {
                return col + " IN (:" + pk + ")";
            }
            return col + " " + cond.getOperator() + " :" + pk;
        }
        if ("IN".equals(cond.getOperator())) {
            String[] parts = cond.getValue().split(",");
            List<String> quoted = new ArrayList<>();
            for (String part : parts) {
                quoted.add(quoteLiteral(part.trim()));
            }
            return col + " IN (" + String.join(", ", quoted) + ")";
        }
        if ("LIKE".equals(cond.getOperator())) {
            return col + " LIKE " + quoteLiteral("%" + cond.getValue() + "%");
        }
        return col + " " + cond.getOperator() + " " + quoteLiteral(cond.getValue());
    }

    private static String quoteLiteral(String raw) {
        if (raw == null) {
            return "NULL";
        }
        String trimmed = raw.trim();
        if (trimmed.isEmpty()) {
            return "''";
        }
        if (trimmed.matches("-?\\d+(\\.\\d+)?")) {
            return trimmed;
        }
        return "'" + trimmed.replace("'", "''") + "'";
    }

    private static String resolveSqlColumn(String fieldName) {
        if (StrUtil.isBlank(fieldName)) {
            return "";
        }
        if (fieldName.contains(".")) {
            return fieldName;
        }
        return MAIN_ALIAS + "." + fieldName;
    }

    private static String getJoinAlias(String table) {
        String shortName = table.replaceFirst("^oa_", "").replace("_", "");
        if (shortName.length() > 6) {
            shortName = shortName.substring(0, 6);
        }
        return "j_" + shortName;
    }
}
