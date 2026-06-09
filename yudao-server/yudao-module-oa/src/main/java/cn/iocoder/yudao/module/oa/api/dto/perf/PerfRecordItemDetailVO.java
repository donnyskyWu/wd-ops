package cn.iocoder.yudao.module.oa.api.dto.perf;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class PerfRecordItemDetailVO {

    private Long id;
    private String metricName;
    private BigDecimal weight;
    private BigDecimal metricValue;
    private BigDecimal score;
    private BigDecimal manualAdjustment;
    private BigDecimal finalScore;
}
