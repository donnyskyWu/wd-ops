package cn.iocoder.yudao.module.oa.api.dto.account;

import cn.iocoder.yudao.module.oa.framework.dict.InDict;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class AccountCreateReq {

    @NotBlank
    @InDict("dict_platform_type")
    private String platformType;

    @NotBlank
    @Size(max = 128)
    private String accountName;

    @NotBlank
    @Size(max = 64)
    private String externalAccountId;

    @InDict("dict_account_type")
    private String accountType;

    private Long companyId;

    private Long realnameId;

    private Long phoneId;

    private Long simCardId;

    private Long intermediaryId;

    private Long ipGroupId;

    private String cookie;

    /** 采集凭证扩展（ADR-047） */
    private String mpToken;
    private String authToken;
    private String appId;
    private String appSecret;
    private String fieldMapping;

    @InDict("dict_account_status")
    private String status;

    private Boolean forceReplace;

    @Size(min = 5, max = 200)
    private String reason;

    /** 公众号扩展 */
    @Size(max = 128)
    private String trademarkName;

    @Email
    @Size(max = 128)
    private String email;

    @Size(max = 128)
    private String password;

    @InDict("dict_qualification_type")
    private String qualificationType;

    @InDict("dict_wechat_usage_status")
    private String usageStatus;

    @Size(max = 128)
    private String originalAccountName;

    private LocalDateTime certExpiryTime;

    private Long linkedVideoAccountId;

    private LocalDateTime videoAccountRegisteredAt;

    @Size(max = 64)
    private String adminName;

    private Long adminUserId;

    @Size(max = 32)
    private String adminIdCard;
}
