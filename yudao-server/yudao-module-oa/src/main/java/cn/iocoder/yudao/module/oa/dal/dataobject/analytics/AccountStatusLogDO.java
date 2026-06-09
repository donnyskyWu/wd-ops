package cn.iocoder.yudao.module.oa.dal.dataobject.analytics;

import cn.iocoder.yudao.framework.tenant.core.db.TenantBaseDO;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("oa_account_status_log")
public class AccountStatusLogDO extends TenantBaseDO {
    private Long accountId;
    private LocalDate statDate;
    private String status;
    private Long followerCount;
}
