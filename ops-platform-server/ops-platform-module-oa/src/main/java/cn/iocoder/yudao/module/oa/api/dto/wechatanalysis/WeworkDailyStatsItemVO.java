package cn.iocoder.yudao.module.oa.api.dto.wechatanalysis;

import lombok.Data;

@Data
public class WeworkDailyStatsItemVO {

    private String statDate;
    private Integer totalFriends;
    private Integer todayFriendInteractions;
    private Integer todayMessagesSent;
    private String syncedAt;
}
