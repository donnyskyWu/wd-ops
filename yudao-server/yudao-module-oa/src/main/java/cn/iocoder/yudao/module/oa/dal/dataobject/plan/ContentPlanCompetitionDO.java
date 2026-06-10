package cn.iocoder.yudao.module.oa.dal.dataobject.plan;

import cn.iocoder.yudao.framework.tenant.core.db.TenantBaseDO;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("oa_content_plan_competition")
public class ContentPlanCompetitionDO extends TenantBaseDO {

    private Long planId;
    private String competitionId;
    private String competitionName;
}
