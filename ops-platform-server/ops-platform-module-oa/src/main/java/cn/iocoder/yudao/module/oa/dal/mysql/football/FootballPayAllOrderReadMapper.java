package cn.iocoder.yudao.module.oa.dal.mysql.football;

import cn.iocoder.yudao.module.oa.dal.dataobject.football.FootballPayAllOrderReadDO;
import com.baomidou.dynamic.datasource.annotation.DS;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Read-only cross-query to Football order SSOT {@code pay_all_order} in shenyu-pay (S3 @DS pay).
 * <p>
 * ADR-049 names {@code trade_order}/{@code pay_order} (ruoyi-vue-pro mall template); this Football
 * deployment has no {@code football-module-trade} — business orders live in {@code pay_all_order},
 * gold top-ups in {@code pay_gold_order}. SELECT-only; no writes.
 * </p>
 */
@Mapper
@DS("pay")
public interface FootballPayAllOrderReadMapper {

    @Select("""
            <script>
            SELECT COUNT(1)
            FROM pay_all_order
            WHERE tenant_id = #{tenantId}
              AND deleted = 0
              AND create_time &gt;= #{startTime}
              AND create_time &lt; #{endTime}
            <if test="authorId != null">
              AND author_id = #{authorId}
            </if>
            <if test="status != null">
              AND status = #{status}
            </if>
            </script>
            """)
    long countPage(@Param("tenantId") Long tenantId,
                   @Param("startTime") LocalDateTime startTime,
                   @Param("endTime") LocalDateTime endTime,
                   @Param("authorId") Long authorId,
                   @Param("status") Integer status);

    @Select("""
            <script>
            SELECT id,
                   tenant_id   AS tenantId,
                   order_no    AS orderNo,
                   user_id     AS userId,
                   author_id   AS authorId,
                   amount,
                   pay_amount  AS payAmount,
                   status,
                   order_type  AS orderType,
                   pay_time    AS payTime,
                   create_time AS createTime
            FROM pay_all_order
            WHERE tenant_id = #{tenantId}
              AND deleted = 0
              AND create_time &gt;= #{startTime}
              AND create_time &lt; #{endTime}
            <if test="authorId != null">
              AND author_id = #{authorId}
            </if>
            <if test="status != null">
              AND status = #{status}
            </if>
            ORDER BY create_time DESC
            LIMIT #{offset}, #{limit}
            </script>
            """)
    List<FootballPayAllOrderReadDO> selectPage(@Param("tenantId") Long tenantId,
                                               @Param("startTime") LocalDateTime startTime,
                                               @Param("endTime") LocalDateTime endTime,
                                               @Param("authorId") Long authorId,
                                               @Param("status") Integer status,
                                               @Param("offset") int offset,
                                               @Param("limit") int limit);
}
