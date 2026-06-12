package cn.iocoder.yudao.module.oa.dal.dataobject.author;

import cn.iocoder.yudao.framework.tenant.core.db.TenantBaseDO;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("oa_author")
public class AuthorDO extends TenantBaseDO {

    private String authorName;
    private Long ipGroupId;
    private String authorType;
    private Long primaryAccountId;
    private Long userId;
    private Integer status;
    private String remark;
}
