package cn.iocoder.yudao.module.oa.dal.dataobject.metadata;

import cn.iocoder.yudao.framework.tenant.core.db.TenantBaseDO;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_metadata_field")
public class MetadataFieldDO extends TenantBaseDO {

    private Long entityId;
    private String fieldCode;
    private String fieldName;
    private String columnName;
    private String dataType;
    private String queryConditionType;
    private String dictType;
    private String selectorConfig;
    private Integer sort;
}
