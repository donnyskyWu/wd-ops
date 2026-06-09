package cn.iocoder.yudao.module.oa.dal.dataobject.sop;

import cn.iocoder.yudao.framework.tenant.core.db.TenantBaseDO;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("oa_sop_review")
public class SopReviewDO extends TenantBaseDO {

    private Long taskId;
    private Long reviewerId;
    private String reviewerRole;
    private String status;
    private String comment;
}
