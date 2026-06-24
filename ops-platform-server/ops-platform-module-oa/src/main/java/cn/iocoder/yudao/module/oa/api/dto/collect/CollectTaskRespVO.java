package cn.iocoder.yudao.module.oa.api.dto.collect;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class CollectTaskRespVO {

    private Long id;
    private String name;
    private String platformType;
    private Long accountId;
    private String accountName;
    private String method;
    private String source;
    private String dataType;
    private String frequency;
    private String cron;
    /** 脱敏占位；有配置时返回 ****** */
    private String apiConfig;
    private String status;
    private LocalDateTime lastRunAt;
    private LocalDateTime nextRunAt;
    private Integer runCount;
    private Integer failCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
