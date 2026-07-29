package cn.iocoder.yudao.module.oa.service.auth;

import cn.iocoder.yudao.framework.common.exception.OaErrorCodes;
import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.oa.dal.dataobject.account.AccountDO;
import cn.iocoder.yudao.module.oa.dal.dataobject.ipgroup.IpGroupDO;
import cn.iocoder.yudao.module.oa.dal.dataobject.ipgroup.IpGroupMemberDO;
import cn.iocoder.yudao.module.oa.dal.mysql.account.AccountMapper;
import cn.iocoder.yudao.module.oa.dal.mysql.ipgroup.IpGroupMapper;
import cn.iocoder.yudao.module.oa.dal.mysql.ipgroup.IpGroupMemberMapper;
import cn.iocoder.yudao.module.oa.framework.auth.DataScopeSupport;
import cn.iocoder.yudao.module.oa.framework.auth.LoginUser;
import cn.iocoder.yudao.module.oa.framework.auth.LoginUserContext;
import cn.iocoder.yudao.module.oa.service.ipgroup.IpGroupAccessSupport;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.support.SFunction;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * OPS 数据范围中心组件：IP 组 / 账号级 fail-closed 过滤。
 */
@Component
@RequiredArgsConstructor
public class OpsDataScopeSupport {

    private static final long NO_ACCESS_SENTINEL = -1L;

    /** OA/租户/Football 平台管理员角色；不含 OPS_LEADER（dataScope=ALL 但非系统管理员） */
    private static final Set<String> OA_TENANT_ADMIN_ROLE_CODES = Set.of(
            "OA_ADMIN", "TENANT_ADMIN", "super_admin");

    private static OpsDataScopeSupport instance;

    private final IpGroupAccessSupport ipGroupAccessSupport;
    private final IpGroupMapper ipGroupMapper;
    private final IpGroupMemberMapper ipGroupMemberMapper;
    private final AccountMapper accountMapper;

    public enum AccountScopeMode {
        /** admin：不过滤 */
        ALL,
        /**
         * 账号类菜单默认：成员组 ∪ 组长管辖组（「所在 IP 组」含担任组长的组，即使未写入 member 表）。
         */
        MEMBER_GROUPS,
        /** 组长管辖组下账号（IP 组管理写范围等） */
        LED_GROUPS
    }

    @PostConstruct
    void registerInstance() {
        instance = this;
    }

    public static OpsDataScopeSupport getInstance() {
        return instance;
    }

    /**
     * 数据权限「系统管理员」：OA/租户/Football super_admin 全量；不含 OPS_LEADER 等 dataScope=ALL 角色。
     */
    public static boolean hasOaTenantAdminAuthority(Set<String> authorities) {
        if (authorities == null || authorities.isEmpty()) {
            return false;
        }
        for (String code : OA_TENANT_ADMIN_ROLE_CODES) {
            if (authorities.contains(code) || authorities.contains("ROLE_" + code)) {
                return true;
            }
        }
        return false;
    }

    public boolean isOaTenantAdmin(LoginUser user) {
        return user != null && hasOaTenantAdminAuthority(user.getAuthorities());
    }

    /** @deprecated 语义同 {@link #isOaTenantAdmin}，保留兼容旧调用方 */
    public boolean isSystemAdmin(LoginUser user) {
        return isOaTenantAdmin(user);
    }

    public boolean isIpGroupLeader(LoginUser user) {
        return user != null && Boolean.TRUE.equals(user.getIpGroupLeader());
    }

    /** 登录时或运行时：oa_ip_group_member 所属小组 */
    public Set<Long> resolveMemberIpGroupIds(Long userId, String username, Long tenantId) {
        Set<Long> userIds = ipGroupAccessSupport.resolveMembershipUserIds(userId, username, tenantId);
        if (userIds.isEmpty() || tenantId == null) {
            return Set.of();
        }
        Set<Long> groupIds = new LinkedHashSet<>();
        ipGroupMemberMapper.selectList(new LambdaQueryWrapper<IpGroupMemberDO>()
                        .eq(IpGroupMemberDO::getTenantId, tenantId)
                        .in(IpGroupMemberDO::getUserId, userIds))
                .forEach(member -> {
                    if (member.getIpGroupId() != null) {
                        groupIds.add(member.getIpGroupId());
                    }
                });
        return groupIds;
    }

