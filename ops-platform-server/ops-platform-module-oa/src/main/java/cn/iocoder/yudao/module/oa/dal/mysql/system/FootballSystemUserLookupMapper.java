package cn.iocoder.yudao.module.oa.dal.mysql.system;

import cn.iocoder.yudao.module.oa.dal.dataobject.auth.FootballSystemUserDO;
import com.baomidou.dynamic.datasource.annotation.DS;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
@DS("system")
public interface FootballSystemUserLookupMapper {

    @Select("""
            SELECT id FROM system_users
            WHERE tenant_id = #{tenantId}
              AND deleted = 0
              AND username LIKE CONCAT('%', #{username}, '%')
            """)
    List<Long> selectUserIdsByUsernameLike(@Param("tenantId") Long tenantId,
                                           @Param("username") String username);

    @Select("""
            SELECT username FROM system_users
            WHERE id = #{userId} AND deleted = 0
            LIMIT 1
            """)
    String selectUsernameById(@Param("userId") Long userId);

    @Select("""
            <script>
            SELECT id, nickname FROM system_users
            WHERE deleted = 0
              AND id IN
              <foreach collection="ids" item="id" open="(" separator="," close=")">
                  #{id}
              </foreach>
            </script>
            """)
    List<FootballSystemUserDO> selectNicknamesByIds(@Param("ids") List<Long> ids);

    @Select("""
            SELECT id, tenant_id AS tenantId, username, nickname, status
            FROM system_users
            WHERE id = #{userId} AND deleted = 0
            LIMIT 1
            """)
    FootballSystemUserDO selectById(@Param("userId") Long userId);
}
