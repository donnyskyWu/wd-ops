package cn.iocoder.yudao.module.oa.dal.dataobject.analytics;

import cn.iocoder.yudao.framework.tenant.core.db.TenantBaseDO;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("oa_funnel")
public class FunnelDO extends TenantBaseDO {
    private String funnelName;
    private String funnelType;
    private Integer status;
    private String remark;
}
