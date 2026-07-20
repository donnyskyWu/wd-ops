package cn.iocoder.yudao.module.oa.service.ipgroup;

import cn.iocoder.yudao.framework.common.exception.OaErrorCodes;
import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.module.oa.dal.dataobject.auth.FootballSystemUserDO;
import cn.iocoder.yudao.module.oa.dal.dataobject.auth.SysUserDO;
import cn.iocoder.yudao.module.oa.dal.dataobject.ipgroup.IpGroupDO;
import cn.iocoder.yudao.module.oa.dal.dataobject.ipgroup.IpGroupMemberDO;
import cn.iocoder.yudao.module.oa.dal.mysql.auth.FootballOAuth2MasterTokenMapper;
import cn.iocoder.yudao.module.oa.dal.mysql.auth.FootballOAuth2TokenMapper;
import cn.iocoder.yudao.module.oa.dal.mysql.auth.SysUserTokenMapper;
import cn.iocoder.yudao.module.oa.dal.mysql.ipgroup.IpGroupMapper;
import cn.iocoder.yudao.module.oa.dal.mysql.ipgroup.IpGroupMemberMapper;
import cn.iocoder.yudao.module.oa.service.support.FootballSystemUserValidator;
import cn.iocoder.yudao.module.oa.framework.auth.DataScopeSupport;
import cn.iocoder.yudao.module.oa.framework.auth.LoginUser;
import cn.iocoder.yudao.module.oa.framework.auth.LoginUserContext;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Resolves IP-group membership across Football {@code system_users.id} and legacy {@code sys_user.id}.
 */
@Component
@RequiredArgsConstructor
public class IpGroupAccessSupport {

    private final IpGroupMapper ipGroupMapper;
    private final IpGroupMemberMapper ipGroupMemberMapper;
    private final SysUserTokenMapper sysUserTokenMapper;
    private final FootballOAuth2MasterTokenMapper footballOAuth2MasterTokenMapper;
    private final FootballOAuth2TokenMapper footballOAuth2TokenMapper;
    private final FootballSystemUserValidator footballSystemUserValidator;

    public boolean hasUnrestrictedIpGroupAccess() {
        return isSystemAdmin(LoginUserContext.get());
    }

    private boolean isSystemAdmin(LoginUser user) {
        if (user == null) {
            return false;
        }
        if (DataScopeSupport.ALL.equals(user.getDataScope())) {
            return true;
        }
        Set<String> authorities = user.getAuthorities();
        if (authorities == null || authorities.isEmpty()) {
            return false;
        }
        return authorities.contains("ROLE_OA_ADMIN") || authorities.contains("OA_ADMIN");
    }

    public Set<Long> resolveMembershipUserIds(Long tenantId) {
        LoginUser user = LoginUserContext.getRequired();
        return resolveMembershipUserIds(user.getUserId(), user.getUsername(), tenantId);
    }

    /** 登录前/后均可：Football + sys_user + wd 三轨 userId 并集 */
    public Set<Long> resolveMembershipUserIds(Long userId, String username, Long tenantId) {
        Set<Long> userIds = new LinkedHashSet<>();
        if (userId != null) {
            userIds.add(userId);
        }
        if (username != null && !username.isBlank() && tenantId != null) {
            SysUserDO oaUser = sysUserTokenMapper.selectUserByUsernameAndTenant(username, tenantId);
            if (oaUser != null) {
                userIds.add(oaUser.getId());
            }
            addFootballUserIds(username, userIds);
        }
        return userIds;
    }

    /**
     * Union legacy {@code sys_user.id}, wd master and shenyu-system ids for a stored/submitted user id.
     */
    public Set<Long> resolveEquivalentUserIds(Long userId, Long tenantId) {
        Set<Long> userIds = new LinkedHashSet<>();
        if (userId == null) {
            return userIds;
        }
        userIds.add(userId);

        SysUserDO legacyUser = footballSystemUserValidator.findLegacyUser(userId);
        if (legacyUser != null) {
            userIds.add(legacyUser.getId());
            addFootballUserIds(legacyUser.getUsername(), userIds);
            if (tenantId != null && legacyUser.getUsername() != null && !legacyUser.getUsername().isBlank()) {
                SysUserDO tenantUser = sysUserTokenMapper.selectUserByUsernameAndTenant(legacyUser.getUsername(), tenantId);
                if (tenantUser != null) {
                    userIds.add(tenantUser.getId());
                }
            }
        }

        FootballSystemUserDO footballUser = footballSystemUserValidator.findFootballUser(userId);
        if (footballUser != null && footballUser.getUsername() != null && !footballUser.getUsername().isBlank()) {
            addFootballUserIds(footballUser.getUsername(), userIds);
            if (tenantId != null) {
                SysUserDO oaUser = sysUserTokenMapper.selectUserByUsernameAndTenant(footballUser.getUsername(), tenantId);
                if (oaUser != null) {
                    userIds.add(oaUser.getId());
                }
            }
        }
        return userIds;
    }

