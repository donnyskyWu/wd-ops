package cn.iocoder.yudao.module.oa.api.dto.config;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class AiModelConfigRespVO {

    private Long id;
    private String modelName;
    private String modelId;
    private String modelType;
    private String apiEndpoint;
    private String apiKeyMasked;
    private Integer maxTokens;
    private Integer timeout;
    private Boolean isDefault;
    private String connStatus;
    private BigDecimal temperature;
    private BigDecimal topP;
    private String status;
    private String remark;
    private LocalDateTime createTime;
}
