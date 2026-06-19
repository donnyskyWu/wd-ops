package cn.iocoder.yudao.module.oa.dal.dataobject.content;

import cn.iocoder.yudao.framework.tenant.core.db.TenantBaseDO;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("oa_typesetting_rule")
public class TypesettingRuleDO extends TenantBaseDO {

    private String ruleCode;
    private String name;
    private String description;
    private String ruleConfig;
    private Integer sort;
    private String status;
}
