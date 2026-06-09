package cn.iocoder.yudao.module.oa.api.dto.finance;

import lombok.Data;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Data
public class FinanceRoiAnalysisVO {

    private BigDecimal totalRevenue = BigDecimal.ZERO;
    private BigDecimal totalCost = BigDecimal.ZERO;
    private BigDecimal roi = BigDecimal.ZERO;
    private List<RoiDetailItem> details = new ArrayList<>();

    @Data
    public static class RoiDetailItem {
        private String name;
        private Long id;
        private BigDecimal revenue = BigDecimal.ZERO;
        private BigDecimal cost = BigDecimal.ZERO;
        private BigDecimal roi = BigDecimal.ZERO;
    }
}
