package cn.iocoder.yudao.module.oa.service.support;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.biz.system.permission.PermissionCommonApi;
import cn.iocoder.yudao.framework.common.biz.system.user.AdminUserApi;
import cn.iocoder.yudao.framework.common.biz.system.user.dto.AdminUserRespDTO;
import cn.iocoder.yudao.framework.common.exception.ErrorCode;
import cn.iocoder.yudao.framework.common.exception.OaErrorCodes;
import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.module.oa.dal.dataobject.auth.FootballSystemRoleDO;
import cn.iocoder.yudao.module.oa.dal.dataobject.auth.FootballSystemUserDO;
import cn.iocoder.yudao.module.oa.dal.dataobject.auth.SysRoleDO;
import cn.iocoder.yudao.module.oa.dal.dataobject.auth.SysUserDO;
import cn.iocoder.yudao.module.oa.dal.mysql.auth.FootballOAuth2MasterTokenMapper;
import cn.iocoder.yudao.module.oa.dal.mysql.auth.FootballOAuth2TokenMapper;
import cn.iocoder.yudao.module.oa.dal.mysql.auth.SysUserMapper;
import cn.iocoder.yudao.module.oa.dal.mysql.auth.SysUserTokenMapper;
import cn.iocoder.yudao.module.oa.dal.mysql.system.FootballSystemRoleLookupMapper;
import cn.iocoder.yudao.module.oa.dal.mysql.system.FootballSystemUserLookupMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Validates and resolves user ids submitted from {@code UserSelect}.
 * SSOT = shenyu-system {@code system_users.id}; legacy {@code sys_user} / wd master 仅 username 桥接读。
 */
@Component
@RequiredArgsConstructor
public class FootballSystemUserValidator {

    private final FootballOAuth2MasterTokenMapper footballOAuth2MasterTokenMapper;
    private final FootballOAuth2TokenMapper footballOAuth2TokenMapper;
    private final FootballSystemUserLookupMapper footballSystemUserLookupMapper;
    private final FootballSystemRoleLookupMapper footballSystemRoleLookupMapper;
    private final FootballSystemUserSystemReader footballSystemUserSystemReader;
    private final AdminUserApi adminUserApi;
    private final PermissionCommonApi permissionCommonApi;
    private final SysUserMapper sysUserMapper;
    private final SysUserTokenMapper sysUserTokenMapper;

    public FootballSystemUserDO findFootballUser(Long userId) {
        if (userId == null) {
            return null;
        }
        try {
            FootballSystemUserDO systemUser = footballSystemUserSystemReader.findById(userId);
            if (systemUser != null) {
                return systemUser;
            }
        } catch (Exception ignored) {
            // H2 integration tests have no Football overlay tables.
        }
        return findMasterFootballUser(userId);
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
        assertInTenant(userId, tenantId, OaErrorCodes.ENTITY_NOT_EXISTS, notFoundMessage);
    }

    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public void assertInTenant(Long userId, Long tenantId, ErrorCode notFoundCode) {
        assertInTenant(userId, tenantId, notFoundCode, notFoundCode.getMsg());
    }

    /**
     * Suspend outer {@code @Transactional} so {@code @DS("system")} lookups work while validating
     * UserSelect ids that only exist in shenyu-system {@code system_users}.
     */
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public void assertInTenant(Long userId, Long tenantId, ErrorCode notFoundCode, String notFoundMessage) {
        if (userId == null) {
            return;
        }
        FootballSystemUserDO footballUser = findFootballUser(userId);
        if (footballUser == null) {
            footballUser = findFootballUserByStoredIdBridge(userId);
        }
        if (footballUser != null) {
            if (!Objects.equals(tenantId, footballUser.getTenantId())) {
                throw new ServiceException(OaErrorCodes.TENANT_FORBIDDEN);
            }
            return;
        }
        SysUserDO legacyUser = findLegacyUser(userId);
        if (legacyUser == null) {
            throw new ServiceException(notFoundCode.getCode(), notFoundMessage);
        }
        if (!Objects.equals(tenantId, legacyUser.getTenantId())) {
            throw new ServiceException(OaErrorCodes.TENANT_FORBIDDEN);
        }
    }

    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public void assertEnabledInTenant(Long userId, Long tenantId, String notFoundMessage) {
        if (userId == null) {
            return;
        }
        assertEnabledViaFeign(userId, tenantId, notFoundMessage);
    }

