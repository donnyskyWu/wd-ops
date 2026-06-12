package cn.iocoder.yudao.module.oa.api.dto.perf;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class ScoreStandardDTO {

    private List<Range> ranges;

    @Data
    public static class Range {
        private BigDecimal min;
        private BigDecimal max;
        private BigDecimal score;
        private String grade;
    }
}
