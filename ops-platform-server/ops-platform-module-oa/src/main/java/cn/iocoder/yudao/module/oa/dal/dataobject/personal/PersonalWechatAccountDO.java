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
    private String apiUrlEncrypted;
    private String appIdEncrypted;
    private String appSecretEncrypted;
    private String tokenEncrypted;
    private String status;
}
