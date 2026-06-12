package cn.iocoder.yudao.module.oa.api.dto.finance;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class AccountCostVO {

    private Long id;
    private Long accountId;
    private String accountName;
    private String costType;
    private BigDecimal amount;
    private String payMethod;
    private LocalDate payDate;
    private String period;
    private String remark;
    private Long attachmentId;
}
