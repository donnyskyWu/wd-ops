package cn.iocoder.yudao.module.oa.dal.dataobject.analytics;

import cn.iocoder.yudao.framework.tenant.core.db.TenantBaseDO;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("oa_custom_query")
public class CustomQueryDO extends TenantBaseDO {
    private String queryName;
    private String status;
    private String sqlText;
    private String paramsJson;
}
