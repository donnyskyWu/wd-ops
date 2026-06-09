package cn.iocoder.yudao.module.oa.api.dto.sop;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class TaskCreateReq {

    @NotNull
    private Long templateId;
    @NotNull
    private Long nodeId;
    private String planName;
    @NotNull
    private Long assigneeId;
    private Long ipGroupId;
    private Long authorId;
    private Integer needReview;
}
