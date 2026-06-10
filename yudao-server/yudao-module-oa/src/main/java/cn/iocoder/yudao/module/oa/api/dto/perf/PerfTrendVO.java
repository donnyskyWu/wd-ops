package cn.iocoder.yudao.module.oa.api.dto.perf;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class PerfTrendVO {

    private Long userId;
    private String userName;
    private String position;
    private String dept;
    private String joinAt;
    private UserInfo userInfo;
    private List<TrendPoint> trends;
    /** 与 trends 同内容，兼容前端 points / historyList */
    private List<TrendPoint> points;

    @Data
    public static class UserInfo {
        private Long id;
        private String name;
        private String position;
        private String dept;
        private String joinAt;
    }

    @Data
    public static class TrendPoint {
        private String period;
        private BigDecimal totalScore;
        /** 与 totalScore 同义 */
        private BigDecimal score;
        private String grade;
        private String templateName;
        private BigDecimal baseScore;
        private BigDecimal metricScore;
        private BigDecimal bonusScore;
        private String status;
    }
}
