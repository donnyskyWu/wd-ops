package cn.iocoder.yudao.module.oa.api.dto.operations;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class FollowerAnalysisVO {

    private LocalDate statDate;
    private Long accountId;
    private String accountName;
    private String ipGroupName;
    private Long followerCount;
    private Integer newFollower;
    private Integer unfollowCount;
    private Integer netGrowth;
    private BigDecimal growthRate;
}
