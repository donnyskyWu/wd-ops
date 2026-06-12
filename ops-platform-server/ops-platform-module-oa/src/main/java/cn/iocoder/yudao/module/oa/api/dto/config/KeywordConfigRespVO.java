package cn.iocoder.yudao.module.oa.api.dto.config;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class KeywordConfigRespVO {

    private Long id;
    private String platform;
    private String keyword;
    private String matchType;
    private String status;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
