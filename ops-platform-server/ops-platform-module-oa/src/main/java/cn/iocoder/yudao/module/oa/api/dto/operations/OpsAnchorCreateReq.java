package cn.iocoder.yudao.module.oa.api.dto.operations;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

@Data
public class OpsAnchorCreateReq {

    @NotNull
    private Long opsUserId;
    @NotNull
    private Long anchorUserId;
    private Long ipGroupId;
    @NotNull
    private LocalDate startDate;
    @NotNull
    private LocalDate endDate;
}
