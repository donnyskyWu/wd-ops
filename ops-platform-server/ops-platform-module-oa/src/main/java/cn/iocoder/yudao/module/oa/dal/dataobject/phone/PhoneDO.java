package cn.iocoder.yudao.module.oa.dal.dataobject.phone;

import cn.iocoder.yudao.framework.tenant.core.db.TenantBaseDO;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("oa_phone")
public class PhoneDO extends TenantBaseDO {

    private Long realnameId;
    private String phoneNumberEncrypted;
    private String phoneNumberHash;
    private String phoneCode;
    private String phoneModel;
    private Long keeperId;
    private String wechatBound;
    private String status;
    private Integer accountBoundCount;
}
