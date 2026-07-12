package cn.iocoder.yudao.module.oa.dal.dataobject.collect;

import cn.iocoder.yudao.framework.tenant.core.db.TenantBaseDO;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("oa_tenant_collector_credential")
public class TenantCollectorCredentialDO extends TenantBaseDO {

    private String platform;
    private String credentialProfile;
    private String profileName;
    private String cookieEncrypted;
    private String authTokenEncrypted;
    private LocalDateTime expireAt;
    private String connStatus;
    private String status;
    private LocalDateTime lastVerifiedAt;
    private String remark;
}
