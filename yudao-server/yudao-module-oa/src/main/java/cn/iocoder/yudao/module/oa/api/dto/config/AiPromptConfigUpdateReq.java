package cn.iocoder.yudao.module.oa.api.dto.config;

import cn.iocoder.yudao.module.oa.framework.dict.InDict;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class AiPromptConfigUpdateReq {

    @NotNull
    private Long id;

    @Size(max = 128)
    private String templateName;

    @InDict("dict_ai_scene")
    private String scene;

    private String promptContent;
    private String variableDesc;
    private BigDecimal temperature;

    @InDict("dict_config_status")
    private String status;

    @Size(max = 512)
    private String remark;
}
