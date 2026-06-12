package cn.iocoder.yudao.module.oa.dal.dataobject.demo;

import cn.iocoder.yudao.framework.tenant.core.db.TenantBaseDO;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("oa_demo_item")
public class OaDemoItemDO extends TenantBaseDO {

    private String name;
}
