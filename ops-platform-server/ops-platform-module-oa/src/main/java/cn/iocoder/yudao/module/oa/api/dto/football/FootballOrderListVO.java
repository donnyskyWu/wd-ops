package cn.iocoder.yudao.module.oa.api.dto.football;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Ops-facing read model for Football {@code pay_all_order} rows (P2b).
 */
@Data
public class FootballOrderListVO {

    private Long id;
    private String orderNo;
    private Long userId;
    private Long authorId;
    private BigDecimal amount;
    private BigDecimal payAmount;
    /** Football pay status: 0待支付 1支付成功 2支付失败 3取消 */
    private Integer status;
    /** 0方案 1订阅 2专栏 */
    private Integer orderType;
    private LocalDateTime payTime;
    private LocalDateTime createTime;
    /** Source table for attribution join planning */
    private String sourceTable = "pay_all_order";
}
