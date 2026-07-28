package cn.iocoder.yudao.framework.common.biz.pay.order.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Vendored subset of Football {@code AllOrderRespDTO} for OPS order list (G-PAY-01).
 */
@Data
public class AllOrderRespDTO {

    private Long id;
    private String orderNo;
    private Long userId;
    private Long authorId;
    private Integer orderType;
    private BigDecimal amount;
    private BigDecimal payAmount;
    private LocalDateTime payTime;
    private LocalDateTime createTime;
    private Integer status;
}
