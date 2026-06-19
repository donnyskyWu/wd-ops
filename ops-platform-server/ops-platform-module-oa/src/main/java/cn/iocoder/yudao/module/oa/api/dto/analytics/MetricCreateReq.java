package cn.iocoder.yudao.module.oa.api.dto.analytics;

import cn.iocoder.yudao.module.oa.framework.dict.InDict;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class MetricCreateReq {

    @NotBlank
    private String metricName;

    @NotBlank
    private String metricCode;

    @InDict("dict_perf_metric_type")
    private String metricType;

    private String unit;
    private String description;
    private String metricFormula;
    private String dataSource;
    private String paramsJson;
}
