package cn.iocoder.yudao.module.oa.api.dto.config;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class AiModelConfigRespVO {

    private Long id;
    private String modelName;
    private String modelType;
    private String apiEndpoint;
    private String apiKeyMasked;
    private Integer maxTokens;
    private BigDecimal temperature;
    private BigDecimal topP;
    private String status;
    private String remark;
    private LocalDateTime createTime;
}
