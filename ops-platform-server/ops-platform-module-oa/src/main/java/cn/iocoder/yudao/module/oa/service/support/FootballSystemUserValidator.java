package cn.iocoder.yudao.module.oa.service.support;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.exception.ErrorCode;
import cn.iocoder.yudao.framework.common.exception.OaErrorCodes;
import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.module.oa.dal.dataobject.auth.FootballSystemRoleDO;
import cn.iocoder.yudao.module.oa.dal.dataobject.auth.FootballSystemUserDO;
import cn.iocoder.yudao.module.oa.dal.dataobject.auth.SysRoleDO;
import cn.iocoder.yudao.module.oa.dal.dataobject.auth.SysUserDO;
import cn.iocoder.yudao.module.oa.dal.mysql.auth.FootballOAuth2MasterTokenMapper;
import cn.iocoder.yudao.module.oa.dal.mysql.auth.FootballOAuth2TokenMapper;
import cn.iocoder.yudao.module.oa.dal.mysql.auth.SysUserMapper;
import cn.iocoder.yudao.module.oa.dal.mysql.auth.SysUserTokenMapper;
import cn.iocoder.yudao.module.oa.dal.mysql.system.FootballSystemUserLookupMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Validates and resolves user ids submitted from {@code UserSelect} (Football {@code system_users.id})
 * with legacy {@code sys_user} fallback for H2 integration tests.
 */
@Component
@RequiredArgsConstructor
public class FootballSystemUserValidator {

    private final FootballOAuth2MasterTokenMapper footballOAuth2MasterTokenMapper;
    private final FootballOAuth2TokenMapper footballOAuth2TokenMapper;
    private final FootballSystemUserLookupMapper footballSystemUserLookupMapper;
    private final SysUserMapper sysUserMapper;
    private final SysUserTokenMapper sysUserTokenMapper;

    public FootballSystemUserDO findFootballUser(Long userId) {
        if (userId == null) {
            return null;
        }
        FootballSystemUserDO masterUser = findMasterFootballUser(userId);
        if (masterUser != null) {
            return masterUser;
        }
        try {
            return footballSystemUserLookupMapper.selectById(userId);
        } catch (Exception ignored) {
            return null;
        }
    }

    private FootballSystemUserDO findMasterFootballUser(Long userId) {
        try {
            return footballOAuth2MasterTokenMapper.selectDisplayUserById(userId);
        } catch (Exception ignored) {
            return null;
        }
    }

    public SysUserDO findLegacyUser(Long userId) {
        if (userId == null) {
            return null;
        }
        return sysUserMapper.selectById(userId);
    }

    public void assertInTenant(Long userId, Long tenantId, String notFoundMessage) {
        if (userId == null) {
            return;
        }
        FootballSystemUserDO footballUser = findFootballUser(userId);
        if (footballUser != null) {
            if (!Objects.equals(tenantId, footballUser.getTenantId())) {
                throw new ServiceException(OaErrorCodes.TENANT_FORBIDDEN);
            }
            return;
        }
        SysUserDO legacyUser = findLegacyUser(userId);
        if (legacyUser == null) {
            throw new ServiceException(OaErrorCodes.ENTITY_NOT_EXISTS.getCode(), notFoundMessage);
        }
        if (!Objects.equals(tenantId, legacyUser.getTenantId())) {
            throw new ServiceException(OaErrorCodes.TENANT_FORBIDDEN);
        }
    }

    public void assertEnabledInTenant(Long userId, Long tenantId, String notFoundMessage) {
        if (userId == null) {
            return;
        }
        FootballSystemUserDO footballUser = findFootballUser(userId);
        if (footballUser != null) {
            if (!Objects.equals(tenantId, footballUser.getTenantId())) {
                throw new ServiceException(OaErrorCodes.TENANT_FORBIDDEN);
            }
            if (!isFootballUserEnabled(footballUser)) {
                throw new ServiceException(OaErrorCodes.ENTITY_DISABLED);
            }
            return;
        }
        SysUserDO legacyUser = findLegacyUser(userId);
        if (legacyUser == null) {
            throw new ServiceException(OaErrorCodes.ENTITY_NOT_EXISTS.getCode(), notFoundMessage);
        }
        if (!Objects.equals(tenantId, legacyUser.getTenantId())) {
            throw new ServiceException(OaErrorCodes.TENANT_FORBIDDEN);
        }
        if (!"ENABLED".equals(legacyUser.getStatus())) {
            throw new ServiceException(OaErrorCodes.ENTITY_DISABLED);
        }
    }

