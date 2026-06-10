package cn.iocoder.yudao.module.oa.dal.mysql.sop;

import cn.iocoder.yudao.module.oa.dal.dataobject.sop.TaskDO;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Map;

@Mapper
public interface TaskMapper extends BaseMapper<TaskDO> {

    // S-R9: 按 assignee_id 聚合 4 任务 KPI
    //   total=全量
    //   completed=DONE
    //   inProgress=IN_PROGRESS
    //   overdue=PENDING/IN_PROGRESS/PENDING_REVIEW 且 sla_deadline < now
    @Select("""
            <script>
            SELECT
              assignee_id                                       AS userId,
              COUNT(*)                                          AS total,
              SUM(CASE WHEN status = 'DONE' THEN 1 ELSE 0 END)  AS completed,
              SUM(CASE WHEN status = 'IN_PROGRESS' THEN 1 ELSE 0 END) AS inProgress,
              SUM(CASE WHEN status NOT IN ('DONE','CANCELED')
                        AND sla_deadline IS NOT NULL
                        AND sla_deadline &lt; #{now}
                    THEN 1 ELSE 0 END)                          AS overdue
            FROM oa_task
            WHERE tenant_id = #{tenantId}
              AND deleted = 0
              AND assignee_id IN
              <foreach collection='userIds' item='id' open='(' separator=',' close=')'>
                #{id}
              </foreach>
              <if test='startTime != null'>AND create_time &gt;= #{startTime}</if>
              <if test='endTime != null'>AND create_time &lt;= #{endTime}</if>
            GROUP BY assignee_id
            </script>
            """)
    List<Map<String, Object>> sumByUser(@Param("tenantId") Long tenantId,
                                        @Param("userIds") Collection<Long> userIds,
                                        @Param("startTime") LocalDateTime startTime,
                                        @Param("endTime") LocalDateTime endTime,
                                        @Param("now") LocalDateTime now);
}
