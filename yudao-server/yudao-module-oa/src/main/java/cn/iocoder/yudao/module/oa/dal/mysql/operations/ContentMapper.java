package cn.iocoder.yudao.module.oa.dal.mysql.operations;

import cn.iocoder.yudao.module.oa.dal.dataobject.operations.ContentDO;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Map;

@Mapper
public interface ContentMapper extends BaseMapper<ContentDO> {

    // S-R8 B1: 5 KPI 卡 SQL 聚合，避免前端 list.reduce 因分页导致数字跟着翻页变
    @Select("""
            <script>
            SELECT
              COUNT(*)                       AS totalCount,
              SUM(CASE WHEN is_hit = 1 THEN 1 ELSE 0 END) AS hitCount,
              COALESCE(SUM(read_count), 0)    AS totalRead,
              COALESCE(SUM(like_count), 0)    AS totalLikes,
              COALESCE(SUM(comment_count), 0) AS totalComments,
              COALESCE(SUM(forward_count), 0) AS totalShares
            FROM oa_content
            WHERE tenant_id = #{tenantId}
              AND deleted = 0
              AND account_id IN
              <foreach collection='accountIds' item='id' open='(' separator=',' close=')'>
                #{id}
              </foreach>
              <if test='startDate != null'>AND publish_time &gt;= #{startDate}</if>
              <if test='endDate != null'>AND publish_time &lt;= #{endDate}</if>
            </script>
            """)
    Map<String, Object> sumStats(@Param("tenantId") Long tenantId,
                                 @Param("accountIds") Collection<Long> accountIds,
                                 @Param("startDate") LocalDateTime startDate,
                                 @Param("endDate") LocalDateTime endDate);
}
