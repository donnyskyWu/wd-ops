package cn.iocoder.yudao.module.oa.dal.dataobject.plan;

import cn.iocoder.yudao.framework.tenant.core.db.TenantBaseDO;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("oa_content_plan_step")
public class ContentPlanStepDO extends TenantBaseDO {

    private Long planId;
    private Long nodeId;
    private String assigneeIdsJson;
    private LocalDateTime scheduledStart;
    private LocalDateTime scheduledEnd;
}
