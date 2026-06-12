package cn.iocoder.yudao.module.oa.api.dto.perf;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class PerfRecordConfirmReq {

    @NotNull
    private Long id;
}
