package cn.iocoder.yudao.module.oa.api.dto.plan;

import lombok.Data;

@Data
public class ContentPlanTaskPreviewVO {

    private Long nodeId;
    private String nodeName;
    private Integer nodeOrder;
    private String executorRole;
    private String competitionId;
    private String competitionName;
    private Long assigneeId;
    private String assigneeName;
    /** 是否因岗位无匹配而回退到 IP 组长 */
    private Boolean assigneeFallback;
    /** 岗位在 IP 组内无成员时的提示 */
    private String positionWarning;
    private String scheduledStart;
    private String scheduledEnd;
}
