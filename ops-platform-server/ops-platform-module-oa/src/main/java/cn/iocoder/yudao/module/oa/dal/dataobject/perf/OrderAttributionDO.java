package cn.iocoder.yudao.module.oa.dal.dataobject.perf;

import cn.iocoder.yudao.framework.tenant.core.db.TenantBaseDO;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("oa_order_attribution")
public class OrderAttributionDO extends TenantBaseDO {

    private Long orderId;
    private Long accountId;
    private Long ipGroupId;
    private Long authorId;
    private Long opsUserId;
    private BigDecimal revenue;
    private BigDecimal cost;
    private BigDecimal roi;
    private LocalDate statDate;
}
