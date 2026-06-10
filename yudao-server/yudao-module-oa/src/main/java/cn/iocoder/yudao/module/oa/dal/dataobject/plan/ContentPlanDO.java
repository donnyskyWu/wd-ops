package cn.iocoder.yudao.module.oa.dal.dataobject.plan;

import cn.iocoder.yudao.framework.tenant.core.db.TenantBaseDO;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("oa_content_plan")
public class ContentPlanDO extends TenantBaseDO {

    private String planName;
    private Long templateId;
    private Long ipGroupId;
    private LocalDate startDate;
    private LocalDate endDate;
    private String description;
    private String status;
}
