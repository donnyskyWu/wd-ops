package cn.iocoder.yudao.module.oa.api.dto.sop;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class SopReviewActionReq {

    @NotNull
    private Long reviewId;
    private String comment;
}
