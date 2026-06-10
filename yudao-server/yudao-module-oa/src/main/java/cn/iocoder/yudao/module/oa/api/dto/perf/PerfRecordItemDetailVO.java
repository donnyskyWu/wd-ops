package cn.iocoder.yudao.module.oa.api.dto.perf;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class PerfRecordItemDetailVO {

    private Long id;
    private String metricName;
    private String metricCode;
    private BigDecimal weight;
    private BigDecimal metricValue;
    /** 与 metricValue 同义，供前端 actualValue 映射 */
    private BigDecimal actualValue;
    /** 评分标准上限，供完成率展示 */
    private BigDecimal target;
    private BigDecimal score;
    private BigDecimal manualAdjustment;
    private BigDecimal finalScore;
}
