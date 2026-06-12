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
}
