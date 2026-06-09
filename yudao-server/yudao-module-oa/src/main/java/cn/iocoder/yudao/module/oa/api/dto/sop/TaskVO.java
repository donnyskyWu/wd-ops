package cn.iocoder.yudao.module.oa.api.dto.sop;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class TaskVO {

    private Long id;
    private Long templateId;
    private Long nodeId;
    private String planName;
    private String nodeName;
    private Long assigneeId;
    private String assigneeName;
    private String executorRole;
    private String status;
    private Integer needReview;
    private LocalDateTime slaDeadline;
    private String deliverables;
    private LocalDateTime startTime;
    private LocalDateTime completeTime;
}
