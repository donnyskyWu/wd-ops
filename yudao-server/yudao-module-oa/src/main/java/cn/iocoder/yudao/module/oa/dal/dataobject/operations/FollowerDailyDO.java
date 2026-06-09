package cn.iocoder.yudao.module.oa.dal.dataobject.operations;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("oa_follower_daily")
public class FollowerDailyDO {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long tenantId;
    private Long accountId;
    private LocalDate statDate;
    private Long followerCount;
    private Integer newFollower;
    private Integer unfollowCount;
    private Integer netGrowth;
    private BigDecimal growthRate;
    private String creator;
    private LocalDateTime createTime;

    @TableLogic
    private Integer deleted;
}