    /** 登录时或运行时：leader_user_id 或 member.is_leader=1 的管辖组 */
    public Set<Long> resolveLedIpGroupIds(Long userId, String username, Long tenantId) {
        Set<Long> userIds = ipGroupAccessSupport.resolveMembershipUserIds(userId, username, tenantId);
        if (userIds.isEmpty() || tenantId == null) {
            return Set.of();
        }
        Map<Long, Boolean> ledGroups = new LinkedHashMap<>();
        ipGroupMapper.selectList(new LambdaQueryWrapper<IpGroupDO>()
                        .eq(IpGroupDO::getTenantId, tenantId)
                        .eq(IpGroupDO::getGroupType, 2)
                        .eq(IpGroupDO::getStatus, 1)
                        .in(IpGroupDO::getLeaderUserId, userIds)
                        .orderByAsc(IpGroupDO::getSortOrder)
                        .orderByAsc(IpGroupDO::getId))
                .forEach(group -> ledGroups.put(group.getId(), Boolean.TRUE));

        ipGroupMemberMapper.selectList(new LambdaQueryWrapper<IpGroupMemberDO>()
                        .eq(IpGroupMemberDO::getTenantId, tenantId)
                        .in(IpGroupMemberDO::getUserId, userIds)
                        .eq(IpGroupMemberDO::getIsLeader, 1))
                .forEach(member -> {
                    if (ledGroups.containsKey(member.getIpGroupId())) {
                        return;
                    }
                    IpGroupDO group = ipGroupMapper.selectById(member.getIpGroupId());
                    if (group != null && Objects.equals(group.getTenantId(), tenantId)
                            && Objects.equals(group.getGroupType(), 2)
                            && Objects.equals(group.getStatus(), 1)) {
                        ledGroups.put(group.getId(), Boolean.TRUE);
                    }
                });
        return ledGroups.keySet();
    }

    /** 成员组 ∪ 组长组；admin 返回 null 表示无限制 */
    public Set<Long> resolveAccessibleIpGroupIds(Long userId, String username, Long tenantId) {
        LoginUser user = LoginUserContext.get();
        if (user != null && isOaTenantAdmin(user)) {
            return null;
        }
        Set<Long> accessible = new LinkedHashSet<>(resolveMemberIpGroupIds(userId, username, tenantId));
        accessible.addAll(resolveLedIpGroupIds(userId, username, tenantId));
        return accessible;
    }

    /**
     * 可访问账号 ID；admin 返回 null（不过滤）；无权限组返回 {-1} fail-closed。
     */
    public Set<Long> resolveAccessibleAccountIds(Long tenantId, AccountScopeMode mode) {
        LoginUser user = LoginUserContext.get();
        if (isOaTenantAdmin(user)) {
            return null;
        }
        Set<Long> groupIds = resolveScopeIpGroupIds(user, tenantId, mode);
        if (groupIds.isEmpty()) {
            return Set.of(NO_ACCESS_SENTINEL);
        }
        Set<Long> accountIds = accountMapper.selectList(new LambdaQueryWrapper<AccountDO>()
                        .eq(AccountDO::getTenantId, tenantId)
                        .in(AccountDO::getIpGroupId, groupIds)
                        .select(AccountDO::getId))
                .stream()
                .map(AccountDO::getId)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        return accountIds.isEmpty() ? Set.of(NO_ACCESS_SENTINEL) : accountIds;
    }

    public <T> void applyIpGroupIdIn(LambdaQueryWrapper<T> wrapper, SFunction<T, Long> ipGroupColumn) {
        LoginUser user = LoginUserContext.get();
        if (isOaTenantAdmin(user)) {
            return;
        }
        Long tenantId = TenantContextHolder.getTenantId();
        Set<Long> groupIds = resolveScopeIpGroupIds(user, tenantId, AccountScopeMode.MEMBER_GROUPS);
        applyIdIn(wrapper, ipGroupColumn, groupIds);
    }