    public String resolveDisplayName(Long userId) {
        if (userId == null) {
            return null;
        }
        FootballSystemUserDO footballUser = findFootballUser(userId);
        if (footballUser != null) {
            return StrUtil.blankToDefault(footballUser.getNickname(), footballUser.getUsername());
        }
        SysUserDO legacyUser = findLegacyUser(userId);
        if (legacyUser == null) {
            return null;
        }
        return StrUtil.blankToDefault(legacyUser.getNickname(), legacyUser.getUsername());
    }

    public String resolveLegacyPosition(Long userId) {
        if (userId == null) {
            return null;
        }
        SysUserDO legacyUser = findLegacyUser(userId);
        if (legacyUser != null && StrUtil.isNotBlank(legacyUser.getPosition())) {
            return legacyUser.getPosition();
        }
        FootballSystemUserDO footballUser = findFootballUser(userId);
        if (footballUser == null || StrUtil.isBlank(footballUser.getUsername())) {
            return null;
        }
        SysUserDO byUsername = sysUserMapper.selectOne(new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<SysUserDO>()
                .eq(SysUserDO::getUsername, footballUser.getUsername())
                .last("LIMIT 1"));
        return byUsername == null ? null : byUsername.getPosition();
    }

    /**
     * Normalize stored member user id (legacy {@code sys_user.id} or Football {@code system_users.id})
     * to the id expected by UserSelect / perf create APIs.
     */
    public Long resolvePresentableUserId(Long storedUserId) {
        if (storedUserId == null) {
            return null;
        }
        if (findFootballUser(storedUserId) != null) {
            return storedUserId;
        }
        SysUserDO legacyUser = findLegacyUser(storedUserId);
        if (legacyUser == null || StrUtil.isBlank(legacyUser.getUsername())) {
            return storedUserId;
        }
        try {
            FootballSystemUserDO masterUser = footballOAuth2MasterTokenMapper.selectUserByUsername(legacyUser.getUsername());
            if (masterUser != null && masterUser.getId() != null) {
                return masterUser.getId();
            }
        } catch (Exception ignored) {
            // H2 integration tests have no Football overlay tables.
        }
        return storedUserId;
    }

    public String resolveMemberDisplayName(Long storedUserId, String batchNickname) {
        String bridged = resolveStoredUserDisplayName(storedUserId);
        if (StrUtil.isNotBlank(bridged)) {
            return bridged;
        }
        if (StrUtil.isNotBlank(batchNickname)) {
            return batchNickname;
        }
        String displayName = resolveDisplayName(storedUserId);
        if (StrUtil.isNotBlank(displayName)) {
            return displayName;
        }
        Long presentableUserId = resolvePresentableUserId(storedUserId);
        if (presentableUserId != null && !presentableUserId.equals(storedUserId)) {
            displayName = resolveDisplayName(presentableUserId);
            if (StrUtil.isNotBlank(displayName)) {
                return displayName;
            }
        }
        return "用户" + storedUserId;
    }

