package cn.iocoder.yudao.module.oa.dal.dataobject.account;

import cn.iocoder.yudao.framework.tenant.core.db.TenantBaseDO;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("oa_platform_account_fan_group")
public class PlatformAccountFanGroupDO extends TenantBaseDO {

    private Long accountId;
    private String groupName;
    private Integer memberCount;
}
