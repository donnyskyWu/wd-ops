package cn.iocoder.yudao.module.oa.api.dto.sop;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class SopReviewVO {

    private Long id;
    private Long taskId;
    private String planName;
    private String nodeName;
    private Long reviewerId;
    private String reviewerRole;
    private String status;
    private String comment;
    private LocalDateTime createTime;
}
