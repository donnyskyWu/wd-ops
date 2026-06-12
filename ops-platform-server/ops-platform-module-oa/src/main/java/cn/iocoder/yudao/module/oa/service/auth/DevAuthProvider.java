package cn.iocoder.yudao.module.oa.service.auth;

import cn.iocoder.yudao.framework.common.exception.OaErrorCodes;
import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.module.oa.dal.dataobject.auth.SysRoleDO;
import cn.iocoder.yudao.module.oa.dal.dataobject.auth.SysUserDO;
import cn.iocoder.yudao.module.oa.dal.mysql.auth.SysUserTokenMapper;
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
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DevAuthProvider implements AuthProvider {

    private final SysUserTokenMapper sysUserTokenMapper;

    @Override
    public Optional<LoginUser> authenticate(HttpServletRequest request, String token, Long headerTenantId) {
        if (token == null || token.isBlank()) {
            return Optional.empty();
        }
        if (headerTenantId == null) {
            throw new ServiceException(OaErrorCodes.UNAUTHORIZED.getCode(), "缺少 X-Tenant-Id");
        }

        SysUserDO user = sysUserTokenMapper.selectUserByToken(token);
        if (user == null) {
            return Optional.empty();
        }
        if (!headerTenantId.equals(user.getTenantId())) {
            throw new ServiceException(OaErrorCodes.TENANT_FORBIDDEN);
        }

        List<SysRoleDO> roles = sysUserTokenMapper.selectRolesByUserId(user.getId());
        Set<String> authorities = new LinkedHashSet<>();
        roles.stream()
                .map(role -> "ROLE_" + role.getCode())
                .forEach(authorities::add);
        sysUserTokenMapper.selectPermissionCodesByUserId(user.getId()).forEach(authorities::add);

        LoginUser loginUser = LoginUser.builder()
                .userId(user.getId())
                .tenantId(user.getTenantId())
                .username(user.getUsername())
                .nickname(user.getNickname())
                .email(user.getEmail())
                .authorities(authorities)
                .dataScope(resolveDataScope(roles))
                .ipGroupId(user.getIpGroupId())
                .build();
        return Optional.of(loginUser);
    }

    private String resolveDataScope(List<SysRoleDO> roles) {
        boolean hasAll = roles.stream().anyMatch(r -> DataScopeSupport.ALL.equals(r.getDataScope()));
        if (hasAll) {
            return DataScopeSupport.ALL;
        }
        boolean hasIpGroup = roles.stream().anyMatch(r -> DataScopeSupport.IP_GROUP.equals(r.getDataScope()));
        if (hasIpGroup) {
            return DataScopeSupport.IP_GROUP;
        }
        return DataScopeSupport.SELF;
    }
}
