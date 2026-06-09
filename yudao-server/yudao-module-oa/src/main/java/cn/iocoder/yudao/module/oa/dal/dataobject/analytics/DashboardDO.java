package cn.iocoder.yudao.module.oa.dal.dataobject.analytics;

import cn.iocoder.yudao.framework.tenant.core.db.TenantBaseDO;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("oa_dashboard")
public class DashboardDO extends TenantBaseDO {
    private String dashboardName;
    private String dashboardType;
    private String layoutJson;
    private Integer status;
}
