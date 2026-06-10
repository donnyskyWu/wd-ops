package cn.iocoder.yudao.module.oa.dal.mysql.perf;

import cn.iocoder.yudao.module.oa.dal.dataobject.perf.OrderAttributionDO;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Map;

@Mapper
public interface OrderAttributionMapper extends BaseMapper<OrderAttributionDO> {

    // S-R9: 按 ops_user_id 聚合 营收/成本/ROI
    //   SUM(revenue) AS revenue
    //   SUM(cost) AS cost
    //   revenue / cost AS roi (in service)
    @Select("""
            <script>
            SELECT
              ops_user_id                            AS userId,
              COALESCE(SUM(revenue), 0)              AS revenue,
              COALESCE(SUM(cost), 0)                 AS cost,
              COUNT(*)                                AS orderCount
            FROM oa_order_attribution
            WHERE tenant_id = #{tenantId}
              AND deleted = 0
              AND ops_user_id IN
              <foreach collection='userIds' item='id' open='(' separator=',' close=')'>
                #{id}
              </foreach>
              <if test='startDate != null'>AND stat_date &gt;= #{startDate}</if>
              <if test='endDate != null'>AND stat_date &lt;= #{endDate}</if>
            GROUP BY ops_user_id
            </script>
            """)
    List<Map<String, Object>> sumByUser(@Param("tenantId") Long tenantId,
                                        @Param("userIds") Collection<Long> userIds,
                                        @Param("startDate") LocalDate startDate,
                                        @Param("endDate") LocalDate endDate);
}
