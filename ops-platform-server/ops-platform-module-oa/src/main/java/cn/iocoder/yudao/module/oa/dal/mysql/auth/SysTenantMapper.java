package cn.iocoder.yudao.module.oa.dal.mysql.auth;

import cn.iocoder.yudao.module.oa.dal.dataobject.auth.SysTenantDO;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface SysTenantMapper extends BaseMapper<SysTenantDO> {

    @Select("SELECT COUNT(1) FROM sys_user WHERE tenant_id = #{tenantId} AND deleted = 0")
    Long countUsersByTenantId(@Param("tenantId") Long tenantId);
}
