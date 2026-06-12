package cn.iocoder.yudao.module.oa.api.dto.report;

import lombok.Data;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Data
public class ReportStatsVO {

    private Long totalAccounts;
    private Long totalFollowers;
    private Long totalContents;
    private Long totalReads;
    private BigDecimal totalRevenue;
    private BigDecimal overallRoi;
    private List<Map<String, Object>> items = new ArrayList<>();
}
