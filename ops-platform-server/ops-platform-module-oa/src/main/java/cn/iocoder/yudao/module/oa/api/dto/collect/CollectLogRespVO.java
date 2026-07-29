package cn.iocoder.yudao.module.oa.api.dto.collect;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class CollectLogRespVO {

    private Long id;
    private Long taskId;
    private String taskName;
    /** 关联采集任务账号名称（列表展示） */
    private String accountName;
    private String status;
    private LocalDateTime startAt;
    private Long durationMs;
    private Integer recordCount;
    private String errorMessage;
    private Integer retryCount;
}
