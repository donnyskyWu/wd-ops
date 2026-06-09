package cn.iocoder.yudao.module.oa.api.dto.config;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class AiPromptConfigRespVO {

    private Long id;
    private String templateName;
    private String scene;
    private String promptContent;
    private String variableDesc;
    private BigDecimal temperature;
    private String status;
    private String remark;
    private LocalDateTime createTime;
}
