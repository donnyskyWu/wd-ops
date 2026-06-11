package cn.iocoder.yudao.module.oa.api.dto.account;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class AccountRespVO {

    private Long id;
    private String platformType;
    private String accountType;
    private String accountName;
    private String externalAccountId;
    private Long companyId;
    private String companyName;
    private Long realnameId;
    private String realName;
    private Long phoneId;
    private String phoneNumberMasked;
    private Long simCardId;
    private String simCardMasked;
    private Long intermediaryId;
    private String intermediaryName;
    private Long ipGroupId;
    private String ipGroupName;
    private Long followerCount;
    private Integer workCount;
    private String status;
    private Boolean hasCookie;
    private LocalDateTime linkedAt;
    private LocalDateTime createTime;
}
