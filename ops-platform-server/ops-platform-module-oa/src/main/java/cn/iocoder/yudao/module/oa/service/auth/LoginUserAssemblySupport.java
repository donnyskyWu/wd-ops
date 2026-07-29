package cn.iocoder.yudao.module.oa.service.auth;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.biz.system.user.AdminUserApi;
import cn.iocoder.yudao.framework.common.biz.system.user.dto.AdminUserRespDTO;
import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.module.oa.dal.dataobject.auth.FootballSystemRoleDO;
import cn.iocoder.yudao.module.oa.dal.dataobject.auth.FootballSystemUserDO;
import cn.iocoder.yudao.module.oa.dal.dataobject.auth.SysUserDO;
import cn.iocoder.yudao.module.oa.dal.mysql.auth.FootballOAuth2MasterTokenMapper;
import cn.iocoder.yudao.module.oa.dal.mysql.auth.FootballOAuth2TokenMapper;
import cn.iocoder.yudao.module.oa.dal.mysql.auth.SysUserTokenMapper;
import cn.iocoder.yudao.module.oa.framework.auth.DataScopeSupport;
import cn.iocoder.yudao.module.oa.framework.auth.LoginUser;
import cn.iocoder.yudao.module.oa.config.OaAuthProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Builds OPS {@link LoginUser} from Football user id/profile without {@code @DS("system")} token lookup.
 */
@Component
@RequiredArgsConstructor
public class LoginUserAssemblySupport {

    private final AdminUserApi adminUserApi;
    private final FootballOAuth2MasterTokenMapper footballOAuth2MasterTokenMapper;
    private final FootballOAuth2TokenMapper footballOAuth2TokenMapper;
    private final SysUserTokenMapper sysUserTokenMapper;
    private final OpsDataScopeSupport opsDataScopeSupport;
    private final OaAuthProperties authProperties;

    public LoginUser buildLoginUser(Long userId, Long tenantId, Map<String, String> userInfo) {
        FootballSystemUserDO user = resolveProfile(userId, tenantId, userInfo);
        if (user == null) {
            return null;
        }
        FootballSystemUserDO masterUser = footballOAuth2MasterTokenMapper.selectUserByUsername(user.getUsername());
        Long authorityUserId = masterUser != null ? masterUser.getId() : user.getId();

        List<FootballSystemRoleDO> roles = footballOAuth2MasterTokenMapper.selectRolesByUserId(authorityUserId);
        if (roles.isEmpty() && authProperties.isLegacyDsTokenEnabled()) {
            roles = footballOAuth2TokenMapper.selectRolesByUserId(user.getId());
        }
        Set<String> authorities = new LinkedHashSet<>();
        roles.stream()
                .map(role -> "ROLE_" + role.getCode())
                .forEach(authorities::add);
        footballOAuth2MasterTokenMapper.selectPermissionCodesByUserId(authorityUserId).forEach(authorities::add);
        if (authorities.stream().noneMatch(code -> code.startsWith("oa:"))
                && authProperties.isLegacyDsTokenEnabled()) {
            footballOAuth2TokenMapper.selectPermissionCodesByUserId(user.getId()).forEach(authorities::add);
        }
        mergeOaPermissions(user.getUsername(), user.getTenantId(), authorities);

        String dataScope = resolveDataScope(roles);
        Set<Long> memberIds = opsDataScopeSupport.resolveMemberIpGroupIds(
                user.getId(), user.getUsername(), user.getTenantId());
        Set<Long> ledIds = opsDataScopeSupport.resolveLedIpGroupIds(
                user.getId(), user.getUsername(), user.getTenantId());

        return LoginUser.builder()
                .userId(user.getId())
                .tenantId(user.getTenantId())
                .username(user.getUsername())
                .nickname(user.getNickname())
                .email(user.getEmail())
                .authorities(authorities)
                .dataScope(dataScope)
                .ipGroupId(resolveCompatIpGroupId(memberIds, null))
                .memberIpGroupIds(memberIds.isEmpty() ? null : memberIds)
                .ledIpGroupIds(ledIds.isEmpty() ? null : ledIds)
                .ipGroupLeader(!ledIds.isEmpty())
                .build();
    }

    private FootballSystemUserDO resolveProfile(Long userId, Long tenantId, Map<String, String> userInfo) {
        FootballSystemUserDO user = loadProfileViaFeign(userId);
        if (user == null) {
            user = footballOAuth2MasterTokenMapper.selectUserById(userId);
        }
        if (user == null) {
            return null;
        }
        if (tenantId != null && !tenantId.equals(user.getTenantId())) {
            return null;
        }
        if (userInfo != null) {
            if (StrUtil.isBlank(user.getNickname()) && StrUtil.isNotBlank(userInfo.get("nickname"))) {
                user.setNickname(userInfo.get("nickname"));
            }
            if (StrUtil.isBlank(user.getUsername()) && StrUtil.isNotBlank(userInfo.get("username"))) {
                user.setUsername(userInfo.get("username"));
            }
        }
        return user;
    }

    private FootballSystemUserDO loadProfileViaFeign(Long userId) {
        if (adminUserApi == null || userId == null) {
            return null;
        }
        try {
            CommonResult<AdminUserRespDTO> result = adminUserApi.getUser(userId);
            if (result == null || !result.isSuccess() || result.getData() == null) {
                return null;
            }
            AdminUserRespDTO dto = result.getData();
            FootballSystemUserDO user = new FootballSystemUserDO();
            user.setId(dto.getId());
            user.setTenantId(dto.getTenantId());
            user.setNickname(dto.getNickname());
            user.setStatus(dto.getStatus());
            return user;
        } catch (Exception ignored) {
            return null;
        }
    }

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

    private Long resolveCompatIpGroupId(Set<Long> memberIds, Long legacyIpGroupId) {
        if (memberIds != null && memberIds.size() == 1) {
            return memberIds.iterator().next();
        }
        return legacyIpGroupId;
    }
}
