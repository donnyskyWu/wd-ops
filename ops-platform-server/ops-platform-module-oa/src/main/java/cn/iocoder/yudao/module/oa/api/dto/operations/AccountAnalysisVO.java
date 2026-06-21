package cn.iocoder.yudao.module.oa.api.dto.operations;

import lombok.Data;

@Data
public class AccountAnalysisVO {

    private Long accountId;
    private String accountName;
    private String platformType;
    private Long ipGroupId;
    private String ipGroupName;
    private String accountType;
    private String status;
    private Long followerCount;
    private Integer contentCount;
    /** 个微日统计（M10-AO-S-06） */
    private Integer messagesSent;
    private Integer messagesReceived;
    private String collectStatus;
    private String statDate;
}
