package cn.iocoder.yudao.module.oa.dal.mysql.auth;

import cn.iocoder.yudao.module.oa.dal.dataobject.auth.FootballSystemRoleDO;
import cn.iocoder.yudao.module.oa.dal.dataobject.auth.FootballSystemUserDO;
import com.baomidou.dynamic.datasource.annotation.DS;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * Football system-server OAuth2 token lookup (ADR-047 §5.3).
 * Validates tokens issued by {@code POST /admin-api/system/auth/login}.
 */
@Mapper
@DS("system")
public interface FootballOAuth2TokenMapper {

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
}
