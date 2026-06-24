package cn.iocoder.yudao.module.oa.dal.dataobject.collect;

import cn.iocoder.yudao.framework.tenant.core.db.TenantBaseDO;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("oa_collect_task")
public class CollectTaskDO extends TenantBaseDO {

    private String taskName;
    private String platformType;
    private Long accountId;
    private String method;
    private String source;
    /** 采集数据类型（dict_collect_data_type），如 MP_FOLLOWER_LIST / MP_ARTICLE_LIST */
    private String dataType;
    private String frequency;
    private String cron;
    private String apiConfigEncrypted;
    private String status;
    private LocalDateTime lastRunAt;
    private LocalDateTime nextRunAt;
    private Integer runCount;
    private Integer failCount;
}
