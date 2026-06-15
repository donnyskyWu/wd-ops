package cn.iocoder.yudao.module.oa.api.dto.content;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ContentApplyLayoutTemplateReq {

    @NotNull
    private Long layoutTemplateId;
    private Boolean overwrite;
}
