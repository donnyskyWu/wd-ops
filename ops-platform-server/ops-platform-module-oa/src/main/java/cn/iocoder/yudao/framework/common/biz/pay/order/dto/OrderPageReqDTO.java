package cn.iocoder.yudao.framework.common.biz.pay.order.dto;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * Vendored subset of Football {@code OrderPageReqDTO} for OPS order list (G-PAY-01).
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class OrderPageReqDTO extends PageParam {

    private Long authorId;
    private Integer status;
    private LocalDateTime[] createTime;
}
