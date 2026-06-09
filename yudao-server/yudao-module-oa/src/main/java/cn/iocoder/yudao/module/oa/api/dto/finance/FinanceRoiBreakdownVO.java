package cn.iocoder.yudao.module.oa.api.dto.finance;

import lombok.Data;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Data
public class FinanceRoiBreakdownVO {

    private BigDecimal purchase = BigDecimal.ZERO;
    private BigDecimal process = BigDecimal.ZERO;
    private List<CostTypeItem> byType = new ArrayList<>();

    @Data
    public static class CostTypeItem {
        private String type;
        private String typeLabel;
        private BigDecimal amount = BigDecimal.ZERO;
        private BigDecimal percentage = BigDecimal.ZERO;
    }
}
