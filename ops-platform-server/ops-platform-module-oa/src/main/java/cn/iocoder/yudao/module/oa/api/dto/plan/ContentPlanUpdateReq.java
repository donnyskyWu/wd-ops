package cn.iocoder.yudao.module.oa.api.dto.plan;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
public class ContentPlanUpdateReq {

    @NotNull
    private Long id;
    @NotBlank
    @Size(max = 100)
    private String planName;
    @NotNull
    private LocalDate startDate;
    @NotNull
    private LocalDate endDate;
    @Size(max = 500)
    private String description;
    @NotEmpty
    private List<ContentPlanCompetitionReq> competitions;
    @NotEmpty
    private List<ContentPlanStepReq> steps;
}
