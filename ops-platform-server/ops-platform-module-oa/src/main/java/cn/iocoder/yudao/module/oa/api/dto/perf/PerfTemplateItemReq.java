package cn.iocoder.yudao.module.oa.api.dto.perf;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class PerfTemplateItemReq {

    @NotNull
    private Long metricId;
    @NotNull
    private BigDecimal weight;
    private String calcRule;
    @NotNull
    private ScoreStandardDTO scoreStandard;
}
