package cn.iocoder.yudao.module.oa.api.dto.content;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class LayoutMergePreviewReq {

    @NotBlank
    private String body;
    private Object existingLayoutJson;
}
