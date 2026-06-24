package cn.iocoder.yudao.module.oa.api.dto.wechatanalysis;

import lombok.Data;

@Data
public class PersonalDailyStatsItemVO {

    private String statDate;
    private Integer totalFriends;
    private Integer newFriends;
    private Integer deletedFriends;
    private Integer messagesSent;
    private Integer messagesReceived;
    private Integer groupCount;
}
