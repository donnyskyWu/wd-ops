package cn.iocoder.yudao.module.oa.dal.dataobject.system;

import cn.iocoder.yudao.framework.tenant.core.db.TenantBaseDO;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_param")
public class SysParamDO extends TenantBaseDO {

    private String paramName;
    private String paramKey;
    private String paramValue;
    private String paramType;
    private String category;
    private String remark;
}
