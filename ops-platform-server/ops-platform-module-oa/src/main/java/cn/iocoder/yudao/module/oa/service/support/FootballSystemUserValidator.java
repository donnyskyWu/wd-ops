package cn.iocoder.yudao.module.oa.service.support;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.exception.OaErrorCodes;
import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.module.oa.dal.dataobject.auth.FootballSystemUserDO;
import cn.iocoder.yudao.module.oa.dal.dataobject.auth.SysUserDO;
import cn.iocoder.yudao.module.oa.dal.mysql.auth.FootballOAuth2MasterTokenMapper;
import cn.iocoder.yudao.module.oa.dal.mysql.auth.SysUserMapper;
import cn.iocoder.yudao.module.oa.dal.mysql.system.FootballSystemUserLookupMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Validates and resolves user ids submitted from {@code UserSelect} (Football {@code system_users.id})
 * with legacy {@code sys_user} fallback for H2 integration tests.
 */
@Component
@RequiredArgsConstructor
public class FootballSystemUserValidator {

    private final FootballOAuth2MasterTokenMapper footballOAuth2MasterTokenMapper;
    private final FootballSystemUserLookupMapper footballSystemUserLookupMapper;
    private final SysUserMapper sysUserMapper;

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
        SysUserDO legacyUser = findLegacyUser(userId);
        return legacyUser == null ? null : legacyUser.getPosition();
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

    private boolean isFootballUserEnabled(FootballSystemUserDO user) {
        return user.getStatus() == null || user.getStatus() == 0;
    }
}
