package cn.iocoder.yudao.module.oa.service.analytics;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.oa.api.dto.analytics.MetricCreateReq;
import cn.iocoder.yudao.module.oa.api.dto.analytics.MetricPreviewReq;
import cn.iocoder.yudao.module.oa.api.dto.analytics.MetricPreviewVO;
import cn.iocoder.yudao.module.oa.api.dto.analytics.MetricUpdateReq;
import cn.iocoder.yudao.module.oa.api.dto.analytics.MetricVO;
import cn.iocoder.yudao.module.oa.service.support.DashboardSqlParamBinder;

import java.util.Map;

public interface AnalyticsMetricService {

    PageResult<MetricVO> list(String metricType, Integer pageNum, Integer pageSize);

    Long create(MetricCreateReq req);

    void update(MetricUpdateReq req);

    void delete(Long id);

    MetricPreviewVO preview(MetricPreviewReq req);

    MetricPreviewVO preview(MetricPreviewReq req, DashboardSqlParamBinder.BindParams bindParams);

    MetricPreviewVO preview(MetricPreviewReq req, DashboardSqlParamBinder.BindParams bindParams,
                            Map<String, Object> widgetDef);
}
