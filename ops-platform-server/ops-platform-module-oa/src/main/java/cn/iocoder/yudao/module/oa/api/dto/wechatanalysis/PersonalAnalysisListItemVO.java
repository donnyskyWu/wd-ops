package cn.iocoder.yudao.module.oa.api.dto.wechatanalysis;

import lombok.Data;

@Data
public class PersonalAnalysisListItemVO {

    private Long accountId;
    private String accountName;
    private Integer totalFriends;
    /** 奥创日聚合 new_friends；M1 文案「今日新增好友」 */
    private Integer newFriends;
    private Integer messagesSent;
    private String statDate;
}
