package cn.iocoder.yudao.module.oa.api.dto.finance;

import cn.iocoder.yudao.module.oa.framework.dict.InDict;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class AccountCostUpdateReq {

    @NotNull
    private Long id;

    @NotNull
    private Long accountId;

    @NotNull
    @InDict("dict_cost_type")
    private String costType;

    @NotNull
    private BigDecimal amount;

    @NotNull
    @InDict("dict_cost_pay_method")
    private String payMethod;

    @NotNull
    private LocalDate payDate;

    @NotNull
    @InDict("dict_cost_period")
    private String period;

    private String remark;
    private Long attachmentId;
}
