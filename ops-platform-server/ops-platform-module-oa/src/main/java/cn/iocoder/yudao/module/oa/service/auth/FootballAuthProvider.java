package cn.iocoder.yudao.module.oa.service.auth;

import cn.iocoder.yudao.framework.common.exception.OaErrorCodes;
import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.module.oa.dal.dataobject.auth.FootballSystemRoleDO;
import cn.iocoder.yudao.module.oa.dal.dataobject.auth.FootballSystemUserDO;
import cn.iocoder.yudao.module.oa.dal.mysql.auth.FootballOAuth2MasterTokenMapper;
import cn.iocoder.yudao.module.oa.dal.mysql.auth.FootballOAuth2TokenMapper;
import cn.iocoder.yudao.module.oa.dal.mysql.auth.SysUserTokenMapper;
import cn.iocoder.yudao.module.oa.dal.dataobject.auth.SysUserDO;
import cn.iocoder.yudao.module.oa.framework.auth.AuthProvider;
import cn.iocoder.yudao.module.oa.framework.auth.DataScopeSupport;
import cn.iocoder.yudao.module.oa.framework.auth.LoginUser;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class FootballAuthProvider implements AuthProvider {

    private final FootballOAuth2TokenMapper footballOAuth2TokenMapper;
    private final FootballOAuth2MasterTokenMapper footballOAuth2MasterTokenMapper;
    private final SysUserTokenMapper sysUserTokenMapper;
    private final FootballOAuth2TokenRedisReader tokenRedisReader;

    @Override
    public Optional<LoginUser> authenticate(HttpServletRequest request, String token, Long headerTenantId) {
        if (token == null || token.isBlank()) {
            return Optional.empty();
        }
        if (headerTenantId == null) {
            throw new ServiceException(OaErrorCodes.UNAUTHORIZED.getCode(), "缺少 X-Tenant-Id");
        }

        FootballSystemUserDO user = resolveUser(token);
        if (user == null) {
            return Optional.empty();
        }
        if (!headerTenantId.equals(user.getTenantId())) {
            throw new ServiceException(OaErrorCodes.TENANT_FORBIDDEN);
        }

        FootballSystemUserDO masterUser = footballOAuth2MasterTokenMapper.selectUserByUsername(user.getUsername());
        Long authorityUserId = masterUser != null ? masterUser.getId() : user.getId();

        List<FootballSystemRoleDO> roles = footballOAuth2MasterTokenMapper.selectRolesByUserId(authorityUserId);
        if (roles.isEmpty()) {
            roles = footballOAuth2TokenMapper.selectRolesByUserId(user.getId());
        }
        Set<String> authorities = new LinkedHashSet<>();
        roles.stream()
                .map(role -> "ROLE_" + role.getCode())
                .forEach(authorities::add);
        footballOAuth2MasterTokenMapper.selectPermissionCodesByUserId(authorityUserId).forEach(authorities::add);
        if (authorities.stream().noneMatch(code -> code.startsWith("oa:"))) {
            footballOAuth2TokenMapper.selectPermissionCodesByUserId(user.getId()).forEach(authorities::add);
        }
        mergeOaPermissions(user.getUsername(), user.getTenantId(), authorities);

        LoginUser loginUser = LoginUser.builder()
                .userId(user.getId())
                .tenantId(user.getTenantId())
                .username(user.getUsername())
                .nickname(user.getNickname())
                .email(user.getEmail())
                .authorities(authorities)
                .dataScope(resolveDataScope(roles))
                .ipGroupId(null)
                .build();
        return Optional.of(loginUser);
    }

    /**
     * system-server master=wd for tokens/users; shenyu-system SSOT for roles/menus (ADR-050).
     */
    private FootballSystemUserDO resolveUser(String token) {
        FootballSystemUserDO user = footballOAuth2TokenMapper.selectUserByAccessToken(token);
        if (user != null) {
            return user;
        }
        user = footballOAuth2MasterTokenMapper.selectUserByAccessToken(token);
        if (user != null) {
            return mapToSystemUser(user);
        }
        return tokenRedisReader.getToken(token)
                .filter(snapshot -> !snapshot.isExpired())
                .filter(snapshot -> snapshot.getUserId() != null)
                .filter(snapshot -> snapshot.getUserType() == null
                        || snapshot.getUserType() == FootballOAuth2TokenSnapshot.USER_TYPE_ADMIN)
                .map(snapshot -> footballOAuth2MasterTokenMapper.selectUserById(snapshot.getUserId()))
                .map(this::mapToSystemUser)
                .orElse(null);
    }

    /** Map wd OAuth user id to shenyu-system profile for role/menu joins. */
    private FootballSystemUserDO mapToSystemUser(FootballSystemUserDO masterUser) {
        if (masterUser == null) {
            return null;
        }
        FootballSystemUserDO systemUser = footballOAuth2TokenMapper.selectUserByUsername(masterUser.getUsername());
        return systemUser != null ? systemUser : masterUser;
    }

    /** Union OA sys_permission when Football user maps to sys_user (ADR-047 §5.3). */
    private void mergeOaPermissions(String username, Long tenantId, Set<String> authorities) {
        SysUserDO oaUser = sysUserTokenMapper.selectUserByUsernameAndTenant(username, tenantId);
        if (oaUser == null) {
            return;
        }
        sysUserTokenMapper.selectPermissionCodesByUserId(oaUser.getId()).forEach(authorities::add);
    }

    private String resolveDataScope(List<FootballSystemRoleDO> roles) {
        boolean hasAll = roles.stream().anyMatch(role -> role.getDataScope() != null && role.getDataScope() == 1);
        if (hasAll) {
            return DataScopeSupport.ALL;
        }
        return DataScopeSupport.SELF;
    }
}
