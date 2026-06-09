package cn.iocoder.yudao.module.oa.dal.mysql.auth;

import cn.iocoder.yudao.module.oa.dal.dataobject.auth.SysRolePermissionDO;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface SysRolePermissionMapper extends BaseMapper<SysRolePermissionDO> {

    @Delete("DELETE FROM sys_role_permission WHERE role_id = #{roleId}")
    void deleteByRoleId(@Param("roleId") Long roleId);
}
