package cn.iocoder.yudao.module.oa.api.dto.aicontent;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class AiContentAdoptRespVO {

    private String sessionId;
    private Long contentId;
    private LocalDateTime adoptedAt;
}
