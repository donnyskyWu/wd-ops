package cn.iocoder.yudao.module.oa.dal.dataobject.sop;

import cn.iocoder.yudao.framework.tenant.core.db.TenantBaseDO;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("oa_sop_template")
public class SopTemplateDO extends TenantBaseDO {

    private String templateName;
    private String contentType;
    private String platformType;
    private String description;
    private Integer status;
}
