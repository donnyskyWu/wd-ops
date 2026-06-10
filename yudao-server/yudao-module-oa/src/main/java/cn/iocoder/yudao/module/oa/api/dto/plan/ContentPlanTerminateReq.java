package cn.iocoder.yudao.module.oa.api.dto.plan;

import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ContentPlanTerminateReq {

    @Size(max = 500)
    private String reason;
}
