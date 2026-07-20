package cn.iocoder.yudao.module.oa.api.dto.aicontent;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class AiContentGenerateRespVO {

    private String sessionId;
    private String content;
    private Long modelId;
    private Integer roundCount;
    private Integer tokensUsed;
    private LocalDateTime generatedAt;
    private Boolean mock;
    private String message;
}
