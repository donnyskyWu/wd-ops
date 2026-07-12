package cn.iocoder.yudao.module.oa.service.user;

import cn.iocoder.yudao.framework.common.exception.OaErrorCodes;
import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.oa.api.dto.user.UserIpGroupVO;
import cn.iocoder.yudao.module.oa.dal.dataobject.ipgroup.IpGroupDO;
import cn.iocoder.yudao.module.oa.dal.dataobject.ipgroup.IpGroupMemberDO;
import cn.iocoder.yudao.module.oa.dal.mysql.ipgroup.IpGroupMapper;
import cn.iocoder.yudao.module.oa.service.author.AuthorResolveSupport;
import cn.iocoder.yudao.module.oa.service.ipgroup.IpGroupAccessSupport;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class UserContentContextServiceImpl implements UserContentContextService {

    private final IpGroupAccessSupport ipGroupAccessSupport;
    private final IpGroupMapper ipGroupMapper;
    private final AuthorResolveSupport authorResolveSupport;

    @Override
    public List<UserIpGroupVO> listMyIpGroups() {
        if (TenantContextHolder.getUserId() == null) {
            throw new ServiceException(OaErrorCodes.UNAUTHORIZED);
        }
        Long tenantId = requireTenantId();
        Map<Long, IpGroupDO> groups = new LinkedHashMap<>();
        if (ipGroupAccessSupport.hasUnrestrictedIpGroupAccess()) {
            ipGroupMapper.selectList(new LambdaQueryWrapper<IpGroupDO>()
                            .eq(IpGroupDO::getTenantId, tenantId)
                            .eq(IpGroupDO::getStatus, 1)
                            .orderByAsc(IpGroupDO::getSortOrder)
                            .orderByAsc(IpGroupDO::getId))
                    .forEach(group -> groups.put(group.getId(), group));
        } else {
            for (IpGroupMemberDO member : ipGroupAccessSupport.listMemberships(tenantId)) {
                IpGroupDO group = ipGroupMapper.selectById(member.getIpGroupId());
                if (group == null || !Objects.equals(group.getTenantId(), tenantId) || group.getStatus() != 1) {
                    continue;
                }
                groups.putIfAbsent(group.getId(), group);
            }
        }
        List<UserIpGroupVO> result = new ArrayList<>();
        for (IpGroupDO group : groups.values()) {
            result.add(toUserIpGroupVO(group, tenantId));
        }
        return result;
    }

    private UserIpGroupVO toUserIpGroupVO(IpGroupDO group, Long tenantId) {
        UserIpGroupVO vo = new UserIpGroupVO();
        vo.setIpGroupId(group.getId());
        vo.setIpGroupName(group.getGroupName());
        vo.setGroupType(group.getGroupType() != null && group.getGroupType() == 2 ? "SMALL" : "BIG");
        Long authorUserId = authorResolveSupport.findFirstAuthorUserId(group.getId(), tenantId);
        if (authorUserId != null) {
            vo.setAuthorId(authorUserId);
            vo.setAuthorName(authorResolveSupport.resolveNickname(authorUserId));
        }
        return vo;
    }

    private Long requireTenantId() {
        Long tenantId = TenantContextHolder.getTenantId();
        if (tenantId == null) {
            throw new ServiceException(OaErrorCodes.UNAUTHORIZED.getCode(), "缺少租户上下文");
        }
        return tenantId;
    }
}
