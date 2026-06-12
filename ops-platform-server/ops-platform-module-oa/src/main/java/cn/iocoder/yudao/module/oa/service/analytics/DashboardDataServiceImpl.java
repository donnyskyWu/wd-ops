package cn.iocoder.yudao.module.oa.service.analytics;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.exception.OaErrorCodes;
import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.oa.api.dto.analytics.DashboardDataVO;
import cn.iocoder.yudao.module.oa.api.dto.analytics.DashboardVO;
import cn.iocoder.yudao.module.oa.api.dto.analytics.DashboardWidgetResultVO;
import cn.iocoder.yudao.module.oa.api.dto.analytics.MetricPreviewReq;
import cn.iocoder.yudao.module.oa.api.dto.analytics.MetricPreviewVO;
import cn.iocoder.yudao.module.oa.api.dto.home.DashboardHomeKpiVO;
import cn.iocoder.yudao.module.oa.api.dto.home.TrendPointVO;
import cn.iocoder.yudao.module.oa.api.dto.monitor.ExternalWorkVO;
import cn.iocoder.yudao.module.oa.api.dto.monitor.MonitorFollowerAccountVO;
import cn.iocoder.yudao.module.oa.dal.dataobject.account.AccountDO;
import cn.iocoder.yudao.module.oa.dal.dataobject.operations.ContentDO;
import cn.iocoder.yudao.module.oa.dal.dataobject.operations.FollowerDailyDO;
import cn.iocoder.yudao.module.oa.dal.dataobject.perf.MetricDO;
import cn.iocoder.yudao.module.oa.dal.dataobject.perf.OrderDO;
import cn.iocoder.yudao.module.oa.dal.mysql.account.AccountMapper;
import cn.iocoder.yudao.module.oa.dal.mysql.perf.MetricMapper;
import cn.iocoder.yudao.module.oa.dal.mysql.operations.ContentMapper;
import cn.iocoder.yudao.module.oa.dal.mysql.operations.FollowerDailyMapper;
import cn.iocoder.yudao.module.oa.dal.mysql.perf.OrderMapper;
import cn.iocoder.yudao.module.oa.service.home.HomeDashboardService;
import cn.iocoder.yudao.module.oa.service.monitor.MonitorService;
import cn.iocoder.yudao.module.oa.service.support.DashboardSqlParamBinder;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class DashboardDataServiceImpl implements DashboardDataService {

    private final DashboardService dashboardService;
    private final HomeDashboardService homeDashboardService;
    private final CustomQueryService customQueryService;
    private final AnalyticsMetricService analyticsMetricService;
    private final MonitorService monitorService;
    private final ContentMapper contentMapper;
    private final FollowerDailyMapper followerDailyMapper;
    private final AccountMapper accountMapper;
    private final OrderMapper orderMapper;
    private final MetricMapper metricMapper;
    private final ObjectMapper objectMapper;

    @Override
    public DashboardDataVO loadData(Long dashboardId, Long ipGroupId, LocalDate startDate, LocalDate endDate,
                                    String platformType) {
        DashboardVO dashboard = dashboardService.get(dashboardId);
        LocalDate end = endDate != null ? endDate : LocalDate.now();
        LocalDate start = startDate != null ? startDate : end.minusDays(6);

        Map<String, Object> layoutRoot = parseLayout(dashboard.getLayout());
        List<Map<String, Object>> widgetDefs = extractWidgets(layoutRoot);
        String scope = layoutRoot.get("scope") != null ? String.valueOf(layoutRoot.get("scope")) : "INTERNAL";

        Long tenantId = TenantContextHolder.getTenantId();
        List<Long> accountIds = resolveAccountIds(tenantId, ipGroupId, platformType);
        DashboardSqlParamBinder.BindParams sqlParams = DashboardSqlParamBinder.BindParams.of(
                tenantId, start, end, ipGroupId, platformType);

        List<DashboardWidgetResultVO> widgets = new ArrayList<>();
        for (Map<String, Object> def : widgetDefs) {
            try {
                widgets.add(resolveWidget(def, scope, tenantId, ipGroupId, start, end, platformType, accountIds, sqlParams));
            } catch (Exception ex) {
                log.warn("[dashboard-data] widget {} failed: {}", def.get("id"), ex.getMessage());
                DashboardWidgetResultVO err = baseWidget(def);
                Map<String, Object> payload = new HashMap<>();
                payload.put("error", ex.getMessage());
                err.setPayload(payload);
                widgets.add(err);
            }
        }

        DashboardDataVO result = new DashboardDataVO();
        result.setDashboard(dashboard);
        result.setWidgets(widgets);
        return result;
    }

    private DashboardWidgetResultVO resolveWidget(Map<String, Object> def, String scope, Long tenantId, Long ipGroupId,
                                                  LocalDate start, LocalDate end, String platformType,
                                                  List<Long> accountIds, DashboardSqlParamBinder.BindParams sqlParams) {
        String sourceType = str(def, "sourceType", "BUILTIN");
        String type = str(def, "type", "KPI").toUpperCase();
        DashboardWidgetResultVO vo = baseWidget(def);

        if ("METRIC".equalsIgnoreCase(sourceType)) {
            vo.setPayload(resolveMetricPayload(def, sqlParams));
            return vo;
        }
        if ("QUERY".equalsIgnoreCase(sourceType)) {
            vo.setPayload(resolveQueryPayload(def, type, sqlParams));
            return vo;
        }

        String builtinKey = str(def, "builtinKey", "");
        if ("EXTERNAL".equalsIgnoreCase(scope)) {
            vo.setPayload(resolveExternalBuiltin(type, builtinKey, def, ipGroupId, start, end, platformType));
        } else {
            vo.setPayload(resolveInternalBuiltin(type, builtinKey, def, tenantId, ipGroupId, start, end, platformType, accountIds));
        }
        return vo;
    }

    private Map<String, Object> resolveInternalBuiltin(String type, String key, Map<String, Object> def, Long tenantId,
                                                       Long ipGroupId, LocalDate start, LocalDate end,
                                                       String platformType, List<Long> accountIds) {
        LocalDate today = LocalDate.now();
        LocalDate effectiveStart = "STAT".equals(type) ? today : start;
        LocalDate effectiveEnd = "STAT".equals(type) ? today : end;
        return switch (type) {
            case "KPI", "STAT" -> kpiPayload(resolveInternalKpiValue(key, tenantId, ipGroupId, effectiveStart, effectiveEnd, platformType, accountIds), null);
            case "CHART" -> chartPayload(str(def, "chartType", defaultChartType(key)), buildInternalChart(key, ipGroupId, start, end, platformType));
            case "LIST" -> {
                List<Map<String, Object>> columns = columnsFromDef(def, defaultListColumns(key));
                List<Map<String, Object>> rows = buildInternalList(key, def, tenantId, accountIds, start, end, platformType);
                yield listPayload(columns, filterRowColumns(rows, columns));
            }
            default -> Map.of("error", "未知组件类型 " + type);
        };
    }

    private Map<String, Object> resolveExternalBuiltin(String type, String key, Map<String, Object> def, Long ipGroupId,
                                                       LocalDate start, LocalDate end, String platformType) {
        LocalDate today = LocalDate.now();
        LocalDate effectiveStart = "STAT".equals(type) ? today : start;
        LocalDate effectiveEnd = "STAT".equals(type) ? today : end;
        return switch (type) {
            case "KPI", "STAT" -> kpiPayload(resolveExternalKpiValue(key, ipGroupId, effectiveStart, effectiveEnd, platformType), null);
            case "CHART" -> chartPayload(str(def, "chartType", "line"), buildExternalChart(key, ipGroupId, start, end));
            case "LIST" -> {
                List<Map<String, Object>> columns = columnsFromDef(def, defaultExternalListColumns(key));
                List<Map<String, Object>> rows = buildExternalList(key, def, ipGroupId, start, end, platformType);
                yield listPayload(columns, filterRowColumns(rows, columns));
            }
            default -> Map.of("error", "未知组件类型 " + type);
        };
    }

    private Object resolveInternalKpiValue(String key, Long tenantId, Long ipGroupId, LocalDate start, LocalDate end,
                                           String platformType, List<Long> accountIds) {
        DashboardHomeKpiVO kpi = homeDashboardService.getKpi(ipGroupId, start, end);
        LocalDate today = LocalDate.now();
        List<ContentDO> contents = queryContents(tenantId, accountIds, platformType, start.atStartOfDay(), end.plusDays(1).atStartOfDay());
        return switch (key) {
            case "work_count" -> (long) contents.size();
            case "like_count" -> contents.stream().mapToLong(c -> c.getLikeCount() == null ? 0 : c.getLikeCount()).sum();
            case "follower_growth" -> sumFollowerGrowth(tenantId, accountIds, start, end);
            case "read_count" -> contents.stream().mapToLong(c -> c.getReadCount() == null ? 0 : c.getReadCount()).sum();
            case "play_count" -> contents.stream().mapToLong(c -> c.getReadCount() == null ? 0 : c.getReadCount()).sum();
            case "today_content" -> countContentsBetween(tenantId, accountIds, platformType, today, today);
            case "today_follower" -> sumFollowerGrowth(tenantId, accountIds, today, today);
            case "today_orders" -> countOrdersBetween(tenantId, ipGroupId, today, today);
            case "today_order_amount" -> sumOrderAmount(tenantId, ipGroupId, today, today);
            case "pending_review" -> kpi.getPendingReviewCount();
            default -> 0L;
        };
    }

    private Object resolveExternalKpiValue(String key, Long ipGroupId, LocalDate start, LocalDate end, String platformType) {
        var external = monitorService.externalList(platformType, null, ipGroupId, null, start, end, 1, 1);
        var hit = monitorService.hitList(platformType, null, ipGroupId, start, end, 1, 1);
        var low = monitorService.lowScoreList(platformType, null, ipGroupId, start, end, 1, 1);
        var accounts = monitorService.highFollowerList(platformType, ipGroupId, 1, 1000);
        long totalPlay = monitorService.externalList(platformType, null, ipGroupId, null, start, end, 1, 500)
                .getList().stream().mapToLong(w -> w.getPlayCount() == null ? 0 : w.getPlayCount()).sum();
        long totalLike = monitorService.externalList(platformType, null, ipGroupId, null, start, end, 1, 500)
                .getList().stream().mapToLong(w -> w.getLikeCount() == null ? 0 : w.getLikeCount()).sum();
        return switch (key) {
            case "monitor_accounts" -> accounts.getTotal();
            case "external_works" -> external.getTotal();
            case "total_plays" -> totalPlay;
            case "total_likes" -> totalLike;
            case "hit_24h" -> hit.getTotal();
            case "low_score_24h" -> low.getTotal();
            default -> 0L;
        };
    }

    private Map<String, Object> buildInternalChart(String key, Long ipGroupId, LocalDate start, LocalDate end, String platformType) {
        return switch (key) {
            case "follower_trend" -> trendToMultiSeries(homeDashboardService.getTrend(ipGroupId, start, end, platformType, "FOLLOWER", "PLATFORM"), "line");
            case "read_trend" -> trendToMultiSeries(homeDashboardService.getTrend(ipGroupId, start, end, platformType, "CONTENT", "PLATFORM"), "bar");
            case "content_type_pie" -> pieFromContentType(ipGroupId, start, end, platformType);
            default -> Map.of("xAxis", List.of(), "series", List.of(), "legend", List.of());
        };
    }

    private Map<String, Object> buildExternalChart(String key, Long ipGroupId, LocalDate start, LocalDate end) {
        if ("ip_theme_trend".equals(key) && ipGroupId != null) {
            Map<String, Object> stats = monitorService.ipTheme(ipGroupId);
            List<String> xAxis = List.of("作品数", "总播放");
            List<Map<String, Object>> series = List.of(Map.of(
                    "name", "IP主题",
                    "type", "bar",
                    "data", List.of(stats.getOrDefault("workCount", 0), stats.getOrDefault("totalPlay", 0))
            ));
            Map<String, Object> chart = new HashMap<>();
            chart.put("xAxis", xAxis);
            chart.put("series", series);
            chart.put("legend", List.of("IP主题"));
            return chart;
        }
        return Map.of("xAxis", List.of(), "series", List.of(), "legend", List.of());
    }

    private List<Map<String, Object>> buildInternalList(String key, Map<String, Object> def, Long tenantId,
                                                        List<Long> accountIds, LocalDate start, LocalDate end,
                                                        String platformType) {
        if ("hit_works_24h".equals(key)) {
            String sortKey = str(def, "sortKey", "read_count");
            boolean asc = "ASC".equalsIgnoreCase(str(def, "sortOrder", "DESC"));
            int limit = intVal(def.get("limit"), 10);

            LambdaQueryWrapper<ContentDO> wrapper = new LambdaQueryWrapper<ContentDO>()
                    .eq(ContentDO::getTenantId, tenantId)
                    .in(!accountIds.isEmpty(), ContentDO::getAccountId, accountIds)
                    .eq(StrUtil.isNotBlank(platformType), ContentDO::getPlatformType, platformType)
                    .eq(ContentDO::getIsHit, 1)
                    .ge(ContentDO::getPublishTime, start.atStartOfDay())
                    .lt(ContentDO::getPublishTime, end.plusDays(1).atStartOfDay());
            applyListOrder(wrapper, sortKey, asc, ContentDO::getReadCount, ContentDO::getLikeCount, ContentDO::getTitle);
            wrapper.last("LIMIT " + limit);

            List<ContentDO> hits = contentMapper.selectList(wrapper);
            List<Map<String, Object>> rows = new ArrayList<>();
            int rank = 1;
            for (ContentDO c : hits) {
                Map<String, Object> row = new LinkedHashMap<>();
                row.put("rank", rank++);
                row.put("title", c.getTitle());
                row.put("read_count", c.getReadCount());
                row.put("like_count", c.getLikeCount());
                rows.add(row);
            }
            appendReadTrendPct(rows, "read_count");
            return rows;
        }
        return List.of();
    }

    private List<Map<String, Object>> buildExternalList(String key, Map<String, Object> def, Long ipGroupId,
                                                        LocalDate start, LocalDate end, String platformType) {
        int limit = intVal(def.get("limit"), 10);
        if ("high_follower_top10".equals(key)) {
            int rank = 1;
            List<Map<String, Object>> rows = new ArrayList<>();
            for (MonitorFollowerAccountVO vo : monitorService.highFollowerList(platformType, ipGroupId, 1, limit).getList()) {
                Map<String, Object> row = followerRow(vo);
                row.put("rank", rank++);
                rows.add(row);
            }
            return rows;
        }
        if ("external_hit_works".equals(key)) {
            int rank = 1;
            List<Map<String, Object>> rows = new ArrayList<>();
            for (ExternalWorkVO vo : monitorService.hitList(platformType, null, ipGroupId, start, end, 1, limit).getList()) {
                Map<String, Object> row = externalWorkRow(vo);
                row.put("rank", rank++);
                rows.add(row);
            }
            return rows;
        }
        return List.of();
    }

    private Map<String, Object> followerRow(MonitorFollowerAccountVO vo) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("account_name", vo.getAccountName());
        row.put("platform_type", vo.getPlatformType());
        row.put("follower_count", vo.getFollowerCount());
        return row;
    }

    private Map<String, Object> externalWorkRow(ExternalWorkVO vo) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("account_name", vo.getAccountId() != null ? String.valueOf(vo.getAccountId()) : "-");
        row.put("title", vo.getTitle());
        row.put("play_count", vo.getPlayCount());
        row.put("like_count", vo.getLikeCount());
        return row;
    }

    private Map<String, Object> resolveMetricPayload(Map<String, Object> def, DashboardSqlParamBinder.BindParams sqlParams) {
        Long metricId = longVal(def.get("metricId"));
        String valueKey = str(def, "valueKey", "metric_value");
        MetricDO metric = loadMetric(metricId);
        MetricPreviewReq req = new MetricPreviewReq();
        req.setMetricFormula(metric.getMetricFormula());
        MetricPreviewVO preview = analyticsMetricService.preview(req, sqlParams, def);
        Object value = 0;
        if (preview.getRows() != null && !preview.getRows().isEmpty()) {
            Map<String, Object> first = preview.getRows().get(0);
            value = first.getOrDefault(valueKey, first.values().stream().findFirst().orElse(0));
        }
        return kpiPayload(value, metric.getUnit());
    }

    private Map<String, Object> resolveQueryPayload(Map<String, Object> def, String type,
                                                    DashboardSqlParamBinder.BindParams sqlParams) {
        Long queryId = longVal(def.get("queryId"));
        if (queryId == null) {
            return Map.of("error", "缺少 queryId");
        }
        Map<String, Object> result = customQueryService.execute(queryId, sqlParams, def);
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> rows = result.get("rows") instanceof List
                ? (List<Map<String, Object>>) result.get("rows") : List.of();

        if ("CHART".equalsIgnoreCase(type)) {
            String xKey = str(def, "xKey", "stat_date");
            String yKey = str(def, "yKey", "value");
            String groupKey = str(def, "groupKey", "");
            String yAgg = str(def, "yAgg", "SUM");
            String chartType = str(def, "chartType", "line");
            if (StrUtil.isNotBlank(groupKey)) {
                return chartPayload(chartType, buildGroupedChart(rows, xKey, yKey, groupKey, yAgg, chartType, str(def, "title", "数据")));
            }
            if ("pie".equalsIgnoreCase(chartType)) {
                return chartPayload(chartType, buildPieChart(rows, xKey, yKey, yAgg, str(def, "title", "数据")));
            }
            List<String> xAxis = new ArrayList<>();
            List<Object> data = new ArrayList<>();
            for (Map<String, Object> row : rows) {
                xAxis.add(String.valueOf(row.getOrDefault(xKey, "")));
                data.add(row.get(yKey));
            }
            Map<String, Object> chart = new HashMap<>();
            chart.put("xAxis", xAxis);
            chart.put("series", List.of(Map.of(
                    "name", str(def, "title", "数据"),
                    "type", chartType,
                    "data", data
            )));
            chart.put("legend", List.of(str(def, "title", "数据")));
            return chartPayload(chartType, chart);
        }
        if ("LIST".equalsIgnoreCase(type)) {
            List<Map<String, Object>> columns = columnsFromDef(def, List.of());
            List<Map<String, Object>> sorted = applyListOptions(rows, def);
            return listPayload(columns, filterRowColumns(sorted, columns));
        }
        if ("KPI".equalsIgnoreCase(type) || "STAT".equalsIgnoreCase(type)) {
            String valueKey = str(def, "valueKey", yKeyFromRows(rows));
            Object value = rows.isEmpty() ? 0 : rows.get(0).getOrDefault(valueKey, 0);
            return kpiPayload(value, null);
        }
        return Map.of("rows", rows);
    }

    private String yKeyFromRows(List<Map<String, Object>> rows) {
        if (rows.isEmpty()) {
            return "value";
        }
        return rows.get(0).keySet().stream()
                .filter(k -> !"stat_date".equals(k) && !"id".equals(k))
                .findFirst().orElse("value");
    }

    private MetricDO loadMetric(Long metricId) {
        if (metricId == null) {
            throw new ServiceException(OaErrorCodes.ENTITY_NOT_EXISTS.getCode(), "metricId 不能为空");
        }
        MetricDO entity = metricMapper.selectById(metricId);
        if (entity == null) {
            throw new ServiceException(OaErrorCodes.ENTITY_NOT_EXISTS);
        }
        Long tenantId = TenantContextHolder.getTenantId();
        if (tenantId != null && !Objects.equals(entity.getTenantId(), tenantId)) {
            throw new ServiceException(OaErrorCodes.TENANT_FORBIDDEN);
        }
        return entity;
    }

    private Map<String, Object> parseLayout(String layoutJson) {
        if (StrUtil.isBlank(layoutJson)) {
            return Map.of("widgets", List.of());
        }
        try {
            return objectMapper.readValue(layoutJson, new TypeReference<Map<String, Object>>() {});
        } catch (Exception ex) {
            log.warn("[dashboard-data] invalid layout json: {}", ex.getMessage());
            return Map.of("widgets", List.of());
        }
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> extractWidgets(Map<String, Object> layoutRoot) {
        Object widgets = layoutRoot.get("widgets");
        if (widgets instanceof List<?> list) {
            List<Map<String, Object>> result = new ArrayList<>();
            for (Object item : list) {
                if (item instanceof Map<?, ?> map) {
                    result.add((Map<String, Object>) map);
                }
            }
            return result;
        }
        return List.of();
    }

    private List<Long> resolveAccountIds(Long tenantId, Long ipGroupId, String platformType) {
        LambdaQueryWrapper<AccountDO> wrapper = new LambdaQueryWrapper<AccountDO>()
                .eq(AccountDO::getTenantId, tenantId);
        if (ipGroupId != null) {
            wrapper.eq(AccountDO::getIpGroupId, ipGroupId);
        }
        if (StrUtil.isNotBlank(platformType)) {
            wrapper.eq(AccountDO::getPlatformType, platformType);
        }
        return accountMapper.selectList(wrapper).stream().map(AccountDO::getId).collect(Collectors.toList());
    }

    private List<ContentDO> queryContents(Long tenantId, List<Long> accountIds, String platformType,
                                          LocalDateTime start, LocalDateTime end) {
        return contentMapper.selectList(new LambdaQueryWrapper<ContentDO>()
                .eq(ContentDO::getTenantId, tenantId)
                .in(!accountIds.isEmpty(), ContentDO::getAccountId, accountIds)
                .eq(StrUtil.isNotBlank(platformType), ContentDO::getPlatformType, platformType)
                .ge(ContentDO::getPublishTime, start)
                .lt(ContentDO::getPublishTime, end));
    }

    private long countContentsBetween(Long tenantId, List<Long> accountIds, String platformType,
                                      LocalDate start, LocalDate end) {
        return queryContents(tenantId, accountIds, platformType,
                start.atStartOfDay(), end.plusDays(1).atStartOfDay()).size();
    }

    private long sumFollowerGrowth(Long tenantId, List<Long> accountIds, LocalDate start, LocalDate end) {
        if (accountIds.isEmpty()) {
            return 0L;
        }
        List<FollowerDailyDO> rows = followerDailyMapper.selectList(new LambdaQueryWrapper<FollowerDailyDO>()
                .eq(FollowerDailyDO::getTenantId, tenantId)
                .in(FollowerDailyDO::getAccountId, accountIds)
                .ge(FollowerDailyDO::getStatDate, start)
                .le(FollowerDailyDO::getStatDate, end));
        return rows.stream().mapToLong(r -> r.getNetGrowth() == null ? 0 : r.getNetGrowth()).sum();
    }

    private long countOrdersBetween(Long tenantId, Long ipGroupId, LocalDate start, LocalDate end) {
        LambdaQueryWrapper<OrderDO> wrapper = new LambdaQueryWrapper<OrderDO>()
                .eq(OrderDO::getTenantId, tenantId)
                .ge(OrderDO::getOrderTime, start.atStartOfDay())
                .lt(OrderDO::getOrderTime, end.plusDays(1).atStartOfDay());
        if (ipGroupId != null) {
            wrapper.eq(OrderDO::getIpGroupId, ipGroupId);
        }
        return orderMapper.selectCount(wrapper);
    }

    private BigDecimal sumOrderAmount(Long tenantId, Long ipGroupId, LocalDate start, LocalDate end) {
        LambdaQueryWrapper<OrderDO> wrapper = new LambdaQueryWrapper<OrderDO>()
                .eq(OrderDO::getTenantId, tenantId)
                .ge(OrderDO::getOrderTime, start.atStartOfDay())
                .lt(OrderDO::getOrderTime, end.plusDays(1).atStartOfDay());
        if (ipGroupId != null) {
            wrapper.eq(OrderDO::getIpGroupId, ipGroupId);
        }
        return orderMapper.selectList(wrapper).stream()
                .map(o -> o.getOrderAmount() == null ? BigDecimal.ZERO : o.getOrderAmount())
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);
    }

    private Map<String, Object> trendToMultiSeries(List<TrendPointVO> points, String seriesType) {
        Set<String> dates = new LinkedHashSet<>();
        Map<String, Map<String, Long>> seriesMap = new LinkedHashMap<>();
        for (TrendPointVO point : points) {
            String date = point.getDate();
            String platform = point.getPlatform() != null ? point.getPlatform() : "全部";
            dates.add(date);
            seriesMap.computeIfAbsent(platform, k -> new LinkedHashMap<>())
                    .put(date, point.getCount() == null ? 0L : point.getCount());
        }
        List<String> xAxis = new ArrayList<>(dates);
        List<Map<String, Object>> series = new ArrayList<>();
        List<String> legend = new ArrayList<>();
        String type = seriesType != null ? seriesType : "line";
        for (Map.Entry<String, Map<String, Long>> entry : seriesMap.entrySet()) {
            legend.add(entry.getKey());
            List<Long> data = new ArrayList<>();
            for (String date : xAxis) {
                data.add(entry.getValue().getOrDefault(date, 0L));
            }
            Map<String, Object> serie = new HashMap<>();
            serie.put("name", entry.getKey());
            serie.put("type", type);
            serie.put("data", data);
            series.add(serie);
        }
        Map<String, Object> chart = new HashMap<>();
        chart.put("xAxis", xAxis);
        chart.put("series", series);
        chart.put("legend", legend);
        return chart;
    }

    private Map<String, Object> pieFromContentType(Long ipGroupId, LocalDate start, LocalDate end, String platformType) {
        Long tenantId = TenantContextHolder.getTenantId();
        List<Long> accountIds = resolveAccountIds(tenantId, ipGroupId, platformType);
        List<ContentDO> contents = queryContents(tenantId, accountIds, platformType,
                start.atStartOfDay(), end.plusDays(1).atStartOfDay());
        Map<String, Long> grouped = contents.stream()
                .collect(Collectors.groupingBy(
                        c -> c.getContentType() != null ? c.getContentType() : "OTHER",
                        Collectors.counting()));
        List<Map<String, Object>> pieData = grouped.entrySet().stream()
                .map(e -> {
                    Map<String, Object> item = new LinkedHashMap<>();
                    item.put("name", e.getKey());
                    item.put("value", e.getValue());
                    return item;
                })
                .collect(Collectors.toList());
        Map<String, Object> chart = new HashMap<>();
        chart.put("xAxis", List.of());
        chart.put("series", List.of(Map.of("name", "内容类型", "type", "pie", "data", pieData)));
        chart.put("legend", grouped.keySet());
        return chart;
    }

    private Map<String, Object> kpiPayload(Object value, String unit) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("value", value);
        if (StrUtil.isNotBlank(unit)) {
            payload.put("unit", unit);
        }
        return payload;
    }

    private Map<String, Object> chartPayload(String chartType, Map<String, Object> chart) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("chartType", chartType);
        payload.put("chart", chart);
        return payload;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> listPayload(List<Map<String, Object>> columns, List<Map<String, Object>> rows) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("columns", columns);
        payload.put("rows", rows);
        return payload;
    }

    private DashboardWidgetResultVO baseWidget(Map<String, Object> def) {
        DashboardWidgetResultVO vo = new DashboardWidgetResultVO();
        vo.setId(str(def, "id", ""));
        vo.setType(str(def, "type", "KPI").toUpperCase());
        vo.setTitle(str(def, "title", ""));
        return vo;
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> columnsFromDef(Map<String, Object> def, List<Map<String, Object>> defaults) {
        Object columns = def.get("columns");
        if (columns instanceof List<?> list && !list.isEmpty()) {
            List<Map<String, Object>> result = new ArrayList<>();
            for (Object item : list) {
                if (item instanceof Map<?, ?> map) {
                    result.add((Map<String, Object>) map);
                }
            }
            if (!result.isEmpty()) {
                return result;
            }
        }
        return new ArrayList<>(defaults);
    }

    private List<Map<String, Object>> filterRowColumns(List<Map<String, Object>> rows,
                                                       List<Map<String, Object>> columns) {
        if (rows == null || rows.isEmpty() || columns == null || columns.isEmpty()) {
            return rows == null ? List.of() : rows;
        }
        Set<String> keys = columns.stream()
                .map(col -> String.valueOf(col.get("key")))
                .collect(Collectors.toCollection(LinkedHashSet::new));
        List<Map<String, Object>> filtered = new ArrayList<>();
        for (Map<String, Object> row : rows) {
            Map<String, Object> out = new LinkedHashMap<>();
            for (String key : keys) {
                if (row.containsKey(key)) {
                    out.put(key, row.get(key));
                }
            }
            filtered.add(out);
        }
        return filtered;
    }

    private List<Map<String, Object>> applyListOptions(List<Map<String, Object>> rows, Map<String, Object> def) {
        if (rows == null || rows.isEmpty()) {
            return List.of();
        }
        String sortKey = str(def, "sortKey", "");
        boolean asc = "ASC".equalsIgnoreCase(str(def, "sortOrder", "DESC"));
        List<Map<String, Object>> sorted = new ArrayList<>(rows);
        if (StrUtil.isNotBlank(sortKey)) {
            sorted.sort((a, b) -> {
                Object va = a.get(sortKey);
                Object vb = b.get(sortKey);
                int cmp = compareValues(va, vb);
                return asc ? cmp : -cmp;
            });
        }
        int limit = intVal(def.get("limit"), sorted.size());
        if (limit > 0 && limit < sorted.size()) {
            return new ArrayList<>(sorted.subList(0, limit));
        }
        return sorted;
    }

    private int compareValues(Object a, Object b) {
        if (a == null && b == null) {
            return 0;
        }
        if (a == null) {
            return -1;
        }
        if (b == null) {
            return 1;
        }
        if (a instanceof Number na && b instanceof Number nb) {
            return Double.compare(na.doubleValue(), nb.doubleValue());
        }
        return String.valueOf(a).compareTo(String.valueOf(b));
    }

    private Map<String, Object> buildGroupedChart(List<Map<String, Object>> rows, String xKey, String yKey,
                                                    String groupKey, String yAgg, String chartType, String title) {
        Set<String> xAxisSet = new LinkedHashSet<>();
        Map<String, Map<String, Double>> groupData = new LinkedHashMap<>();
        for (Map<String, Object> row : rows) {
            String x = String.valueOf(row.getOrDefault(xKey, ""));
            String group = String.valueOf(row.getOrDefault(groupKey, "全部"));
            double val = toDouble(row.get(yKey));
            xAxisSet.add(x);
            groupData.computeIfAbsent(group, k -> new LinkedHashMap<>())
                    .merge(x, val, (oldVal, newVal) -> oldVal + newVal);
        }
        List<String> xAxis = new ArrayList<>(xAxisSet);
        List<Map<String, Object>> series = new ArrayList<>();
        List<String> legend = new ArrayList<>();
        for (Map.Entry<String, Map<String, Double>> entry : groupData.entrySet()) {
            legend.add(entry.getKey());
            List<Object> data = new ArrayList<>();
            for (String x : xAxis) {
                data.add(entry.getValue().getOrDefault(x, 0D));
            }
            Map<String, Object> serie = new HashMap<>();
            serie.put("name", entry.getKey());
            serie.put("type", chartType);
            serie.put("data", data);
            series.add(serie);
        }
        Map<String, Object> chart = new HashMap<>();
        chart.put("xAxis", xAxis);
        chart.put("series", series);
        chart.put("legend", legend);
        return chart;
    }

    private Map<String, Object> buildPieChart(List<Map<String, Object>> rows, String xKey, String yKey,
                                              String yAgg, String title) {
        Map<String, Double> grouped = new LinkedHashMap<>();
        for (Map<String, Object> row : rows) {
            String name = String.valueOf(row.getOrDefault(xKey, ""));
            double val = toDouble(row.get(yKey));
            grouped.merge(name, val, Double::sum);
        }
        List<Map<String, Object>> pieData = grouped.entrySet().stream()
                .map(e -> {
                    Map<String, Object> item = new LinkedHashMap<>();
                    item.put("name", e.getKey());
                    item.put("value", e.getValue());
                    return item;
                })
                .collect(Collectors.toList());
        Map<String, Object> chart = new HashMap<>();
        chart.put("xAxis", List.of());
        chart.put("series", List.of(Map.of("name", title, "type", "pie", "data", pieData)));
        chart.put("legend", grouped.keySet());
        return chart;
    }

    private double toDouble(Object val) {
        if (val == null) {
            return 0D;
        }
        if (val instanceof Number number) {
            return number.doubleValue();
        }
        try {
            return Double.parseDouble(String.valueOf(val));
        } catch (NumberFormatException ex) {
            return 0D;
        }
    }

    private int intVal(Object val, int defaultVal) {
        Long parsed = longVal(val);
        if (parsed == null || parsed <= 0) {
            return defaultVal;
        }
        return parsed.intValue();
    }

    private void applyListOrder(LambdaQueryWrapper<ContentDO> wrapper, String sortKey, boolean asc,
                                com.baomidou.mybatisplus.core.toolkit.support.SFunction<ContentDO, ?> readCount,
                                com.baomidou.mybatisplus.core.toolkit.support.SFunction<ContentDO, ?> likeCount,
                                com.baomidou.mybatisplus.core.toolkit.support.SFunction<ContentDO, ?> title) {
        if ("like_count".equals(sortKey)) {
            if (asc) {
                wrapper.orderByAsc(likeCount);
            } else {
                wrapper.orderByDesc(likeCount);
            }
            return;
        }
        if ("title".equals(sortKey)) {
            if (asc) {
                wrapper.orderByAsc(title);
            } else {
                wrapper.orderByDesc(title);
            }
            return;
        }
        if (asc) {
            wrapper.orderByAsc(readCount);
        } else {
            wrapper.orderByDesc(readCount);
        }
    }

    private String defaultChartType(String key) {
        return switch (key) {
            case "read_trend" -> "bar";
            case "content_type_pie" -> "pie";
            default -> "line";
        };
    }

    private List<Map<String, Object>> defaultListColumns(String key) {
        if ("hit_works_24h".equals(key)) {
            return List.of(
                    Map.of("key", "rank", "label", "排名"),
                    Map.of("key", "title", "label", "作品标题"),
                    Map.of("key", "read_count", "label", "阅读量"),
                    Map.of("key", "trend_pct", "label", "趋势")
            );
        }
        return List.of();
    }

    private List<Map<String, Object>> defaultExternalListColumns(String key) {
        if ("high_follower_top10".equals(key)) {
            return List.of(
                    Map.of("key", "rank", "label", "排名"),
                    Map.of("key", "account_name", "label", "账号名称"),
                    Map.of("key", "platform_type", "label", "平台"),
                    Map.of("key", "follower_count", "label", "粉丝数")
            );
        }
        if ("external_hit_works".equals(key)) {
            return List.of(
                    Map.of("key", "rank", "label", "排名"),
                    Map.of("key", "account_name", "label", "账号"),
                    Map.of("key", "title", "label", "作品标题"),
                    Map.of("key", "play_count", "label", "阅读量"),
                    Map.of("key", "like_count", "label", "点赞")
            );
        }
        return List.of();
    }

    private void appendReadTrendPct(List<Map<String, Object>> rows, String readKey) {
        if (rows == null || rows.isEmpty()) {
            return;
        }
        double avgRead = rows.stream()
                .mapToLong(row -> longVal(row.get(readKey)) != null ? longVal(row.get(readKey)) : 0L)
                .average()
                .orElse(0D);
        for (Map<String, Object> row : rows) {
            long read = longVal(row.get(readKey)) != null ? longVal(row.get(readKey)) : 0L;
            double trend = avgRead > 0 ? ((read - avgRead) / avgRead) * 100D : 0D;
            row.put("trend_pct", Math.round(trend * 10D) / 10D);
        }
    }

    private String str(Map<String, Object> def, String key, String defaultVal) {
        Object val = def.get(key);
        return val != null ? String.valueOf(val) : defaultVal;
    }

    private Long longVal(Object val) {
        if (val == null) {
            return null;
        }
        if (val instanceof Number number) {
            return number.longValue();
        }
        try {
            return Long.parseLong(String.valueOf(val));
        } catch (NumberFormatException ex) {
            return null;
        }
    }
}
