package cn.iocoder.yudao.module.oa.dal.mysql.auth;

import cn.iocoder.yudao.module.oa.dal.dataobject.auth.SysPermissionDO;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface SysPermissionMapper extends BaseMapper<SysPermissionDO> {

    @Select("""
            SELECT p.* FROM sys_permission p
            INNER JOIN sys_role_permission rp ON rp.permission_id = p.id
            WHERE rp.role_id = #{roleId} AND p.deleted = 0
            ORDER BY p.id
            """)
    List<SysPermissionDO> selectByRoleId(@Param("roleId") Long roleId);
}
