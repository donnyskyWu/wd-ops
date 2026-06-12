package cn.iocoder.yudao.module.oa.dal.mysql.operations;

import cn.iocoder.yudao.module.oa.dal.dataobject.operations.FollowerDailyDO;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDate;
import java.util.Collection;
import java.util.Map;

@Mapper
public interface FollowerDailyMapper extends BaseMapper<FollowerDailyDO> {

    /**
     * S-R6-B1+B4：聚合统计（解决前端 list.reduce 错位累加 + list[0].growthRate 错位增长率）
     * 返回 Map 包含：
     *   totalFollowers = MAX(follower_count)         — 当前粉丝总量峰值
     *   newFollowers   = COALESCE(SUM(new_follower), 0)
     *   unfollowers    = COALESCE(SUM(unfollow_count), 0)
     *   netFollowers   = COALESCE(SUM(net_growth), 0)
     *   prevTotal      = MAX(follower_count) - SUM(net_growth)  — 区间首日粉丝总数
     *   growthRate     = SUM(net_growth) / NULLIF(prev_total, 0)
     *
     * 调用方在 service 中保证 accountIds 非空。
     */
    @Select("""
            <script>
            SELECT
              COALESCE(MAX(follower_count), 0) AS totalFollowers,
              COALESCE(SUM(new_follower), 0) AS newFollowers,
              COALESCE(SUM(unfollow_count), 0) AS unfollowers,
              COALESCE(SUM(net_growth), 0) AS netFollowers
            FROM oa_follower_daily
            WHERE tenant_id = #{tenantId}
              AND deleted = 0
              AND account_id IN
              <foreach collection='accountIds' item='id' open='(' separator=',' close=')'>
                #{id}
              </foreach>
              <if test='startDate != null'>AND stat_date &gt;= #{startDate}</if>
              <if test='endDate != null'>AND stat_date &lt;= #{endDate}</if>
            </script>
            """)
    Map<String, Object> sumStats(@Param("tenantId") Long tenantId,
                                @Param("accountIds") Collection<Long> accountIds,
                                @Param("startDate") LocalDate startDate,
                                @Param("endDate") LocalDate endDate);
}
