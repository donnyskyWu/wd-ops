package cn.iocoder.yudao.module.oa.dal.dataobject.ipgroup;

import cn.iocoder.yudao.framework.tenant.core.db.TenantBaseDO;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("oa_ip_group")
public class IpGroupDO extends TenantBaseDO {

    private String groupName;
    /** 1=大组 2=小组 */
    private Integer groupType;
    private Long parentId;
    private Long leaderUserId;
    private Integer sortOrder;
    /** 0=停用 1=启用 */
    private Integer status;
    /** IP组等级 dict_ip_group_level: S/A/B/C */
    private String level;
    private String remark;
}
