package cn.iocoder.yudao.module.oa.api.dto.perf;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class PerfTrendVO {

    private Long userId;
    private List<TrendPoint> trends;

    @Data
    public static class TrendPoint {
        private String period;
        private BigDecimal totalScore;
        private String grade;
    }
}
