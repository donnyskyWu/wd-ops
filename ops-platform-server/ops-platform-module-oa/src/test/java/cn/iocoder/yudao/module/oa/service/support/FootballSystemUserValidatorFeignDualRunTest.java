package cn.iocoder.yudao.module.oa.service.support;

import cn.iocoder.yudao.framework.common.biz.system.permission.PermissionCommonApi;
import cn.iocoder.yudao.framework.common.biz.system.user.AdminUserApi;
import cn.iocoder.yudao.framework.common.biz.system.user.dto.AdminUserRespDTO;
import cn.iocoder.yudao.framework.common.exception.OaErrorCodes;
import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.module.oa.dal.dataobject.auth.FootballSystemUserDO;
import cn.iocoder.yudao.module.oa.dal.mysql.auth.FootballOAuth2MasterTokenMapper;
import cn.iocoder.yudao.module.oa.dal.mysql.auth.FootballOAuth2TokenMapper;
import cn.iocoder.yudao.module.oa.dal.mysql.auth.SysUserMapper;
import cn.iocoder.yudao.module.oa.dal.mysql.auth.SysUserTokenMapper;
import cn.iocoder.yudao.module.oa.dal.mysql.system.FootballSystemRoleLookupMapper;
import cn.iocoder.yudao.module.oa.dal.mysql.system.FootballSystemUserLookupMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FootballSystemUserValidatorFeignDualRunTest {

    private static final Long TENANT_ID = 1L;
    private static final String ROLE_CODE = "oa_ip_leader";

    @Mock
    private FootballOAuth2MasterTokenMapper footballOAuth2MasterTokenMapper;
    @Mock
    private FootballOAuth2TokenMapper footballOAuth2TokenMapper;
    @Mock
    private FootballSystemUserLookupMapper footballSystemUserLookupMapper;
    @Mock
    private FootballSystemRoleLookupMapper footballSystemRoleLookupMapper;
    @Mock
    private FootballSystemUserSystemReader footballSystemUserSystemReader;
    @Mock
    private AdminUserApi adminUserApi;
    @Mock
    private PermissionCommonApi permissionCommonApi;
    @Mock
    private SysUserMapper sysUserMapper;
    @Mock
    private SysUserTokenMapper sysUserTokenMapper;

    private FootballSystemUserValidator validator;

    @BeforeEach
    void setUp() {
        validator = new FootballSystemUserValidator(
                footballOAuth2MasterTokenMapper,
                footballOAuth2TokenMapper,
                footballSystemUserLookupMapper,
                footballSystemRoleLookupMapper,
                footballSystemUserSystemReader,
                adminUserApi,
                permissionCommonApi,
                sysUserMapper,
                sysUserTokenMapper);
    }

    @Test
    @DisplayName("G-SYS-01 cutover: Feign simple-list 成功时不查 @DS Mapper")
    void prefersFeignSimpleListWhenAvailable() {
        AdminUserRespDTO dto = new AdminUserRespDTO();
        dto.setId(1024L);
        dto.setNickname("张三");
        dto.setStatus(0);
        when(adminUserApi.getSimpleUserList(null, 0, null))
                .thenReturn(CommonResult.success(List.of(dto)));

        List<FootballSystemUserDO> users = validator.listEnabledUsersInTenant(TENANT_ID);

        assertEquals(1, users.size());
        assertEquals(1024L, users.get(0).getId());
        assertEquals("张三", users.get(0).getNickname());
        assertEquals(TENANT_ID, users.get(0).getTenantId());
        verify(footballSystemUserLookupMapper, never()).selectEnabledUsersByTenant(any());
    }

    @Test
    @DisplayName("G-SYS-01 cutover: Feign 失败时 fail-fast")
    void throwsWhenFeignSimpleListFails() {
        when(adminUserApi.getSimpleUserList(null, 0, null)).thenThrow(new RuntimeException("system-server down"));

        assertThrows(ServiceException.class, () -> validator.listEnabledUsersInTenant(TENANT_ID));

        verify(footballSystemUserLookupMapper, never()).selectEnabledUsersByTenant(TENANT_ID);
    }

    @Test
    @DisplayName("G-SYS-01 cutover: Feign 返回空列表时不回退 @DS")
    void acceptsEmptyFeignResultWithoutDsFallback() {
        when(adminUserApi.getSimpleUserList(null, 0, null)).thenReturn(CommonResult.success(List.of()));

        List<FootballSystemUserDO> users = validator.listEnabledUsersInTenant(TENANT_ID);

        assertTrue(users.isEmpty());
        verify(footballSystemUserLookupMapper, never()).selectEnabledUsersByTenant(eq(TENANT_ID));
    }

    @Test
    @DisplayName("G-SYS-02 cutover: assertEnabledInTenant 走 getUser + validateUserList Feign")
    void assertEnabledUsesGetUserAndValidateUserList() {
        AdminUserRespDTO dto = new AdminUserRespDTO();
        dto.setId(1024L);
        dto.setTenantId(TENANT_ID);
        dto.setStatus(0);
        when(footballSystemUserSystemReader.findById(1024L)).thenReturn(toSystemUser(dto));
        when(adminUserApi.getUser(1024L)).thenReturn(CommonResult.success(dto));
        when(adminUserApi.validateUserList(List.of(1024L))).thenReturn(CommonResult.success(true));

        validator.assertEnabledInTenant(1024L, TENANT_ID, "用户不存在");

        verify(adminUserApi).getUser(1024L);
        verify(adminUserApi).validateUserList(List.of(1024L));
        verify(sysUserMapper, never()).selectById(any());
    }

    @Test
    @DisplayName("G-SYS-02 cutover: assertEnabledInTenant Feign 租户不一致抛 TENANT_FORBIDDEN")
    void assertEnabledFeignTenantMismatch() {
        AdminUserRespDTO dto = new AdminUserRespDTO();
        dto.setId(1024L);
        dto.setTenantId(99L);
        dto.setStatus(0);
        when(footballSystemUserSystemReader.findById(1024L)).thenReturn(toSystemUser(dto));
        when(adminUserApi.getUser(1024L)).thenReturn(CommonResult.success(dto));

        ServiceException ex = assertThrows(ServiceException.class,
                () -> validator.assertEnabledInTenant(1024L, TENANT_ID, "用户不存在"));

        assertEquals(OaErrorCodes.TENANT_FORBIDDEN.getCode(), ex.getCode());
    }

    @Test
    @DisplayName("G-SYS-02 cutover: hasRoleCode 优先 hasAnyRoles Feign")
    void hasRoleCodePrefersFeignHasAnyRoles() {
        when(permissionCommonApi.hasAnyRoles(1024L, ROLE_CODE)).thenReturn(CommonResult.success(true));

        assertTrue(validator.hasRoleCode(1024L, TENANT_ID, ROLE_CODE));

        verify(permissionCommonApi).hasAnyRoles(1024L, ROLE_CODE);
        verify(footballOAuth2TokenMapper, never()).selectRolesByUserId(any());
    }

    @Test
    @DisplayName("G-SYS-02 cutover: hasRoleCode Feign 不可用时 fail-fast")
    void hasRoleCodeThrowsWhenFeignUnavailable() {
        when(permissionCommonApi.hasAnyRoles(1024L, ROLE_CODE)).thenThrow(new RuntimeException("down"));

        assertThrows(ServiceException.class, () -> validator.hasRoleCode(1024L, TENANT_ID, ROLE_CODE));

        verify(footballOAuth2TokenMapper, never()).selectRolesByUserId(any());
    }

    @Test
    @DisplayName("G-SYS-02 cutover: listPresentableUserIdsByRoleCode 走 roleId 映射 + getUserListByRoleId Feign")
    void listUsersByRoleCodeUsesFeignAndLegacyUnion() {
        when(footballSystemRoleLookupMapper.selectRoleIdByCode(TENANT_ID, ROLE_CODE)).thenReturn(10L);
        AdminUserRespDTO dto = new AdminUserRespDTO();
        dto.setId(3001L);
        dto.setStatus(0);
        when(adminUserApi.getUserListByRoleId(10L)).thenReturn(CommonResult.success(List.of(dto)));

        List<Long> ids = validator.listPresentableUserIdsByRoleCode(TENANT_ID, ROLE_CODE);

        assertTrue(ids.contains(3001L));
        verify(adminUserApi).getUserListByRoleId(10L);
        verify(sysUserTokenMapper).selectUsersByRoleCode(TENANT_ID, ROLE_CODE);
    }

    private static FootballSystemUserDO toSystemUser(AdminUserRespDTO dto) {
        FootballSystemUserDO user = new FootballSystemUserDO();
        user.setId(dto.getId());
        user.setTenantId(dto.getTenantId());
        user.setStatus(dto.getStatus());
        return user;
    }
}
