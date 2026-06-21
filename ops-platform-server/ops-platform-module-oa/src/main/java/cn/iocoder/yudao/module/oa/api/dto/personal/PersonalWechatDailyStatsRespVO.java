package cn.iocoder.yudao.module.oa.api.dto.personal;

import lombok.Data;

@Data
public class PersonalWechatDailyStatsRespVO {

    private Long personalWechatId;
    private String statDate;
    private Integer totalFriends;
    private Integer newFriends;
    private Integer deletedFriends;
    private Integer messagesSent;
    private Integer messagesReceived;
    private Integer groupCount;
}
