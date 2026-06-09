package cn.iocoder.yudao.module.oa.api.dto.operations;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class FollowerTrendVO {

    private String timePeriod;
    private String accountName;
    private String ipGroupName;
    private Long followerCount;
    private Integer newFollower;
    private Integer unfollowCount;
    private Integer netGrowth;
    private BigDecimal growthRate;
}
