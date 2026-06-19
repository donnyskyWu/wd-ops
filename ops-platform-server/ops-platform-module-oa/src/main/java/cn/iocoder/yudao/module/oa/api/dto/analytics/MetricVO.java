package cn.iocoder.yudao.module.oa.api.dto.analytics;

import lombok.Data;

@Data
public class MetricVO {

    private Long id;
    private String metricName;
    private String metricCode;
    private String metricType;
    private String unit;
    private String category;
    private String metricFormula;
    private String dataSource;
    private String paramsJson;
    private Integer status;
    private String description;
}
