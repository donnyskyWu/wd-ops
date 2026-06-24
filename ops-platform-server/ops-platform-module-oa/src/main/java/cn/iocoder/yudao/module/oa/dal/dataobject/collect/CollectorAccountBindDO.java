package cn.iocoder.yudao.module.oa.dal.dataobject.collect;

import cn.iocoder.yudao.framework.tenant.core.db.TenantBaseDO;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("oa_collector_account_bind")
public class CollectorAccountBindDO extends TenantBaseDO {

    private Long oaAccountId;
    private String collectorAccountId;
    private String platformType;
    private String bindStatus;
    private String connStatus;
    private LocalDateTime lastBindAt;
    private LocalDateTime lastHealthCheckAt;
}
