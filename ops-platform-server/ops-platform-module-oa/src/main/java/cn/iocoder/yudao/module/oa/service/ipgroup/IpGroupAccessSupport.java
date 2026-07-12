package cn.iocoder.yudao.module.oa.service.ipgroup;

import cn.iocoder.yudao.module.oa.dal.dataobject.auth.SysUserDO;
import cn.iocoder.yudao.module.oa.dal.dataobject.ipgroup.IpGroupMemberDO;
import cn.iocoder.yudao.module.oa.dal.mysql.auth.SysUserTokenMapper;
import cn.iocoder.yudao.module.oa.dal.mysql.ipgroup.IpGroupMemberMapper;
import cn.iocoder.yudao.module.oa.framework.auth.DataScopeSupport;
import cn.iocoder.yudao.module.oa.framework.auth.LoginUser;
import cn.iocoder.yudao.module.oa.framework.auth.LoginUserContext;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Resolves IP-group membership across Football {@code system_users.id} and legacy {@code sys_user.id}.
 */
@Component
@RequiredArgsConstructor
public class IpGroupAccessSupport {

    private final IpGroupMemberMapper ipGroupMemberMapper;
    private final SysUserTokenMapper sysUserTokenMapper;

    public boolean hasUnrestrictedIpGroupAccess() {
        LoginUser user = LoginUserContext.get();
        return user != null && DataScopeSupport.ALL.equals(user.getDataScope());
    }

    public Set<Long> resolveMembershipUserIds(Long tenantId) {
        LoginUser user = LoginUserContext.getRequired();
        Set<Long> userIds = new LinkedHashSet<>();
        if (user.getUserId() != null) {
            userIds.add(user.getUserId());
        }
        SysUserDO oaUser = sysUserTokenMapper.selectUserByUsernameAndTenant(user.getUsername(), tenantId);
        if (oaUser != null) {
            userIds.add(oaUser.getId());
        }
        return userIds;
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
}
