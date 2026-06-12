package cn.iocoder.yudao.module.oa.api.dto.operations;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 粉丝统计聚合（spec: API-M1-运营管理 §4.4 / FR-M1-004）
 * S-R6-B1+B4：替代前端 list.reduce 累加 + list[0].growthRate 的错误聚合
 */
@Data
public class FollowerStatsVO {
    /** 当前筛选范围内所有粉丝日表记录的 followerCount 最大值（代表总粉丝） */
    private Long totalFollowers;
    /** 新增粉丝数（sum newFollower） */
    private Long newFollowers;
    /** 取消关注数（sum unfollowCount） */
    private Long unfollowers;
    /** 净增粉丝数（sum netGrowth） */
    private Long netFollowers;
    /** 净增 / 上日总数（聚合增长率） */
    private BigDecimal growthRate;
}
