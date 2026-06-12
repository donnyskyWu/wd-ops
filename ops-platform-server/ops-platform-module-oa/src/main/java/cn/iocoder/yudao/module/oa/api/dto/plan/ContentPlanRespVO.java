package cn.iocoder.yudao.module.oa.api.dto.plan;

import lombok.Data;

import java.util.List;

@Data
public class ContentPlanRespVO {

    private Long id;
    private String planName;
    private Long templateId;
    private String templateName;
    private Long ipGroupId;
    private String ipGroupName;
    private String startDate;
    private String endDate;
    private String description;
    private String status;
    private Integer progress;
    private List<ContentPlanCompetitionVO> competitions;
    private List<ContentPlanStepVO> steps;
    private String createTime;
}
