package cn.iocoder.yudao.module.oa.dal.dataobject.perf;

import cn.iocoder.yudao.framework.tenant.core.db.TenantBaseDO;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("oa_perf_template")
public class PerfTemplateDO extends TenantBaseDO {

    private String positionsJson;
    private String templateName;
    private Integer isActive;
    private String remark;
}
