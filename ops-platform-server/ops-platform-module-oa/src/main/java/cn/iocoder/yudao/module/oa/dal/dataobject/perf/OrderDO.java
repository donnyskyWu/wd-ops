package cn.iocoder.yudao.module.oa.dal.dataobject.perf;

import cn.iocoder.yudao.framework.tenant.core.db.TenantBaseDO;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("oa_order")
public class OrderDO extends TenantBaseDO {

    private String orderNo;
    private BigDecimal orderAmount;
    private LocalDateTime orderTime;
    private Long accountId;
    private Long ipGroupId;
    private String remark;
}
