package cn.iocoder.yudao.module.oa.api.dto.config;

import cn.iocoder.yudao.module.oa.framework.dict.InDict;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class AiPromptConfigCreateReq {

    @NotBlank
    @Size(max = 128)
    private String templateName;

    @InDict("dict_ai_scene")
    private String scene;

    @InDict("dict_content_type")
    private String contentType;

    @InDict("dict_document_type")
    private String documentType;

    @NotBlank
    private String promptContent;

    private String variableDesc;
    private BigDecimal temperature;

    @InDict("dict_config_status")
    private String status;

    @Size(max = 512)
    private String remark;
}
