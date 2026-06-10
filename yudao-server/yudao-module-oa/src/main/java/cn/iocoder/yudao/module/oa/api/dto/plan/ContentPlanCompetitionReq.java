package cn.iocoder.yudao.module.oa.api.dto.plan;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ContentPlanCompetitionReq {

    @NotBlank
    private String competitionId;
    @NotBlank
    private String competitionName;
}
