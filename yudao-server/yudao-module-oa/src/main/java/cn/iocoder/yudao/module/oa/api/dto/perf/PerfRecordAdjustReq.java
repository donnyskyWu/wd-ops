package cn.iocoder.yudao.module.oa.api.dto.perf;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class PerfRecordAdjustReq {

    @NotNull
    private Long itemRecordId;
    @NotNull
    private BigDecimal manualAdjustment;
    private String remark;
}
