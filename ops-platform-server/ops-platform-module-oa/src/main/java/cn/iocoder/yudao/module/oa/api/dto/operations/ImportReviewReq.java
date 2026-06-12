package cn.iocoder.yudao.module.oa.api.dto.operations;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ImportReviewReq {

    @NotNull
    private Integer reviewStatus;
    private String remark;
}
