package cn.iocoder.yudao.module.oa;

import cn.iocoder.yudao.module.oa.service.auth.OpsDataScopeSupport;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OpsDataScopeSupportAdminRoleTest {

    @Test
    @DisplayName("系统管理员角色：OA_ADMIN / TENANT_ADMIN / super_admin")
    void recognizesPlatformAdminRoles() {
        assertTrue(OpsDataScopeSupport.hasOaTenantAdminAuthority(Set.of("ROLE_OA_ADMIN")));
        assertTrue(OpsDataScopeSupport.hasOaTenantAdminAuthority(Set.of("ROLE_TENANT_ADMIN")));
        assertTrue(OpsDataScopeSupport.hasOaTenantAdminAuthority(Set.of("ROLE_super_admin")));
    }

    @Test
    @DisplayName("非系统管理员：OPS_LEADER 即使有 ALL 数据范围也不算 admin")
    void rejectsOpsLeaderAsAdmin() {
        Set<String> leaderAuthorities = new LinkedHashSet<>();
        leaderAuthorities.add("ROLE_OPS_LEADER");
        leaderAuthorities.add("oa:ip-group:list");
        assertFalse(OpsDataScopeSupport.hasOaTenantAdminAuthority(leaderAuthorities));
    }
}
