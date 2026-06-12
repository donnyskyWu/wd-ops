package cn.iocoder.yudao.module.oa.api.dto.perf;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class PerfTemplateItemVO {

    private Long id;
    private Long metricId;
    private String metricName;
    private BigDecimal weight;
    private String calcRule;
    private ScoreStandardDTO scoreStandard;
}
