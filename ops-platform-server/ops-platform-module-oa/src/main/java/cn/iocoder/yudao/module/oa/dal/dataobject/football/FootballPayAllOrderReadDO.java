package cn.iocoder.yudao.module.oa.dal.dataobject.football;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Read-only projection of Football {@code pay_all_order} (ADR-049 P2b).
 * Table ownership: football-module-pay — Ops must never INSERT/UPDATE/DELETE.
 */
@Data
public class FootballPayAllOrderReadDO {

    private Long id;
    private Long tenantId;
    private String orderNo;
    private Long userId;
    private Long authorId;
    private BigDecimal amount;
    private BigDecimal payAmount;
    private Integer status;
    private Integer orderType;
    private LocalDateTime payTime;
    private LocalDateTime createTime;
}
