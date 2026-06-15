package cn.iocoder.yudao.module.oa.api.dto.content;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class LayoutTemplateUpdateReq extends LayoutTemplateCreateReq {

    @NotNull
    private Long id;
}
