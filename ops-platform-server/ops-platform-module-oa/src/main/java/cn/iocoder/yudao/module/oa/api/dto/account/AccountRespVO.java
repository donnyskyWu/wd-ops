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
    private Boolean publishEnabled;
    private LocalDateTime linkedAt;
    private LocalDateTime createTime;

    /** 公众号扩展 */
    private String trademarkName;
    private String email;
    private Boolean hasPassword;
    private String qualificationType;
    private String usageStatus;
    private String originalAccountName;
    private LocalDateTime certExpiryTime;
    private Integer certCount;
    private Long linkedVideoAccountId;
    private String linkedVideoAccountName;
    private LocalDateTime videoAccountRegisteredAt;
    private String adminName;
    private Long adminUserId;
    private String adminUserName;
    private String adminPhoneMasked;
    private Boolean hasAdminIdCard;
}