    public <T> void applyAccountIdIn(LambdaQueryWrapper<T> wrapper, SFunction<T, Long> accountColumn,
                                       AccountScopeMode mode) {
        LoginUser user = LoginUserContext.get();
        if (isOaTenantAdmin(user)) {
            return;
        }
        Long tenantId = TenantContextHolder.getTenantId();
        Set<Long> accountIds = resolveAccessibleAccountIds(tenantId, mode);
        if (accountIds == null) {
            return;
        }
        applyIdIn(wrapper, accountColumn, accountIds);
    }

    public void assertAccountReadable(Long accountId) {
        LoginUser user = LoginUserContext.get();
        if (isOaTenantAdmin(user)) {
            return;
        }
        Long tenantId = TenantContextHolder.getTenantId();
        if (tenantId == null) {
            throw new ServiceException(OaErrorCodes.UNAUTHORIZED);
        }
        AccountDO account = accountMapper.selectById(accountId);
        // 公众号 SSOT 可能仅存在于 mp_account + oa_account_ext，由调用方先 resolve 成 AccountDO 再断言
        if (account == null || !Objects.equals(account.getTenantId(), tenantId)) {
            throw new ServiceException(OaErrorCodes.ENTITY_NOT_EXISTS);
        }
        assertAccountInMemberGroups(user, tenantId, account);
    }

    /**
     * 使用已加载/合成的 {@link AccountDO} 做可读校验。
     * <p>公众号详情路径会传入 {@code WechatOfficialAccountResolver} 合成的实体（id=mp_account.id，
     * ipGroupId 来自 oa_account_ext），不得再按 id 回查 oa_account，否则 mp-only 账号会误报 1500。
     */
    public void assertAccountReadable(AccountDO account) {
        LoginUser user = LoginUserContext.get();
        if (isOaTenantAdmin(user)) {
            return;
        }
        Long tenantId = TenantContextHolder.getTenantId();
        if (tenantId == null) {
            throw new ServiceException(OaErrorCodes.UNAUTHORIZED);
        }
        if (account == null || !Objects.equals(account.getTenantId(), tenantId)) {
            throw new ServiceException(OaErrorCodes.ENTITY_NOT_EXISTS);
        }
        assertAccountInMemberGroups(user, tenantId, account);
    }

    private void assertAccountInMemberGroups(LoginUser user, Long tenantId, AccountDO account) {
        Set<Long> groupIds = resolveScopeIpGroupIds(user, tenantId, AccountScopeMode.MEMBER_GROUPS);
        if (groupIds.isEmpty() || account.getIpGroupId() == null || !groupIds.contains(account.getIpGroupId())) {
            throw new ServiceException(OaErrorCodes.FORBIDDEN);
        }
    }

    /** 非 admin：creator = 当前 username；无 username → fail-closed */
    public <T> void applySelfCreator(LambdaQueryWrapper<T> wrapper, SFunction<T, String> creatorColumn) {
        LoginUser user = LoginUserContext.get();
        if (isOaTenantAdmin(user)) {
            return;
        }
        String username = user != null ? user.getUsername() : null;
        if (username == null || username.isBlank()) {
            wrapper.eq(creatorColumn, "__NO_ACCESS__");
            return;
        }
        wrapper.eq(creatorColumn, username);
    }

    /** 非 admin：assignee_id IN resolveMembershipUserIds；空集合 → id=-1 */
    public <T> void applyAssigneeIn(LambdaQueryWrapper<T> wrapper, SFunction<T, Long> assigneeColumn) {
        LoginUser user = LoginUserContext.get();
        if (isOaTenantAdmin(user)) {
            return;
        }
        Long tenantId = TenantContextHolder.getTenantId();
        Set<Long> userIds = ipGroupAccessSupport.resolveMembershipUserIds(tenantId);
        applyIdIn(wrapper, assigneeColumn, userIds.isEmpty() ? Set.of(NO_ACCESS_SENTINEL) : userIds);
    }

