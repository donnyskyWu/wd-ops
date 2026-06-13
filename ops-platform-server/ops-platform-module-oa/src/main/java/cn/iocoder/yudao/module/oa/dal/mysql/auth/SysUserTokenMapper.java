package cn.iocoder.yudao.module.oa.dal.mysql.auth;

import cn.iocoder.yudao.module.oa.dal.dataobject.auth.SysRoleDO;
import cn.iocoder.yudao.module.oa.dal.dataobject.auth.SysUserDO;
import cn.iocoder.yudao.module.oa.dal.dataobject.auth.SysUserTokenDO;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface SysUserTokenMapper extends BaseMapper<SysUserTokenDO> {

    @Select("""
            SELECT u.* FROM sys_user u
            INNER JOIN sys_user_token t ON t.user_id = u.id AND t.deleted = 0
            WHERE t.token = #{token} AND t.status = 'ENABLED' AND u.deleted = 0 AND u.status = 'ENABLED'
            LIMIT 1
            """)
    SysUserDO selectUserByToken(@Param("token") String token);

    @Select("""
            SELECT r.* FROM sys_role r
            INNER JOIN sys_user_role ur ON ur.role_id = r.id
            WHERE ur.user_id = #{userId} AND r.deleted = 0
            """)
    List<SysRoleDO> selectRolesByUserId(@Param("userId") Long userId);

    @Select("""
            SELECT DISTINCT p.code FROM sys_permission p
            INNER JOIN sys_role_permission rp ON rp.permission_id = p.id
            INNER JOIN sys_user_role ur ON ur.role_id = rp.role_id
            WHERE ur.user_id = #{userId} AND p.deleted = 0
            ORDER BY p.code
            """)
    List<String> selectPermissionCodesByUserId(@Param("userId") Long userId);

    @Select("""
            SELECT DISTINCT u.* FROM sys_user u
            INNER JOIN sys_user_role ur ON ur.user_id = u.id
            INNER JOIN sys_role r ON r.id = ur.role_id
            WHERE r.code = #{roleCode} AND r.deleted = 0 AND u.deleted = 0 AND u.status = 'ENABLED'
            AND u.tenant_id = #{tenantId}
            """)
    List<SysUserDO> selectUsersByRoleCode(@Param("tenantId") Long tenantId, @Param("roleCode") String roleCode);
}
