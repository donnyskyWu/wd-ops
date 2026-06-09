package cn.iocoder.yudao.module.oa.api.dto.perf;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class OrderAttributionRoiVO {

    private BigDecimal totalPayAmount;
    private BigDecimal totalInCost;
    private BigDecimal roi;
    private List<IpGroupRoi> byIpGroup;

    @Data
    public static class IpGroupRoi {
        private Long ipGroupId;
        private String ipGroupName;
        private BigDecimal payAmount;
        private BigDecimal inCost;
        private BigDecimal roi;
    }
}