    public String resolveDisplayName(Long userId) {
        if (userId == null) {
            return null;
        }
        FootballSystemUserDO footballUser = findFootballUser(userId);
        if (footballUser != null) {
            String resolved = resolveNicknameFromProfile(footballUser);
            if (StrUtil.isNotBlank(resolved)) {
                return resolved;
            }
        }
        SysUserDO legacyUser = findLegacyUser(userId);
        if (legacyUser == null) {
            return null;
        }
        return StrUtil.blankToDefault(legacyUser.getNickname(), legacyUser.getUsername());
    }

    public String resolveUsername(Long userId) {
        if (userId == null) {
            return null;
        }
        FootballSystemUserDO footballUser = findFootballUser(userId);
        if (footballUser != null && StrUtil.isNotBlank(footballUser.getUsername())) {
            return footballUser.getUsername();
        }
        SysUserDO legacyUser = findLegacyUser(userId);
        return legacyUser == null ? null : legacyUser.getUsername();
    }

    /**
     * Detect nicknames corrupted during non-utf8 import (MySQL stores unmappable CJK as literal {@code ?}).
     */
    static boolean isGarbledNickname(String nickname) {
        if (StrUtil.isBlank(nickname)) {
            return false;
        }
        if (nickname.chars().allMatch(c -> c == '?' || c == '？')) {
            return true;
        }
        // Partial import corruption, e.g. ???02 for 测试号02
        return nickname.matches("^[?？]+.+");
    }

    private String resolveNicknameFromProfile(FootballSystemUserDO profile) {
        if (profile == null) {
            return null;
        }
        if (!isGarbledNickname(profile.getNickname())) {
            return StrUtil.blankToDefault(profile.getNickname(), profile.getUsername());
        }
        if (StrUtil.isNotBlank(profile.getUsername())) {
            try {
                FootballSystemUserDO masterUser = footballOAuth2MasterTokenMapper.selectUserByUsername(profile.getUsername());
                if (masterUser != null && !isGarbledNickname(masterUser.getNickname())) {
                    return StrUtil.blankToDefault(masterUser.getNickname(), masterUser.getUsername());
                }
            } catch (Exception ignored) {
                // H2 test profile has no wd.system_users overlay.
            }
        }
        return profile.getUsername();
    }

