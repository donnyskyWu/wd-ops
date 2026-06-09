package cn.iocoder.yudao.module.oa.dal.dataobject.ipgroup;

import cn.iocoder.yudao.framework.tenant.core.db.TenantBaseDO;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("oa_ip_group_anchor_rel")
public class IpGroupAnchorRelDO extends TenantBaseDO {

    private Long ipGroupId;
    private Long anchorUserId;
    private String anchorType;
}
