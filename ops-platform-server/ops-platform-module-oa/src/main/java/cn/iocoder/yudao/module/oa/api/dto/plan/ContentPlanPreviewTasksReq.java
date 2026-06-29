package cn.iocoder.yudao.module.oa.api.dto.plan;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class ContentPlanPreviewTasksReq {

    @NotNull
    private Long templateId;
    @NotNull
    private Long ipGroupId;
    @NotEmpty
    private List<ContentPlanPreviewMatchReq> matches;
}
