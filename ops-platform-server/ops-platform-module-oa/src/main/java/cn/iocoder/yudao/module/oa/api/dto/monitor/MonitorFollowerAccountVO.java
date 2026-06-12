package cn.iocoder.yudao.module.oa.api.dto.monitor;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class MonitorFollowerAccountVO {

    private Long accountId;
    private String accountName;
    private String platformType;
    private Long ipGroupId;
    private Long followerCount;
    private Integer netGrowth;
    private BigDecimal growthRate;
    private LocalDate statDate;
}