    /** 非 admin：校验实体创建者为当前用户 */
    public void assertSelfCreator(String creatorUsername) {
        LoginUser user = LoginUserContext.get();
        if (isOaTenantAdmin(user)) {
            return;
        }
        String username = user != null ? user.getUsername() : null;
        if (username == null || !username.equals(creatorUsername)) {
            throw new ServiceException(OaErrorCodes.FORBIDDEN);
        }
    }

    /** 非 admin：creator=本人 或 ownerUserId 在 membership userIds 内 */
    public <T> void applySelfCreatorOrUserId(LambdaQueryWrapper<T> wrapper,
                                              SFunction<T, String> creatorColumn,
                                              SFunction<T, Long> ownerUserIdColumn) {
        LoginUser user = LoginUserContext.get();
        if (isOaTenantAdmin(user)) {
            return;
        }
        String username = user != null ? user.getUsername() : null;
        Long tenantId = TenantContextHolder.getTenantId();
        Set<Long> userIds = ipGroupAccessSupport.resolveMembershipUserIds(tenantId);
        wrapper.and(w -> {
            boolean hasCreator = username != null && !username.isBlank();
            if (hasCreator) {
                w.eq(creatorColumn, username);
            }
            if (!userIds.isEmpty()) {
                if (hasCreator) {
                    w.or().in(ownerUserIdColumn, userIds);
                } else {
                    w.in(ownerUserIdColumn, userIds);
                }
            } else if (!hasCreator) {
                w.eq(ownerUserIdColumn, NO_ACCESS_SENTINEL);
            }
        });
    }

    /** 非 admin：creator=本人 或 ownerUserId 在 membership userIds 内 */
    public void assertSelfCreatorOrUserId(String creatorUsername, Long ownerUserId) {
        LoginUser user = LoginUserContext.get();
        if (isOaTenantAdmin(user)) {
            return;
        }
        String username = user != null ? user.getUsername() : null;
        if (username != null && username.equals(creatorUsername)) {
            return;
        }
        Long tenantId = TenantContextHolder.getTenantId();
        Set<Long> userIds = ipGroupAccessSupport.resolveMembershipUserIds(tenantId);
        if (ownerUserId != null && userIds.contains(ownerUserId)) {
            return;
        }
        throw new ServiceException(OaErrorCodes.FORBIDDEN);
    }

    /**
     * 6156 人效盘点：admin 全量；IP 组长看管辖组 member；others 仅自己（membership userIds）。
     */
    public <T> void applyProductivityUserScope(LambdaQueryWrapper<T> wrapper, SFunction<T, Long> userIdColumn) {
        LoginUser user = LoginUserContext.get();
        if (isOaTenantAdmin(user)) {
            return;
        }
        Long tenantId = TenantContextHolder.getTenantId();
        Set<Long> allowedUserIds = isIpGroupLeader(user)
                ? resolveLedGroupMemberUserIds(user, tenantId)
                : ipGroupAccessSupport.resolveMembershipUserIds(tenantId);
        applyIdIn(wrapper, userIdColumn,
                allowedUserIds.isEmpty() ? Set.of(NO_ACCESS_SENTINEL) : allowedUserIds);
    }

    /** 6156 detail/export：目标 userId 须在可见范围内 */
    public void assertProductivityUserReadable(Long targetUserId) {
        LoginUser user = LoginUserContext.get();
        if (isOaTenantAdmin(user) || targetUserId == null) {
            return;
        }
        Long tenantId = TenantContextHolder.getTenantId();
        Set<Long> allowedUserIds = isIpGroupLeader(user)
                ? resolveLedGroupMemberUserIds(user, tenantId)
                : ipGroupAccessSupport.resolveMembershipUserIds(tenantId);
        if (allowedUserIds.isEmpty() || !allowedUserIds.contains(targetUserId)) {
            throw new ServiceException(OaErrorCodes.FORBIDDEN);
        }
    }

