package cn.iocoder.yudao.module.oa.api.dto.perf;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class OrderAttributionVO {

    private Long id;
    private Long orderId;
    private String orderNo;
    private Long accountId;
    private String accountName;
    private Long ipGroupId;
    private String ipGroupName;
    private Long authorId;
    private String authorName;
    private Long opsUserId;
    private String opsUserName;
    private BigDecimal revenue;
    private BigDecimal cost;
    private BigDecimal roi;
    private LocalDate statDate;
}
