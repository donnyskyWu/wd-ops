package cn.iocoder.yudao.module.oa.api.dto.finance;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Data
public class FinanceRoiTrendVO {

    private List<TrendPoint> points = new ArrayList<>();

    @Data
    public static class TrendPoint {
        private LocalDate statDate;
        private BigDecimal revenue = BigDecimal.ZERO;
        private BigDecimal cost = BigDecimal.ZERO;
        private BigDecimal roi = BigDecimal.ZERO;
    }
}
