package cn.iocoder.yudao.module.oa.api.dto.analytics;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CustomQueryPreviewReq {

    @NotBlank
    private String sqlText;
}
