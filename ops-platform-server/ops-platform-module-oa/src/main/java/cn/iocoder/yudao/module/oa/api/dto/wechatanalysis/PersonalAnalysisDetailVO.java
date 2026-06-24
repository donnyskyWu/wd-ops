package cn.iocoder.yudao.module.oa.api.dto.wechatanalysis;

import lombok.Data;

import java.util.List;

@Data
public class PersonalAnalysisDetailVO {

    private Long accountId;
    private String accountName;
    private String wechatId;
    private String collectStatus;
    private String aochuangBindStatus;
    private List<PersonalDailyStatsItemVO> dailyStats;
}
