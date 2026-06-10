package cn.iocoder.yudao.module.oa.api.dto.plan;

import lombok.Data;

import java.util.List;

@Data
public class ContentPlanStepVO {

    private Long nodeId;
    private String nodeName;
    private Integer nodeOrder;
    private String executorRole;
    private List<Long> assigneeIds;
    private String scheduledStart;
    private String scheduledEnd;
}
