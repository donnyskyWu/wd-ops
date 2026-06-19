package cn.iocoder.yudao.module.oa.service.perf;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.module.oa.api.dto.analytics.MetricPreviewReq;
import cn.iocoder.yudao.module.oa.api.dto.analytics.MetricPreviewVO;
import cn.iocoder.yudao.module.oa.dal.dataobject.perf.MetricDO;
import cn.iocoder.yudao.module.oa.dal.dataobject.perf.PerfRecordDO;
import cn.iocoder.yudao.module.oa.service.analytics.AnalyticsMetricService;
import cn.iocoder.yudao.module.oa.service.support.DashboardSqlParamBinder;
import cn.iocoder.yudao.module.oa.service.support.MetricSqlBuilder;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Map;

/**
 * 绩效考核：按被考核人 + 考核周期绑定指标参数并执行指标公式。
 */
@Component
@RequiredArgsConstructor
public class PerfMetricValueResolver {

    private final AnalyticsMetricService analyticsMetricService;

    public BigDecimal resolve(MetricDO metric, PerfRecordDO record) {
        if (metric == null) {
            return BigDecimal.ZERO;
        }
        if (StrUtil.isBlank(metric.getMetricFormula())) {
            return BigDecimal.ZERO;
        }

        Map<String, String> bindParams = MetricSqlBuilder.buildPerfBindParams(
                metric.getParamsJson(),
                record.getTargetUserId(),
                record.getPeriodStart(),
                record.getPeriodEnd());

        String sql = MetricSqlBuilder.buildRuntimeMetricSql(
                metric.getMetricFormula(),
                metric.getParamsJson(),
                bindParams);

        MetricPreviewReq req = new MetricPreviewReq();
        req.setMetricFormula(sql);
        req.setBindParams(bindParams);

        DashboardSqlParamBinder.BindParams tenantParams = DashboardSqlParamBinder.BindParams.of(
                record.getTenantId(),
                record.getPeriodStart(),
                record.getPeriodEnd(),
                record.getIpGroupId(),
                null);

        MetricPreviewVO preview = analyticsMetricService.preview(req, tenantParams);
        return extractMetricValue(preview);
    }

    private BigDecimal extractMetricValue(MetricPreviewVO preview) {
        if (preview == null || preview.getRows() == null || preview.getRows().isEmpty()) {
            return BigDecimal.ZERO;
        }
        Map<String, Object> row = preview.getRows().get(0);
        Object value = row.get("metric_value");
        if (value == null) {
            value = row.values().stream().findFirst().orElse(null);
        }
        if (value == null) {
            return BigDecimal.ZERO;
        }
        if (value instanceof BigDecimal bd) {
            return bd;
        }
        if (value instanceof Number num) {
            return BigDecimal.valueOf(num.doubleValue());
        }
        try {
            return new BigDecimal(String.valueOf(value));
        } catch (NumberFormatException ignored) {
            return BigDecimal.ZERO;
        }
    }
}
