package cn.iocoder.yudao.module.oa.dal.dataobject.operations;

import cn.iocoder.yudao.framework.tenant.core.db.TenantBaseDO;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("oa_ops_anchor_rel")
public class OpsAnchorRelDO extends TenantBaseDO {

    private Long opsUserId;
    private Long anchorUserId;
    private Long ipGroupId;
    private LocalDate startDate;
    private LocalDate endDate;
}
