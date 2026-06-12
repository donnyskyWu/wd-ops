package cn.iocoder.yudao.module.oa.service.perf;

import cn.hutool.json.JSONUtil;
import cn.iocoder.yudao.framework.common.exception.OaErrorCodes;
import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.module.oa.api.dto.perf.ScoreStandardDTO;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Comparator;
import java.util.List;

public final class PerfScoreSupport {

    private static final BigDecimal HUNDRED = new BigDecimal("100");

    private PerfScoreSupport() {
    }

    public static void validateWeightSum(List<BigDecimal> weights) {
        BigDecimal sum = weights.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        if (sum.setScale(2, RoundingMode.HALF_UP).compareTo(HUNDRED) != 0) {
            throw new ServiceException(OaErrorCodes.PERF_WEIGHT_NOT_100);
        }
    }

    public static void validateRanges(List<ScoreStandardDTO.Range> ranges) {
        if (ranges == null || ranges.isEmpty()) {
            throw new ServiceException(OaErrorCodes.PERF_RANGE_GAP);
        }
        List<ScoreStandardDTO.Range> sorted = ranges.stream()
                .sorted(Comparator.comparing(ScoreStandardDTO.Range::getMin))
                .toList();
        for (int i = 0; i < sorted.size(); i++) {
            ScoreStandardDTO.Range current = sorted.get(i);
            if (current.getMin() == null || current.getMax() == null || current.getScore() == null) {
                throw new ServiceException(OaErrorCodes.PERF_RANGE_GAP);
            }
            if (current.getMin().compareTo(current.getMax()) >= 0) {
                throw new ServiceException(OaErrorCodes.PERF_RANGE_OVERLAP);
            }
            if (i > 0) {
                ScoreStandardDTO.Range prev = sorted.get(i - 1);
                if (current.getMin().compareTo(prev.getMax()) < 0) {
                    throw new ServiceException(OaErrorCodes.PERF_RANGE_OVERLAP);
                }
                if (current.getMin().compareTo(prev.getMax()) != 0) {
                    throw new ServiceException(OaErrorCodes.PERF_RANGE_GAP);
                }
            }
        }
    }

    public static ScoreStandardDTO parseStandard(String json) {
        if (json == null || json.isBlank()) {
            throw new ServiceException(OaErrorCodes.PERF_RANGE_GAP);
        }
        return JSONUtil.toBean(json, ScoreStandardDTO.class);
    }

    public static String toJson(ScoreStandardDTO standard) {
        validateRanges(standard.getRanges());
        return JSONUtil.toJsonStr(standard);
    }

    public static ScoreResult resolveScore(BigDecimal metricValue, ScoreStandardDTO standard) {
        validateRanges(standard.getRanges());
        List<ScoreStandardDTO.Range> sorted = standard.getRanges().stream()
                .sorted(Comparator.comparing(ScoreStandardDTO.Range::getMin))
                .toList();
        for (int i = 0; i < sorted.size(); i++) {
            ScoreStandardDTO.Range range = sorted.get(i);
            boolean isLast = i == sorted.size() - 1;
            boolean inRange = metricValue.compareTo(range.getMin()) >= 0
                    && (isLast ? metricValue.compareTo(range.getMax()) <= 0
                    : metricValue.compareTo(range.getMax()) < 0);
            if (inRange) {
                return new ScoreResult(range.getScore(), range.getGrade());
            }
        }
        ScoreStandardDTO.Range last = sorted.get(sorted.size() - 1);
        return new ScoreResult(last.getScore(), last.getGrade());
    }

    public static String resolveGrade(BigDecimal totalScore) {
        if (totalScore == null) {
            return null;
        }
        if (totalScore.compareTo(new BigDecimal("95")) >= 0) {
            return "S";
        }
        if (totalScore.compareTo(new BigDecimal("85")) >= 0) {
            return "A";
        }
        if (totalScore.compareTo(new BigDecimal("75")) >= 0) {
            return "B";
        }
        if (totalScore.compareTo(new BigDecimal("60")) >= 0) {
            return "C";
        }
        return "D";
    }

    public record ScoreResult(BigDecimal score, String grade) {
    }
}
