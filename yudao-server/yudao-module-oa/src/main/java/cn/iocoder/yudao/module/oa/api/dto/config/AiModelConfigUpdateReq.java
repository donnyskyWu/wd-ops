package cn.iocoder.yudao.module.oa.api.dto.config;

import cn.iocoder.yudao.module.oa.framework.dict.InDict;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class AiModelConfigUpdateReq {

    @NotNull
    private Long id;

    @Size(max = 128)
    private String modelName;

    @InDict("dict_ai_model_type")
    private String modelType;

    @Size(max = 512)
    private String apiEndpoint;

    @Size(max = 512)
    private String apiKey;

    private Integer maxTokens;
    private BigDecimal temperature;
    private BigDecimal topP;

    @InDict("dict_config_status")
    private String status;

    @Size(max = 512)
    private String remark;
}
