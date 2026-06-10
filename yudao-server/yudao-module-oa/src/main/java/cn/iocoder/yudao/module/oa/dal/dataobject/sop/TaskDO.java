package cn.iocoder.yudao.module.oa.dal.dataobject.sop;

import cn.iocoder.yudao.framework.tenant.core.db.TenantBaseDO;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("oa_task")
public class TaskDO extends TenantBaseDO {

    private Long templateId;
    private Long nodeId;
    private Long planId;
    private String planName;
    private Long assigneeId;
    private Long ipGroupId;
    private Long authorId;
    private String status;
    private Integer visibleInList;
    private LocalDateTime scheduledStart;
    private LocalDateTime scheduledEnd;
    private Integer needReview;
    private LocalDateTime slaDeadline;
    private String deliverables;
    private LocalDateTime startTime;
    private LocalDateTime completeTime;
}