    /**
     * Resolve display name for ids stored in OA tables (IP 组长、成员等).
     * When a legacy {@code sys_user} row exists, prefer Football profile matched by username
     * instead of numeric id (avoids wrong user / garbled name when ids diverge).
     */
    private String resolveStoredUserDisplayName(Long storedUserId) {
        if (storedUserId == null) {
            return null;
        }
        SysUserDO legacyUser = findLegacyUser(storedUserId);
        if (legacyUser == null || StrUtil.isBlank(legacyUser.getUsername())) {
            return null;
        }
        FootballSystemUserDO footballById = findFootballUser(storedUserId);
        if (footballById != null && Objects.equals(legacyUser.getUsername(), footballById.getUsername())) {
            return StrUtil.blankToDefault(footballById.getNickname(), footballById.getUsername());
        }
        try {
            FootballSystemUserDO masterUser = footballOAuth2MasterTokenMapper.selectUserByUsername(legacyUser.getUsername());
            if (masterUser != null) {
                return StrUtil.blankToDefault(masterUser.getNickname(), masterUser.getUsername());
            }
        } catch (Exception ignored) {
            // H2 integration tests have no Football overlay tables.
        }
        return StrUtil.blankToDefault(legacyUser.getNickname(), legacyUser.getUsername());
    }

    public Map<Long, String> loadNicknames(Collection<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return Collections.emptyMap();
        }
        List<Long> ids = userIds.stream().filter(id -> id != null && id > 0).distinct().collect(Collectors.toList());
        if (ids.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<Long, String> names = new HashMap<>();
        try {
            List<FootballSystemUserDO> masterUsers = footballOAuth2MasterTokenMapper.selectDisplayUsersByIds(ids);
            if (masterUsers != null) {
                masterUsers.forEach(user -> names.put(user.getId(),
                        StrUtil.blankToDefault(user.getNickname(), user.getUsername())));
            }
        } catch (Exception ignored) {
            // H2 test profile has no wd.system_users overlay.
        }
        List<Long> missingIds = ids.stream().filter(id -> !names.containsKey(id)).collect(Collectors.toList());
        if (!missingIds.isEmpty()) {
            try {
                List<FootballSystemUserDO> footballUsers = footballSystemUserLookupMapper.selectNicknamesByIds(missingIds);
                if (footballUsers != null) {
                    footballUsers.forEach(user -> names.put(user.getId(),
                            StrUtil.blankToDefault(user.getNickname(), user.getUsername())));
                }
            } catch (Exception ignored) {
                // H2 test profile has no shenyu-system system_users.
            }
        }
        missingIds = ids.stream().filter(id -> !names.containsKey(id)).collect(Collectors.toList());
        if (!missingIds.isEmpty()) {
            List<SysUserDO> legacyUsers = sysUserMapper.selectBatchIds(missingIds);
            if (legacyUsers != null) {
                legacyUsers.forEach(user -> names.put(user.getId(),
                        StrUtil.blankToDefault(user.getNickname(), user.getUsername())));
            }
        }
        return names;
    }

    /**
     * Whether the user (or an id-equivalent legacy/Football account) holds {@code roleCode}.
     * Checks {@code sys_user_role} then wd / shenyu-system {@code system_role}.
     */
    public boolean hasRoleCode(Long userId, Long tenantId, String roleCode) {
        if (userId == null || StrUtil.isBlank(roleCode)) {
            return false;
        }
        for (Long candidateId : resolveRoleCheckUserIds(userId, tenantId)) {
            if (hasRoleCodeForUserId(candidateId, roleCode)) {
                return true;
            }
        }
        return false;
    }

    public void assertHasRoleCode(Long userId, Long tenantId, String roleCode, ErrorCode errorCode) {
        if (!hasRoleCode(userId, tenantId, roleCode)) {
            throw new ServiceException(errorCode);
        }
    }

