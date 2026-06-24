package cn.iocoder.yudao.module.oa.dal.dataobject.collect;

import cn.iocoder.yudao.framework.tenant.core.db.TenantBaseDO;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("oa_wework_daily_stats")
public class WeworkDailyStatsDO extends TenantBaseDO {

    private Long weworkAccountId;
    private LocalDate statDate;
    private Integer totalFriends;
    private Integer todayFriendInteractions;
    private Integer todayMessagesSent;
    private LocalDateTime syncedAt;
}