    private void putResolvedNickname(Map<Long, String> names, FootballSystemUserDO user) {
        if (user == null || user.getId() == null) {
            return;
        }
        String resolved = resolveNicknameFromProfile(user);
        if (StrUtil.isNotBlank(resolved)) {
            names.put(user.getId(), resolved);
        }
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
        FootballSystemUserDO footballUser = findFootballUser(storedUserId);
        if (footballUser != null && StrUtil.isNotBlank(footballUser.getUsername())) {
            FootballSystemUserDO systemUser = footballSystemUserSystemReader.findByUsername(footballUser.getUsername());
            if (systemUser != null && systemUser.getId() != null) {
                return systemUser.getId();
            }
            return storedUserId;
        }
        SysUserDO legacyUser = findLegacyUser(storedUserId);
        if (legacyUser == null || StrUtil.isBlank(legacyUser.getUsername())) {
            return storedUserId;
        }
        try {
            FootballSystemUserDO masterUser = footballOAuth2MasterTokenMapper.selectUserByUsername(legacyUser.getUsername());
            if (masterUser != null && masterUser.getId() != null) {
                FootballSystemUserDO systemUser = footballSystemUserSystemReader.findByUsername(legacyUser.getUsername());
                if (systemUser != null && systemUser.getId() != null) {
                    return systemUser.getId();
                }
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
            String resolved = resolveNicknameFromProfile(footballById);
            if (StrUtil.isNotBlank(resolved)) {
                return resolved;
            }
        }
        try {
            FootballSystemUserDO masterUser = footballOAuth2MasterTokenMapper.selectUserByUsername(legacyUser.getUsername());
            if (masterUser != null) {
                String resolved = resolveNicknameFromProfile(masterUser);
                if (StrUtil.isNotBlank(resolved)) {
                    return resolved;
                }
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
            List<FootballSystemUserDO> footballUsers = footballSystemUserLookupMapper.selectNicknamesByIds(ids);
            if (footballUsers != null) {
                footballUsers.forEach(user -> putResolvedNickname(names, user));
            }
        } catch (Exception ignored) {
            // H2 test profile has no shenyu-system system_users.
        }
        List<Long> missingIds = ids.stream().filter(id -> !names.containsKey(id)).collect(Collectors.toList());
        if (!missingIds.isEmpty()) {
            try {
                List<FootballSystemUserDO> masterUsers = footballOAuth2MasterTokenMapper.selectDisplayUsersByIds(missingIds);
                if (masterUsers != null) {
                    masterUsers.forEach(user -> putResolvedNickname(names, user));
                }
            } catch (Exception ignored) {
                // H2 test profile has no wd.system_users overlay.
            }
        }
        missingIds = ids.stream().filter(id -> !names.containsKey(id)).collect(Collectors.toList());
        if (!missingIds.isEmpty()) {
            List<SysUserDO> legacyUsers = sysUserMapper.selectBatchIds(missingIds);
            if (legacyUsers != null) {
                legacyUsers.forEach(user -> {
                    if (user.getId() == null) {
                        return;
                    }
                    String resolved = isGarbledNickname(user.getNickname())
                            ? user.getUsername()
                            : StrUtil.blankToDefault(user.getNickname(), user.getUsername());
                    if (StrUtil.isNotBlank(resolved)) {
                        names.put(user.getId(), resolved);
                    }
                });
            }
        }
        return names;
    }

    /**
     * Whether the user holds {@code roleCode}. G-SYS-02 cutover: Feign {@link PermissionCommonApi} only.
     */
    public boolean hasRoleCode(Long userId, Long tenantId, String roleCode) {
        if (userId == null || StrUtil.isBlank(roleCode)) {
            return false;
        }
        Set<Long> candidateIds = resolveRoleCheckUserIds(userId, tenantId);
        Boolean feignAvailable = null;
        for (Long candidateId : candidateIds) {
            Boolean feignResult = hasRoleCodeViaFeign(candidateId, roleCode);
            if (feignResult == null) {
                feignAvailable = false;
                break;
            }
            feignAvailable = true;
            if (feignResult) {
                return true;
            }
        }
        if (Boolean.TRUE.equals(feignAvailable)) {
            return false;
        }
        throw rpcUnavailable();
    }

    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public void assertHasRoleCode(Long userId, Long tenantId, String roleCode, ErrorCode errorCode) {
        if (!hasRoleCode(userId, tenantId, roleCode)) {
            throw new ServiceException(errorCode);
        }
    }

    /**
     * Normalize UserSelect / legacy id to shenyu-system {@code system_users.id} (Football SSOT).
     */
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public Long resolveStorableUserId(Long submittedUserId, Long tenantId) {
        if (submittedUserId == null) {
            return null;
        }
        try {
            FootballSystemUserDO directSystemUser = footballSystemUserSystemReader.findById(submittedUserId);
            if (directSystemUser != null && Objects.equals(tenantId, directSystemUser.getTenantId())) {
                return directSystemUser.getId();
            }
        } catch (Exception ignored) {
            // H2 integration tests have no Football overlay tables.
        }
        FootballSystemUserDO footballUser = findFootballUser(submittedUserId);
        if (footballUser == null) {
            footballUser = findFootballUserByStoredIdBridge(submittedUserId);
        }
        if (footballUser != null && StrUtil.isNotBlank(footballUser.getUsername())) {
            try {
                FootballSystemUserDO systemUser = footballSystemUserSystemReader.findByUsername(
                        footballUser.getUsername());
                if (systemUser != null && systemUser.getId() != null
                        && Objects.equals(tenantId, systemUser.getTenantId())) {
                    return systemUser.getId();
                }
            } catch (Exception ignored) {
                // H2 integration tests have no Football overlay tables.
            }
            if (Objects.equals(tenantId, footballUser.getTenantId())) {
                return footballUser.getId();
            }
        }
        SysUserDO legacyUser = findLegacyUser(submittedUserId);
        if (legacyUser != null && StrUtil.isNotBlank(legacyUser.getUsername())) {
            try {
                FootballSystemUserDO systemUser = footballSystemUserSystemReader.findByUsername(legacyUser.getUsername());
                if (systemUser != null && systemUser.getId() != null
                        && Objects.equals(tenantId, systemUser.getTenantId())) {
                    return systemUser.getId();
                }
            } catch (Exception ignored) {
                // H2 integration tests have no Football overlay tables.
            }
            if (Objects.equals(tenantId, legacyUser.getTenantId())) {
                return legacyUser.getId();
            }
        }
        return submittedUserId;
    }

    /**
     * Tenant-wide enabled users for selector UIs that must NOT inherit Football dept/self data scope
     * (e.g. IP 组「添加成员」). G-SYS-01 cutover: Feign-only.
     */
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public List<FootballSystemUserDO> listEnabledUsersInTenant(Long tenantId) {
        if (tenantId == null) {
            return Collections.emptyList();
        }
        List<FootballSystemUserDO> feignUsers = loadEnabledUsersViaFeign(tenantId);
        if (feignUsers != null) {
            return feignUsers;
        }
        throw rpcUnavailable();
    }

    /**
     * G-SYS-01 cutover: Feign {@link AdminUserApi#getSimpleUserList} only.
     */
    List<FootballSystemUserDO> loadEnabledUsersViaFeign(Long tenantId) {
        if (adminUserApi == null || tenantId == null) {
            return null;
        }
        try {
            CommonResult<List<AdminUserRespDTO>> result = adminUserApi.getSimpleUserList(null, 0, null);
            if (result == null || !result.isSuccess()) {
                return null;
            }
            List<AdminUserRespDTO> users = result.getData();
            if (users == null || users.isEmpty()) {
                return List.of();
            }
            Map<Long, FootballSystemUserDO> byId = new LinkedHashMap<>();
            for (AdminUserRespDTO dto : users) {
                if (dto == null || dto.getId() == null || !isRpcUserEnabled(dto.getStatus())) {
                    continue;
                }
                FootballSystemUserDO user = new FootballSystemUserDO();
                user.setId(dto.getId());
                user.setTenantId(tenantId);
                user.setNickname(dto.getNickname());
                user.setStatus(dto.getStatus());
                byId.putIfAbsent(dto.getId(), user);
            }
            return List.copyOf(byId.values());
        } catch (Exception ignored) {
            return null;
        }
    }

    /**
     * Tenant users holding {@code roleCode}; ids normalized to shenyu-system {@code system_users.id}.
     * Role SSOT = shenyu-system {@code system_role}; legacy sys_role / wd master union for username 桥接.
     */
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public List<Long> listPresentableUserIdsByRoleCode(Long tenantId, String roleCode) {
        if (tenantId == null || StrUtil.isBlank(roleCode)) {
            return Collections.emptyList();
        }
        // legacy → master → system：须先查 wd（primary/master），再切 shenyu-system。
        // 若先 @DS("system")，动态数据源线程会粘滞，legacy/master 查询会落到错误库并静默为空。
        Set<Long> rawIds = new LinkedHashSet<>();
        List<Long> feignIds = listUserIdsByRoleCodeViaFeign(tenantId, roleCode);
        if (feignIds == null) {
            throw rpcUnavailable();
        }
        rawIds.addAll(feignIds);
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
        return rawIds.stream()
                .map(this::resolvePresentableUserId)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
    }

    /** 按 username 桥接 wd master / shenyu-system / legacy sys_user */
    private FootballSystemUserDO findFootballUserByStoredIdBridge(Long userId) {
        String username = resolveUsernameForBridge(userId);
        if (StrUtil.isBlank(username)) {
            return null;
        }
        try {
            FootballSystemUserDO systemUser = footballSystemUserSystemReader.findByUsername(username);
            if (systemUser != null) {
                return systemUser;
            }
        } catch (Exception ignored) {
            // H2
        }
        try {
            FootballSystemUserDO masterUser = footballOAuth2MasterTokenMapper.selectUserByUsername(username);
            if (masterUser != null) {
                return masterUser;
            }
        } catch (Exception ignored) {
            // H2
        }
        return null;
    }

    private String resolveUsernameForBridge(Long userId) {
        try {
            String username = footballSystemUserSystemReader.findUsernameById(userId);
            if (StrUtil.isNotBlank(username)) {
                return username;
            }
        } catch (Exception ignored) {
            // H2
        }
        SysUserDO legacyUser = findLegacyUser(userId);
        if (legacyUser != null && StrUtil.isNotBlank(legacyUser.getUsername())) {
            return legacyUser.getUsername();
        }
        return null;
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
            FootballSystemUserDO systemUser = footballSystemUserSystemReader.findByUsername(username);
            if (systemUser != null && systemUser.getId() != null) {
                ids.add(systemUser.getId());
            }
        } catch (Exception ignored) {
            // H2
        }
    }

    private static boolean isRpcUserEnabled(Integer status) {
        return status == null || status == 0;
    }

    /**
     * G-SYS-02 cutover: Feign {@link AdminUserApi#getUser} + {@link AdminUserApi#validateUserList} only.
     */
    void assertEnabledViaFeign(Long userId, Long tenantId, String notFoundMessage) {
        if (adminUserApi == null || tenantId == null) {
            throw rpcUnavailable();
        }
        try {
            Long storableId = resolveStorableUserId(userId, tenantId);
            CommonResult<AdminUserRespDTO> userResult = adminUserApi.getUser(storableId);
            if (userResult == null || !userResult.isSuccess() || userResult.getData() == null) {
                throw new ServiceException(OaErrorCodes.ENTITY_NOT_EXISTS.getCode(), notFoundMessage);
            }
            AdminUserRespDTO user = userResult.getData();
            if (!Objects.equals(tenantId, user.getTenantId())) {
                throw new ServiceException(OaErrorCodes.TENANT_FORBIDDEN);
            }
            if (!isRpcUserEnabled(user.getStatus())) {
                throw new ServiceException(OaErrorCodes.ENTITY_DISABLED);
            }
            CommonResult<Boolean> validResult = adminUserApi.validateUserList(List.of(storableId));
            if (validResult == null || !validResult.isSuccess()) {
                throw rpcUnavailable();
            }
            if (!Boolean.TRUE.equals(validResult.getData())) {
                throw new ServiceException(OaErrorCodes.ENTITY_DISABLED);
            }
        } catch (ServiceException ex) {
            throw ex;
        } catch (Exception ex) {
            throw rpcUnavailable();
        }
    }

    /**
     * G-SYS-02: Feign {@link PermissionCommonApi#hasAnyRoles}; {@code null} = unavailable (fall back @DS).
     */
    Boolean hasRoleCodeViaFeign(Long userId, String roleCode) {
        if (permissionCommonApi == null || userId == null || StrUtil.isBlank(roleCode)) {
            return null;
        }
        try {
            CommonResult<Boolean> result = permissionCommonApi.hasAnyRoles(userId, roleCode);
            if (result == null || !result.isSuccess()) {
                return null;
            }
            return Boolean.TRUE.equals(result.getData());
        } catch (Exception ignored) {
            return null;
        }
    }

    /**
     * G-SYS-02: roleCode→roleId (@DS) + {@link AdminUserApi#getUserListByRoleId}; {@code null} = Feign unavailable.
     */
    List<Long> listUserIdsByRoleCodeViaFeign(Long tenantId, String roleCode) {
        if (adminUserApi == null || tenantId == null || StrUtil.isBlank(roleCode)) {
            return null;
        }
        try {
            Long roleId = resolveRoleIdByCode(tenantId, roleCode);
            if (roleId == null) {
                return List.of();
            }
            CommonResult<List<AdminUserRespDTO>> result = adminUserApi.getUserListByRoleId(roleId);
            if (result == null || !result.isSuccess()) {
                return null;
            }
            List<AdminUserRespDTO> users = result.getData();
            if (users == null || users.isEmpty()) {
                return List.of();
            }
            return users.stream()
                    .filter(u -> u != null && u.getId() != null && isRpcUserEnabled(u.getStatus()))
                    .map(AdminUserRespDTO::getId)
                    .collect(Collectors.toList());
        } catch (Exception ignored) {
            return null;
        }
    }

    private Long resolveRoleIdByCode(Long tenantId, String roleCode) {
        try {
            Long roleId = footballSystemRoleLookupMapper.selectRoleIdByCode(tenantId, roleCode);
            if (roleId != null) {
                return roleId;
            }
        } catch (Exception ignored) {
            // H2 / no shenyu-system
        }
        try {
            return footballOAuth2MasterTokenMapper.selectRoleIdByCode(tenantId, roleCode);
        } catch (Exception ignored) {
            return null;
        }
    }

    private static ServiceException rpcUnavailable() {
        return new ServiceException(OaErrorCodes.BAD_REQUEST.getCode(),
                "用户/权限服务不可用，请确认 system-server 已启动");
    }
}
