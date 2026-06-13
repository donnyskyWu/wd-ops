package cn.iocoder.yudao.module.oa.api.dto.sop;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class TaskVO {

    private Long id;
    private Long templateId;
    private Long nodeId;
    private String competitionId;
    /** 赛事名称（计划详情展示用，由后端解析） */
    private String competitionName;
    private String planName;
    private String nodeName;
    private Long assigneeId;
    private String assigneeName;
    private String executorRole;
    /** 执行岗位名称（dict_position 标签） */
    private String executorRoleText;
    private String status;
    private Integer needReview;
    private LocalDateTime slaDeadline;
    private String deliverables;
    /** 计划开始时间（来自计划步骤 scheduledStart） */
    private LocalDateTime scheduledStart;
    /** 计划结束时间（来自计划步骤 scheduledEnd） */
    private LocalDateTime scheduledEnd;
    private LocalDateTime startTime;
    private LocalDateTime completeTime;
}
