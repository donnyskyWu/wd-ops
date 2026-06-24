package cn.iocoder.yudao.module.oa.api.dto.wechatanalysis;

import lombok.Data;

@Data
public class WeworkAnalysisListItemVO {

    private Long accountId;
    private String accountName;
    private Integer totalFriends;
    /** 92132 chat_cnt 映射；M1 文案「今日好友互动」 */
    private Integer todayFriendInteractions;
    private Integer todayMessagesSent;
    private String statDate;
}
