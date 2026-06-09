package cn.iocoder.yudao.module.oa.api.dto.analytics;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class FunnelDataVO {

    private Long funnelId;
    private List<FunnelStepDataVO> steps = new ArrayList<>();

    @Data
    public static class FunnelStepDataVO {
        private Integer stepOrder;
        private String name;
        private Long count;
        private Double conversionRate;
    }
}
