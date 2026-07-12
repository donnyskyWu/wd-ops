package cn.iocoder.yudao.module.oa.dal.mysql.auth;

import cn.iocoder.yudao.module.oa.dal.dataobject.auth.FootballSystemRoleDO;
import cn.iocoder.yudao.module.oa.dal.dataobject.auth.FootballSystemUserDO;
import com.baomidou.dynamic.datasource.annotation.DS;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * Football system-server OAuth2 + RBAC on {@code wd} master (integration overlay DS).
 * Tokens/users and {@code oa:*} menu permissions live here until shenyu-system menu seed catches up.
 */
@Mapper
@DS("master")
public interface FootballOAuth2MasterTokenMapper {

    @Select("""
            SELECT u.id, u.tenant_id AS tenantId, u.username, u.nickname, u.email, u.status
            FROM system_users u
            INNER JOIN system_oauth2_access_token t
                ON t.user_id = u.id AND t.deleted = 0
            WHERE t.access_token = #{token}
              AND t.expires_time > NOW()
              AND u.deleted = 0
              AND u.status = 0
            LIMIT 1
            """)
    FootballSystemUserDO selectUserByAccessToken(@Param("token") String token);

    @Select("""
            SELECT u.id, u.tenant_id AS tenantId, u.username, u.nickname, u.email, u.status
            FROM system_users u
            WHERE u.id = #{userId}
              AND u.deleted = 0
              AND u.status = 0
            LIMIT 1
            """)
    FootballSystemUserDO selectUserById(@Param("userId") Long userId);

    @Select("""
            SELECT u.id, u.tenant_id AS tenantId, u.username, u.nickname, u.email, u.status
            FROM system_users u
            WHERE u.username = #{username}
              AND u.deleted = 0
              AND u.status = 0
            LIMIT 1
            """)
    FootballSystemUserDO selectUserByUsername(@Param("username") String username);

    @Select("""
            SELECT r.id, r.code, r.data_scope AS dataScope
            FROM system_role r
            INNER JOIN system_user_role ur ON ur.role_id = r.id AND ur.deleted = 0
            WHERE ur.user_id = #{userId} AND r.deleted = 0 AND r.status = 0
            """)
    List<FootballSystemRoleDO> selectRolesByUserId(@Param("userId") Long userId);

    @Select("""
            SELECT DISTINCT m.permission
            FROM system_menu m
            INNER JOIN system_role_menu rm ON rm.menu_id = m.id AND rm.deleted = 0
            INNER JOIN system_user_role ur ON ur.role_id = rm.role_id AND ur.deleted = 0
            WHERE ur.user_id = #{userId}
              AND m.deleted = 0
              AND m.permission IS NOT NULL
              AND m.permission <> ''
            ORDER BY m.permission
            """)
    List<String> selectPermissionCodesByUserId(@Param("userId") Long userId);

    @Select("""
            SELECT DISTINCT u.id
            FROM system_users u
            INNER JOIN system_user_role ur ON ur.user_id = u.id AND ur.deleted = 0
            INNER JOIN system_role r ON r.id = ur.role_id AND r.deleted = 0 AND r.status = 0
            WHERE r.code = #{roleCode}
              AND u.tenant_id = #{tenantId}
              AND u.deleted = 0
              AND u.status = 0
            """)
    List<Long> selectUserIdsByRoleCode(@Param("tenantId") Long tenantId, @Param("roleCode") String roleCode);

    @Select("""
            SELECT u.id, u.tenant_id AS tenantId, u.username, u.nickname, u.email, u.status
            FROM system_users u
            WHERE u.id = #{userId}
              AND u.deleted = 0
            LIMIT 1
            """)
    FootballSystemUserDO selectDisplayUserById(@Param("userId") Long userId);

    @Select("""
            <script>
            SELECT id, tenant_id AS tenantId, username, nickname, status
            FROM system_users
            WHERE deleted = 0
              AND id IN
              <foreach collection="ids" item="id" open="(" separator="," close=")">
                  #{id}
              </foreach>
            </script>
            """)
    List<FootballSystemUserDO> selectDisplayUsersByIds(@Param("ids") List<Long> ids);

    @Select("""
            SELECT r.name
            FROM system_role r
            WHERE r.code = #{roleCode}
              AND r.tenant_id = #{tenantId}
              AND r.deleted = 0
            LIMIT 1
            """)
    String selectRoleNameByCode(@Param("tenantId") Long tenantId, @Param("roleCode") String roleCode);
}
