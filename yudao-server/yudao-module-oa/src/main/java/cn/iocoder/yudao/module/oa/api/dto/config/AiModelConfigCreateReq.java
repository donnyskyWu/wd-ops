package cn.iocoder.yudao.module.oa.api.dto.config;

import cn.iocoder.yudao.module.oa.framework.dict.InDict;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class AiModelConfigCreateReq {

    @NotBlank
    @Size(max = 128)
    private String modelName;

    @Size(max = 100)
    private String modelId;

    @InDict("dict_ai_model_type")
    private String modelType;

    @Size(max = 512)
    private String apiEndpoint;

    @Size(max = 512)
    private String apiKey;

    private Integer maxTokens;
    private Integer timeout;
    private Boolean isDefault;
    private BigDecimal temperature;
    private BigDecimal topP;

    @InDict("dict_config_status")
    private String status;

    @Size(max = 512)
    private String remark;
}
