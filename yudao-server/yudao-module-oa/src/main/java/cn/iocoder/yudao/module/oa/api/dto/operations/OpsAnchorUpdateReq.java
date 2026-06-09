package cn.iocoder.yudao.module.oa.api.dto.operations;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

@Data
public class OpsAnchorUpdateReq {

    @NotNull
    private Long id;
    private Long opsUserId;
    private Long anchorUserId;
    private Long ipGroupId;
    private LocalDate startDate;
    private LocalDate endDate;
}
