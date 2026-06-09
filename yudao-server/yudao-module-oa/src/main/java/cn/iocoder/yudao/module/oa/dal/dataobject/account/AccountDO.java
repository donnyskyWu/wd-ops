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
    private String status;
    private LocalDateTime linkedAt;
}
