package cn.iocoder.yudao.module.oa.api.dto.plan;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ContentPlanPreviewMatchReq {

    @NotBlank
    private String competitionId;
    @NotBlank
    private String competitionName;
    /** 比赛开始时间毫秒时间戳；缺省时按当天 20:00 估算 */
    private Long matchTimeRaw;
}