    /**
     * Union wd master + shenyu-system user ids so IP-group leader/member rows match Football login.
     */
    private void addFootballUserIds(String username, Set<Long> userIds) {
        if (username == null || username.isBlank()) {
            return;
        }
        try {
            FootballSystemUserDO masterUser = footballOAuth2MasterTokenMapper.selectUserByUsername(username);
            if (masterUser != null && masterUser.getId() != null) {
                userIds.add(masterUser.getId());
            }
        } catch (Exception ignored) {
            // H2 integration tests have no Football overlay tables.
        }
        try {
            FootballSystemUserDO systemUser = footballOAuth2TokenMapper.selectUserByUsername(username);
            if (systemUser != null && systemUser.getId() != null) {
                userIds.add(systemUser.getId());
            }
        } catch (Exception ignored) {
            // H2 integration tests have no Football overlay tables.
        }
    }

    public List<IpGroupMemberDO> listMemberships(Long tenantId) {
        Set<Long> userIds = resolveMembershipUserIds(tenantId);
        if (userIds.isEmpty()) {
            return List.of();
        }
        return ipGroupMemberMapper.selectList(new LambdaQueryWrapper<IpGroupMemberDO>()
                .eq(IpGroupMemberDO::getTenantId, tenantId)
                .in(IpGroupMemberDO::getUserId, userIds)
                .orderByAsc(IpGroupMemberDO::getId));
    }

    public boolean isMemberOfIpGroup(Long ipGroupId, Long tenantId) {
        if (hasUnrestrictedIpGroupAccess()) {
            return true;
        }
        Set<Long> userIds = resolveMembershipUserIds(tenantId);
        if (userIds.isEmpty()) {
            return false;
        }
        Long count = ipGroupMemberMapper.selectCount(new LambdaQueryWrapper<IpGroupMemberDO>()
                .eq(IpGroupMemberDO::getTenantId, tenantId)
                .eq(IpGroupMemberDO::getIpGroupId, ipGroupId)
                .in(IpGroupMemberDO::getUserId, userIds));
        return count != null && count > 0;
    }

    /** oa_ip_group_member 中当前用户（含 ID 桥接）所属的小组 ID */
    public Set<Long> resolveMemberIpGroupIds(Long tenantId) {
        LoginUser user = LoginUserContext.getRequired();
        if (user.getMemberIpGroupIds() != null && !user.getMemberIpGroupIds().isEmpty()) {
            return user.getMemberIpGroupIds();
        }
        return queryMemberIpGroupIds(user.getUserId(), user.getUsername(), tenantId);
    }

    /** 当前用户担任组长的业务 IP 组（leader_user_id 或成员 is_leader） */
    public Set<Long> resolveLedIpGroupIds(Long tenantId) {
        LoginUser user = LoginUserContext.getRequired();
        if (user.getLedIpGroupIds() != null && !user.getLedIpGroupIds().isEmpty()) {
            return user.getLedIpGroupIds();
        }
        return queryLedIpGroupIds(user.getUserId(), user.getUsername(), tenantId);
    }

    /** 成员组 ∪ 组长组；系统管理员返回 null 表示无范围限制 */
    public Set<Long> resolveAccessibleIpGroupIds(Long tenantId) {
        if (hasUnrestrictedIpGroupAccess()) {
            return null;
        }
        LoginUser user = LoginUserContext.getRequired();
        Set<Long> accessible = new LinkedHashSet<>(queryMemberIpGroupIds(user.getUserId(), user.getUsername(), tenantId));
        accessible.addAll(queryLedIpGroupIds(user.getUserId(), user.getUsername(), tenantId));
        return accessible;
    }

    private Set<Long> queryMemberIpGroupIds(Long userId, String username, Long tenantId) {
        Set<Long> userIds = resolveMembershipUserIds(userId, username, tenantId);
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

    private Set<Long> queryLedIpGroupIds(Long userId, String username, Long tenantId) {
        Set<Long> userIds = resolveMembershipUserIds(userId, username, tenantId);
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

    public void assertIpGroupAccessible(Long ipGroupId, Long tenantId) {
        if (ipGroupId == null) {
            throw new ServiceException(OaErrorCodes.BAD_REQUEST.getCode(), "所属 IP 组不能为空");
        }
        IpGroupDO group = ipGroupMapper.selectById(ipGroupId);
        if (group == null || !Objects.equals(group.getTenantId(), tenantId)) {
            throw new ServiceException(OaErrorCodes.ENTITY_NOT_EXISTS);
        }
        if (group.getStatus() == null || group.getStatus() != 1) {
            throw new ServiceException(OaErrorCodes.ENTITY_DISABLED);
        }
        if (group.getGroupType() == null || group.getGroupType() != 2) {
            throw new ServiceException(OaErrorCodes.IP_GROUP_ACCOUNT_SMALL_ONLY);
        }
        Set<Long> accessible = resolveAccessibleIpGroupIds(tenantId);
        if (accessible != null && !accessible.contains(ipGroupId)) {
            throw new ServiceException(OaErrorCodes.TENANT_FORBIDDEN.getCode(), "无权绑定该 IP 组");
        }
    }
}
