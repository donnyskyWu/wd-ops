package cn.iocoder.yudao.module.oa.api.dto.home;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class HomeMetricsVO {
    private Integer totalAuthors;
    private Integer totalContent;
    private BigDecimal sopCompletionRate;
    private String avgPerfGrade;
}
