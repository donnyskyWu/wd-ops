package cn.iocoder.yudao.module.oa.dal.dataobject.personal;

import cn.iocoder.yudao.framework.tenant.core.db.TenantBaseDO;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("oa_wework_account")
public class WeworkAccountDO extends TenantBaseDO {

    private String accountName;
    private String corpId;
    private String agentId;
    private String secretEncrypted;
    private String status;
}