    /** 6159 list/tree：非 admin 非 IP 组长 → 403 */
    public void assertIpGroupManagementListAccess() {
        LoginUser user = LoginUserContext.get();
        if (isOaTenantAdmin(user) || isIpGroupLeader(user)) {
            return;
        }
        throw new ServiceException(OaErrorCodes.FORBIDDEN);
    }

    /**
     * 6159 读：admin 全量；组长可管辖组（ledIpGroupIds）及其祖先链（与 getTree 可见范围对齐）；others 403。
     * 祖先可读避免组长点击树上级大组时 members/stats 连续 403。
     */
    public void assertIpGroupLedReadable(Long ipGroupId) {
        LoginUser user = LoginUserContext.get();
        if (isOaTenantAdmin(user)) {
            return;
        }
        if (ipGroupId == null) {
            throw new ServiceException(OaErrorCodes.FORBIDDEN);
        }
        Long tenantId = TenantContextHolder.getTenantId();
        Set<Long> ledGroupIds = resolveScopeIpGroupIds(user, tenantId, AccountScopeMode.LED_GROUPS);
        if (ledGroupIds.isEmpty()) {
            throw new ServiceException(OaErrorCodes.FORBIDDEN);
        }
        if (ledGroupIds.contains(ipGroupId)) {
            return;
        }
        if (isAncestorOfAnyLedGroup(ipGroupId, ledGroupIds, tenantId)) {
            return;
        }
        throw new ServiceException(OaErrorCodes.FORBIDDEN);
    }

    /** 6159 写：admin 全量；组长仅 ledIpGroupIds（不含祖先大组）；others 403 */
    public void assertIpGroupLedWritable(Long ipGroupId) {
        LoginUser user = LoginUserContext.get();
        if (isOaTenantAdmin(user)) {
            return;
        }
        if (ipGroupId == null) {
            throw new ServiceException(OaErrorCodes.FORBIDDEN);
        }
        Long tenantId = TenantContextHolder.getTenantId();
        Set<Long> ledGroupIds = resolveScopeIpGroupIds(user, tenantId, AccountScopeMode.LED_GROUPS);
        if (ledGroupIds.isEmpty() || !ledGroupIds.contains(ipGroupId)) {
            throw new ServiceException(OaErrorCodes.FORBIDDEN);
        }
    }

    /** 候选节点是否为任一管辖小组的祖先（用于树展示上级大组的只读详情） */
    private boolean isAncestorOfAnyLedGroup(Long candidateId, Set<Long> ledGroupIds, Long tenantId) {
        if (candidateId == null || ledGroupIds == null || ledGroupIds.isEmpty() || tenantId == null) {
            return false;
        }
        for (Long ledId : ledGroupIds) {
            Long cursor = ledId;
            for (int depth = 0; depth < 100 && cursor != null; depth++) {
                IpGroupDO node = ipGroupMapper.selectById(cursor);
                if (node == null || !Objects.equals(node.getTenantId(), tenantId)) {
                    break;
                }
                Long parentId = node.getParentId();
                if (parentId == null) {
                    break;
                }
                if (Objects.equals(parentId, candidateId)) {
                    return true;
                }
                cursor = parentId;
            }
        }
        return false;
    }

    /** 6159 getTree/listPage：admin=null（全量）；组长=ledIpGroupIds；others 由 assert 拦截 */
    public Set<Long> resolveIpGroupManagementScopeIds() {
        LoginUser user = LoginUserContext.get();
        if (isOaTenantAdmin(user)) {
            return null;
        }
        Long tenantId = TenantContextHolder.getTenantId();
        Set<Long> ledGroupIds = resolveScopeIpGroupIds(user, tenantId, AccountScopeMode.LED_GROUPS);
        return ledGroupIds.isEmpty() ? Set.of(NO_ACCESS_SENTINEL) : ledGroupIds;
    }