    /**
     * Tenant users holding {@code roleCode} (sys_role ∪ Football system_role on master/system DS).
     * Ids are normalized to presentable Football {@code system_users.id} when possible.
     */
    public List<Long> listPresentableUserIdsByRoleCode(Long tenantId, String roleCode) {
        if (tenantId == null || StrUtil.isBlank(roleCode)) {
            return Collections.emptyList();
        }
        Set<Long> rawIds = new LinkedHashSet<>();
        try {
            List<SysUserDO> legacyUsers = sysUserTokenMapper.selectUsersByRoleCode(tenantId, roleCode);
            if (legacyUsers != null) {
                legacyUsers.forEach(u -> {
                    if (u.getId() != null) {
                        rawIds.add(u.getId());
                    }
                });
            }
        } catch (Exception ignored) {
            // ignore
        }
        try {
            List<Long> masterIds = footballOAuth2MasterTokenMapper.selectUserIdsByRoleCode(tenantId, roleCode);
            if (masterIds != null) {
                rawIds.addAll(masterIds);
            }
        } catch (Exception ignored) {
            // H2 / no overlay
        }
        try {
            List<Long> systemIds = footballOAuth2TokenMapper.selectUserIdsByRoleCode(tenantId, roleCode);
            if (systemIds != null) {
                rawIds.addAll(systemIds);
            }
        } catch (Exception ignored) {
            // H2 / no shenyu-system
        }
        return rawIds.stream()
                .map(this::resolvePresentableUserId)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
    }

    private Set<Long> resolveRoleCheckUserIds(Long userId, Long tenantId) {
        Set<Long> ids = new LinkedHashSet<>();
        ids.add(userId);
        SysUserDO legacyUser = findLegacyUser(userId);
        if (legacyUser != null) {
            ids.add(legacyUser.getId());
            addFootballIdsByUsername(legacyUser.getUsername(), ids);
            if (tenantId != null && StrUtil.isNotBlank(legacyUser.getUsername())) {
                SysUserDO tenantUser = sysUserTokenMapper.selectUserByUsernameAndTenant(
                        legacyUser.getUsername(), tenantId);
                if (tenantUser != null) {
                    ids.add(tenantUser.getId());
                }
            }
        }
        FootballSystemUserDO footballUser = findFootballUser(userId);
        if (footballUser != null) {
            ids.add(footballUser.getId());
            addFootballIdsByUsername(footballUser.getUsername(), ids);
            if (tenantId != null && StrUtil.isNotBlank(footballUser.getUsername())) {
                SysUserDO oaUser = sysUserTokenMapper.selectUserByUsernameAndTenant(
                        footballUser.getUsername(), tenantId);
                if (oaUser != null) {
                    ids.add(oaUser.getId());
                }
            }
        }
        return ids;
    }

    private void addFootballIdsByUsername(String username, Set<Long> ids) {
        if (StrUtil.isBlank(username)) {
            return;
        }
        try {
            FootballSystemUserDO masterUser = footballOAuth2MasterTokenMapper.selectUserByUsername(username);
            if (masterUser != null && masterUser.getId() != null) {
                ids.add(masterUser.getId());
            }
        } catch (Exception ignored) {
            // H2
        }
        try {
            FootballSystemUserDO systemUser = footballOAuth2TokenMapper.selectUserByUsername(username);
            if (systemUser != null && systemUser.getId() != null) {
                ids.add(systemUser.getId());
            }
        } catch (Exception ignored) {
            // H2
        }
    }

    private boolean hasRoleCodeForUserId(Long userId, String roleCode) {
        if (userId == null) {
            return false;
        }
        try {
            List<SysRoleDO> legacyRoles = sysUserTokenMapper.selectRolesByUserId(userId);
            if (legacyRoles != null && legacyRoles.stream().anyMatch(r -> roleCode.equals(r.getCode()))) {
                return true;
            }
        } catch (Exception ignored) {
            // ignore
        }
        try {
            List<FootballSystemRoleDO> masterRoles = footballOAuth2MasterTokenMapper.selectRolesByUserId(userId);
            if (masterRoles != null && masterRoles.stream().anyMatch(r -> roleCode.equals(r.getCode()))) {
                return true;
            }
        } catch (Exception ignored) {
            // H2
        }
        try {
            List<FootballSystemRoleDO> systemRoles = footballOAuth2TokenMapper.selectRolesByUserId(userId);
            return systemRoles != null && systemRoles.stream().anyMatch(r -> roleCode.equals(r.getCode()));
        } catch (Exception ignored) {
            return false;
        }
    }

    private boolean isFootballUserEnabled(FootballSystemUserDO user) {
        return user.getStatus() == null || user.getStatus() == 0;
    }
}
