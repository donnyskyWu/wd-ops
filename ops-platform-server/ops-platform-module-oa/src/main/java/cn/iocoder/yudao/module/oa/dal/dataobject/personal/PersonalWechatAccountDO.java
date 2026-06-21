package cn.iocoder.yudao.module.oa.dal.dataobject.personal;

import cn.iocoder.yudao.framework.tenant.core.db.TenantBaseDO;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("oa_personal_wechat_account")
public class PersonalWechatAccountDO extends TenantBaseDO {

    private String accountName;
    private String wechatId;
    @TableField("contact_phone")
    private String contactPhone;
    private Long phoneId;
    private Long linkedWeworkEmployeeId;
    private String apiUrlEncrypted;
    private String appIdEncrypted;
    private String appSecretEncrypted;
    private String tokenEncrypted;
    private String status;
    /** 奥创设备 ID（同步写入，默认只读） */
    private String aochuangWechatAccountId;
    /** FK → oa_aocreate_account.id */
    private Long aochuangAccountRefId;
    private String aochuangBindStatus;
    private String aochuangNickname;
    private String aochuangAvatar;
    private Boolean aochuangIsAlive;
    private java.time.LocalDateTime lastDeviceSyncAt;
    private java.time.LocalDateTime lastFriendSyncAt;
    private java.time.LocalDateTime lastMessageSyncAt;
    private String collectStatus;
}
