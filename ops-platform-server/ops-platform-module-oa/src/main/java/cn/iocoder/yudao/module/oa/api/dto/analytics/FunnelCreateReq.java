package cn.iocoder.yudao.module.oa.api.dto.analytics;

import cn.iocoder.yudao.module.oa.framework.dict.InDict;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

@Data
public class FunnelCreateReq {

    @NotBlank
    private String funnelName;

    @NotBlank
    @InDict("dict_funnel_type")
    private String funnelType;

    @NotEmpty
    private List<FunnelStepReq> steps;

    @Data
    public static class FunnelStepReq {
        private Integer stepOrder;
        private String eventCode;
        private String stepName;
    }
}
