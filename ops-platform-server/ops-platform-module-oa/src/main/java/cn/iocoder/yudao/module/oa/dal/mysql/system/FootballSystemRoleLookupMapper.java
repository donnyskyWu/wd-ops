package cn.iocoder.yudao.module.oa.dal.mysql.system;

import com.baomidou.dynamic.datasource.annotation.DS;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * shenyu-system {@code system_role} reads for G-SYS-02 roleCode → roleId mapping.
 */
@Mapper
@DS("system")
public interface FootballSystemRoleLookupMapper {

    @Select("""
            SELECT id FROM system_role
            WHERE code = #{roleCode}
              AND tenant_id = #{tenantId}
              AND deleted = 0
              AND status = 0
            LIMIT 1
            """)
    Long selectRoleIdByCode(@Param("tenantId") Long tenantId, @Param("roleCode") String roleCode);
}