    /** 组长管辖组下全部 member.user_id（含 ID 桥接扩展） */
    public Set<Long> resolveLedGroupMemberUserIds(LoginUser user, Long tenantId) {
        if (user == null || tenantId == null) {
            return Set.of();
        }
        Set<Long> ledGroupIds = resolveScopeIpGroupIds(user, tenantId, AccountScopeMode.LED_GROUPS);
        if (ledGroupIds.isEmpty()) {
            return Set.of();
        }
        Set<Long> memberUserIds = new LinkedHashSet<>();
        ipGroupMemberMapper.selectList(new LambdaQueryWrapper<IpGroupMemberDO>()
                        .eq(IpGroupMemberDO::getTenantId, tenantId)
                        .in(IpGroupMemberDO::getIpGroupId, ledGroupIds))
                .forEach(member -> {
                    if (member.getUserId() != null) {
                        memberUserIds.add(member.getUserId());
                    }
                });
        Set<Long> expanded = new LinkedHashSet<>();
        for (Long memberUserId : memberUserIds) {
            expanded.addAll(ipGroupAccessSupport.resolveEquivalentUserIds(memberUserId, tenantId));
        }
        return expanded.isEmpty() ? memberUserIds : expanded;
    }

    /** 请求参数 ipGroupId 与可访问组求交；admin 可任意指定 */
    public Set<Long> narrowIpGroupIds(Long requestedIpGroupId) {
        LoginUser user = LoginUserContext.get();
        if (isOaTenantAdmin(user)) {
            return requestedIpGroupId != null ? Set.of(requestedIpGroupId) : null;
        }
        Long tenantId = TenantContextHolder.getTenantId();
        Set<Long> accessible = resolveScopeIpGroupIds(user, tenantId, AccountScopeMode.MEMBER_GROUPS);
        if (accessible.isEmpty()) {
            return Set.of(NO_ACCESS_SENTINEL);
        }
        if (requestedIpGroupId == null) {
            return accessible;
        }
        return accessible.contains(requestedIpGroupId) ? Set.of(requestedIpGroupId) : Set.of(NO_ACCESS_SENTINEL);
    }

    private Set<Long> resolveScopeIpGroupIds(LoginUser user, Long tenantId, AccountScopeMode mode) {
        if (user == null || tenantId == null) {
            return Set.of();
        }
        return switch (mode) {
            case MEMBER_GROUPS -> {
                // 产品：其他角色看「所在 IP 组」数据 = 成员组 ∪ 组长组（组长可能仅写在 leader_user_id）
                Set<Long> accessible = new LinkedHashSet<>();
                if (user.getMemberIpGroupIds() != null && !user.getMemberIpGroupIds().isEmpty()) {
                    accessible.addAll(user.getMemberIpGroupIds());
                } else {
                    accessible.addAll(resolveMemberIpGroupIds(user.getUserId(), user.getUsername(), tenantId));
                }
                if (user.getLedIpGroupIds() != null && !user.getLedIpGroupIds().isEmpty()) {
                    accessible.addAll(user.getLedIpGroupIds());
                } else {
                    accessible.addAll(resolveLedIpGroupIds(user.getUserId(), user.getUsername(), tenantId));
                }
                yield accessible;
            }
            case LED_GROUPS -> {
                if (user.getLedIpGroupIds() != null && !user.getLedIpGroupIds().isEmpty()) {
                    yield new LinkedHashSet<>(user.getLedIpGroupIds());
                }
                yield resolveLedIpGroupIds(user.getUserId(), user.getUsername(), tenantId);
            }
            default -> Set.of();
        };
    }

    private <T> void applyIdIn(LambdaQueryWrapper<T> wrapper, SFunction<T, Long> column, Set<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            wrapper.eq(column, NO_ACCESS_SENTINEL);
            return;
        }
        if (ids.size() == 1 && ids.contains(NO_ACCESS_SENTINEL)) {
            wrapper.eq(column, NO_ACCESS_SENTINEL);
            return;
        }
        wrapper.in(column, ids);
    }
}
