package cn.iocoder.yudao.module.oa.api.dto.operations;

import lombok.Data;

@Data
public class ProductivityReviewVO {

    private Long userId;
    private String userName;
    private String position;
    private Long ipGroupId;
    private String ipGroupName;

    // S-R9: 任务 KPI（按 oa_task.assignee_id 聚合）
    private Integer taskTotal = 0;
    private Integer taskCompleted = 0;
    private Integer taskInProgress = 0;
    private Integer taskOverdue = 0;
    private Double completionRate = 0.0;

    // S-R9: 财务 KPI（按 oa_order_attribution.ops_user_id 聚合）
    private Double costAmount = 0.0;
    private Double revenue = 0.0;
    private Double roi = 0.0;
    private Integer orderCount = 0;

    // S-R21-Mike / ADR-008：内容 KPI（按 oa_content.author_id 聚合）
    private Integer contentOutput = 0;
    private Long avgRead = 0L;
    private Long avgPlay = 0L;
    private Integer hitCount = 0;
}
