package cn.iocoder.yudao.module.oa.api.dto.operations;

import lombok.Data;

@Data
public class ProductivityReviewVO {

    private Long userId;
    private String userName;
    private String position;
    private Long ipGroupId;
    private String ipGroupName;
    private Integer taskCompleted = 0;
    private Integer taskInProgress = 0;
    private Integer taskOverdue = 0;
    private Double completionRate = 0.0;
    private Double roi = 0.0;
    private Integer contentOutput = 0;
}
