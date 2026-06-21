package cn.iocoder.yudao.module.oa.dal.dataobject.collect;

import cn.iocoder.yudao.framework.tenant.core.db.TenantBaseDO;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("oa_collect_log")
public class CollectLogDO extends TenantBaseDO {

    private Long taskId;
    private String status;
    private LocalDateTime startAt;
    private LocalDateTime endAt;
    private Long durationMs;
    private Integer recordCount;
    private String errorMessage;
    private Integer retryCount;
}
