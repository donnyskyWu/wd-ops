package cn.iocoder.yudao.module.oa.service.user;

import cn.iocoder.yudao.framework.common.exception.OaErrorCodes;
import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.oa.api.dto.user.UserIpGroupVO;
import cn.iocoder.yudao.module.oa.dal.dataobject.author.AuthorDO;
import cn.iocoder.yudao.module.oa.dal.dataobject.ipgroup.IpGroupDO;
import cn.iocoder.yudao.module.oa.dal.dataobject.ipgroup.IpGroupMemberDO;
import cn.iocoder.yudao.module.oa.dal.mysql.author.AuthorMapper;
import cn.iocoder.yudao.module.oa.dal.mysql.ipgroup.IpGroupMapper;
import cn.iocoder.yudao.module.oa.dal.mysql.ipgroup.IpGroupMemberMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class UserContentContextServiceImpl implements UserContentContextService {

    private final IpGroupMemberMapper ipGroupMemberMapper;
    private final IpGroupMapper ipGroupMapper;
    private final AuthorMapper authorMapper;

    @Override
    public List<UserIpGroupVO> listMyIpGroups() {
        Long userId = TenantContextHolder.getUserId();
        if (userId == null) {
            throw new ServiceException(OaErrorCodes.UNAUTHORIZED);
        }
        Long tenantId = requireTenantId();
        List<IpGroupMemberDO> memberships = ipGroupMemberMapper.selectList(
                new LambdaQueryWrapper<IpGroupMemberDO>()
                        .eq(IpGroupMemberDO::getTenantId, tenantId)
                        .eq(IpGroupMemberDO::getUserId, userId)
                        .orderByAsc(IpGroupMemberDO::getId));
        List<UserIpGroupVO> result = new ArrayList<>();
        for (IpGroupMemberDO member : memberships) {
            IpGroupDO group = ipGroupMapper.selectById(member.getIpGroupId());
            if (group == null || !Objects.equals(group.getTenantId(), tenantId) || group.getStatus() != 1) {
                continue;
            }
            UserIpGroupVO vo = new UserIpGroupVO();
            vo.setIpGroupId(group.getId());
            vo.setIpGroupName(group.getGroupName());
            vo.setGroupType(group.getGroupType() != null && group.getGroupType() == 2 ? "SMALL" : "BIG");
            AuthorDO author = authorMapper.selectOne(
                    new LambdaQueryWrapper<AuthorDO>()
                            .eq(AuthorDO::getTenantId, tenantId)
                            .eq(AuthorDO::getIpGroupId, group.getId())
                            .eq(AuthorDO::getStatus, 1)
                            .orderByAsc(AuthorDO::getId)
                            .last("LIMIT 1"));
            if (author != null) {
                vo.setAuthorId(author.getId());
                vo.setAuthorName(author.getAuthorName());
            }
            result.add(vo);
        }
        return result;
    }

    private Long requireTenantId() {
        Long tenantId = TenantContextHolder.getTenantId();
        if (tenantId == null) {
            throw new ServiceException(OaErrorCodes.UNAUTHORIZED.getCode(), "缺少租户上下文");
        }
        return tenantId;
    }
}
