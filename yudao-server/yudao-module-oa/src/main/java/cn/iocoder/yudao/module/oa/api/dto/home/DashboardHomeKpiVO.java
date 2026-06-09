package cn.iocoder.yudao.module.oa.api.dto.home;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class DashboardHomeKpiVO {
    private Long totalAccounts;
    private BigDecimal accountChangeRate;
    private Long totalFollowers;
    private BigDecimal followerChangeRate;
    private Long todayContentCount;
    private BigDecimal contentChangeRate;
    private Long pendingReviewCount;
    private Long alertCount;
}
