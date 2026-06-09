package cn.iocoder.yudao.module.oa.dal.dataobject.finance;

import cn.iocoder.yudao.framework.tenant.core.db.TenantBaseDO;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("oa_account_cost")
public class AccountCostDO extends TenantBaseDO {
    private Long accountId;
    private String costType;
    private BigDecimal amount;
    private String payMethod;
    private LocalDate payDate;
    private String period;
    private String remark;
    private Long attachmentId;
}
