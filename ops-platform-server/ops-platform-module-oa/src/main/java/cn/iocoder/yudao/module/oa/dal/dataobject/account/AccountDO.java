package cn.iocoder.yudao.module.oa.dal.dataobject.account;

import cn.iocoder.yudao.framework.tenant.core.db.TenantBaseDO;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("oa_account")
public class AccountDO extends TenantBaseDO {

    private String platformType;
    private String accountType;
    private String accountName;
    private String externalAccountId;
    private Long companyId;
    private Long realnameId;
    private Long intermediaryId;
    private Long phoneId;
    private String phoneNumberHash;
    private Long simCardId;
    private Long ipGroupId;
    private String cookieEncrypted;
    /** 公众号后台 Token（ADR-047） */
    private String mpTokenEncrypted;
    /** 平台专用 Token，如快手 cp API（ADR-047） */
    private String authTokenEncrypted;
    /** 账号级字段映射 JSON（ADR-047） */
    private String fieldMapping;
    /** AppId 档案可选（ADR-047） */
    private String appId;
    /** AppSecret AES-256 档案可选（ADR-047） */
    private String appSecretEncrypted;
    /** 是否配置发布权限（ADR-022） */
    private Integer publishEnabled;
    private String status;
    private LocalDateTime linkedAt;

    /** 公众号扩展字段 */
    private String trademarkName;
    private String email;
    private String passwordEncrypted;
    private String qualificationType;
    private String usageStatus;
    private String originalAccountName;
    private LocalDateTime certExpiryTime;
    private Integer certCount;
    private Long linkedVideoAccountId;
    private LocalDateTime videoAccountRegisteredAt;
    private String adminName;
    private Long adminUserId;
    private String adminIdCardEncrypted;
}
