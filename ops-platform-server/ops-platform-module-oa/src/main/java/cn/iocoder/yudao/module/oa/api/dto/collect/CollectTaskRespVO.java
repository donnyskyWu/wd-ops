package cn.iocoder.yudao.module.oa.api.dto.collect;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class CollectTaskRespVO {

    private Long id;
    private String name;
    private String platformType;
    private Long accountId;
    /** 微信公号：shenyu-mp.mp_account.id（S3 bind 语义） */
    private Long mpAccountId;
    /** 非微信：wd.oa_account.id（S3 bind 语义） */
    private Long oaAccountId;
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
