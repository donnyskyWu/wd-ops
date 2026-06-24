package cn.iocoder.yudao.module.oa.api.dto.collect;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class CollectorAccountBindRespVO {

    private Long id;
    private Long oaAccountId;
    private String collectorAccountId;
    private String platformType;
    private String bindStatus;
    private String connStatus;
    private LocalDateTime lastBindAt;
    private LocalDateTime lastHealthCheckAt;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
