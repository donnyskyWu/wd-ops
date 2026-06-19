package cn.iocoder.yudao.module.oa.dal.dataobject.metadata;

import cn.iocoder.yudao.framework.tenant.core.db.TenantBaseDO;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_metadata_entity")
public class MetadataEntityDO extends TenantBaseDO {

    private String entityCode;
    private String entityName;
    private String physicalTable;
    private String status;
    private String remark;
}
