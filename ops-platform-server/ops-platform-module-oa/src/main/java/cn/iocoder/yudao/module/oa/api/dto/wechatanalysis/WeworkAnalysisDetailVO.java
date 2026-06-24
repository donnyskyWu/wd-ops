package cn.iocoder.yudao.module.oa.api.dto.wechatanalysis;

import lombok.Data;

import java.util.List;

@Data
public class WeworkAnalysisDetailVO {

    private Long accountId;
    private String accountName;
    private String corpId;
    private String connStatus;
    private List<WeworkDailyStatsItemVO> dailyStats;
}
