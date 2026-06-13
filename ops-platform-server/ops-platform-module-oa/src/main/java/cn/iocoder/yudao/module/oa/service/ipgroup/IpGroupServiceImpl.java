package cn.iocoder.yudao.module.oa.service.ipgroup;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.exception.OaErrorCodes;
import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.oa.api.dto.ipgroup.IpGroupAccountBindReq;
import cn.iocoder.yudao.module.oa.api.dto.ipgroup.IpGroupAccountVO;
import cn.iocoder.yudao.module.oa.api.dto.ipgroup.IpGroupAnchorBindReq;
import cn.iocoder.yudao.module.oa.api.dto.ipgroup.IpGroupAnchorVO;
import cn.iocoder.yudao.module.oa.api.dto.ipgroup.IpGroupCreateReq;
import cn.iocoder.yudao.module.oa.api.dto.ipgroup.IpGroupDetailVO;
import cn.iocoder.yudao.module.oa.api.dto.ipgroup.IpGroupListVO;
import cn.iocoder.yudao.module.oa.api.dto.ipgroup.IpGroupMemberCreateReq;
import cn.iocoder.yudao.module.oa.api.dto.ipgroup.IpGroupMemberUpdateReq;
import cn.iocoder.yudao.module.oa.api.dto.ipgroup.IpGroupMemberVO;
import cn.iocoder.yudao.module.oa.api.dto.ipgroup.IpGroupStatsVO;
import cn.iocoder.yudao.module.oa.api.dto.ipgroup.IpGroupTreeVO;
import cn.iocoder.yudao.module.oa.api.dto.ipgroup.IpGroupUpdateReq;
import cn.iocoder.yudao.module.oa.dal.dataobject.account.AccountDO;
import cn.iocoder.yudao.module.oa.dal.dataobject.auth.SysUserDO;
import cn.iocoder.yudao.module.oa.dal.dataobject.ipgroup.IpGroupAnchorRelDO;
import cn.iocoder.yudao.module.oa.dal.dataobject.ipgroup.IpGroupDO;
import cn.iocoder.yudao.module.oa.dal.dataobject.ipgroup.IpGroupMemberDO;
import cn.iocoder.yudao.module.oa.dal.mysql.account.AccountMapper;
import cn.iocoder.yudao.module.oa.dal.mysql.auth.SysUserMapper;
import cn.iocoder.yudao.module.oa.dal.mysql.ipgroup.IpGroupAnchorRelMapper;
import cn.iocoder.yudao.module.oa.dal.mysql.ipgroup.IpGroupMapper;
import cn.iocoder.yudao.module.oa.dal.mysql.ipgroup.IpGroupMemberMapper;
import cn.iocoder.yudao.module.oa.dal.dataobject.dict.SysDictDataDO;
import cn.iocoder.yudao.module.oa.dal.mysql.dict.SysDictDataMapper;
import cn.iocoder.yudao.module.oa.framework.auth.DataScopeSupport;
import cn.iocoder.yudao.module.oa.framework.audit.AuditLog;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class IpGroupServiceImpl implements IpGroupService {

    private final IpGroupMapper ipGroupMapper;
    private final IpGroupMemberMapper ipGroupMemberMapper;
    private final SysDictDataMapper sysDictDataMapper;
    private final IpGroupAnchorRelMapper ipGroupAnchorRelMapper;
    private final AccountMapper accountMapper;
    private final SysUserMapper sysUserMapper;

    @Override
    public List<IpGroupTreeVO> getTree() {
        Long tenantId = requireTenantId();
        List<IpGroupDO> all = ipGroupMapper.selectList(new LambdaQueryWrapper<IpGroupDO>()
                .eq(IpGroupDO::getTenantId, tenantId)
                .orderByAsc(IpGroupDO::getSortOrder)
                .orderByAsc(IpGroupDO::getId));
        if (all.isEmpty()) {
            return Collections.emptyList();
        }

        Map<Long, String> nameMap = all.stream()
                .collect(Collectors.toMap(IpGroupDO::getId, IpGroupDO::getGroupName, (a, b) -> a));
        Map<Long, String> leaderNameMap = loadLeaderNames(all);
        Map<Long, Integer> memberCounts = countMembersByGroup(tenantId);
        Map<Long, Integer> accountCounts = countAccounts(tenantId);
        Map<Long, Integer> anchorCounts = countAnchorsByGroup(tenantId);

        Map<Long, List<IpGroupDO>> childrenMap = all.stream()
                .filter(g -> g.getParentId() != null)
                .collect(Collectors.groupingBy(IpGroupDO::getParentId));

        return all.stream()
                .filter(g -> g.getParentId() == null)
                .map(root -> toTreeNode(root, nameMap, leaderNameMap, memberCounts, accountCounts, anchorCounts, childrenMap))
                .collect(Collectors.toList());
    }

    @Override
    public PageResult<IpGroupListVO> listPage(
            String groupName, Integer groupType, Integer status,
            Integer pageNum, Integer pageSize) {
        Long tenantId = requireTenantId();
        LambdaQueryWrapper<IpGroupDO> wrapper = new LambdaQueryWrapper<IpGroupDO>()
                .eq(IpGroupDO::getTenantId, tenantId)
                .like(StrUtil.isNotBlank(groupName), IpGroupDO::getGroupName, groupName)
                .eq(groupType != null, IpGroupDO::getGroupType, groupType)
                .eq(status != null, IpGroupDO::getStatus, status)
                .orderByAsc(IpGroupDO::getSortOrder)
                .orderByDesc(IpGroupDO::getId);
        com.baomidou.mybatisplus.extension.plugins.pagination.Page<IpGroupDO> page = ipGroupMapper.selectPage(
                new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>(
                        pageNum == null ? 1 : pageNum, pageSize == null ? 20 : pageSize),
                wrapper);
        List<IpGroupDO> records = page.getRecords();

        // 批量取 parentName + leaderName
        Set<Long> parentIds = records.stream().map(IpGroupDO::getParentId)
                .filter(Objects::nonNull).collect(Collectors.toSet());
        Map<Long, String> parentNameMap = parentIds.isEmpty() ? Collections.emptyMap() :
                ipGroupMapper.selectBatchIds(parentIds).stream()
                        .filter(p -> tenantId.equals(p.getTenantId()))
                        .collect(Collectors.toMap(IpGroupDO::getId, IpGroupDO::getGroupName, (a, b) -> a));
        Map<Long, String> leaderNameMap = loadLeaderNames(records);

        List<IpGroupListVO> list = records.stream().map(d -> {
            IpGroupListVO vo = new IpGroupListVO();
            vo.setId(d.getId());
            vo.setGroupName(d.getGroupName());
            vo.setGroupType(d.getGroupType());
            vo.setParentId(d.getParentId());
            vo.setParentName(d.getParentId() == null ? null : parentNameMap.get(d.getParentId()));
            vo.setLeaderName(d.getLeaderUserId() == null ? null : leaderNameMap.get(d.getLeaderUserId()));
            vo.setStatus(d.getStatus());
            vo.setCreateTime(d.getCreateTime());
            vo.setMemberCount(0);
            vo.setAccountCount(0);
            vo.setAnchorCount(0);
            return vo;
        }).collect(Collectors.toList());
        return new PageResult<>(list, page.getTotal());
    }

    @Override
    public IpGroupDetailVO getDetail(Long id) {
        IpGroupDO entity = requireGroup(id);
        Map<Long, String> leaderNameMap = loadLeaderNames(List.of(entity));
        String parentName = null;
        if (entity.getParentId() != null) {
            IpGroupDO parent = ipGroupMapper.selectById(entity.getParentId());
            if (parent != null && Objects.equals(parent.getTenantId(), entity.getTenantId())) {
                parentName = parent.getGroupName();
            }
        }

        IpGroupDetailVO vo = new IpGroupDetailVO();
        vo.setId(entity.getId());
        vo.setGroupName(entity.getGroupName());
        vo.setGroupType(entity.getGroupType());
        vo.setParentId(entity.getParentId());
        vo.setParentName(parentName);
        vo.setLeaderId(entity.getLeaderUserId());
        vo.setLeaderName(entity.getLeaderUserId() == null ? null : leaderNameMap.get(entity.getLeaderUserId()));
        vo.setSortOrder(entity.getSortOrder());
        vo.setStatus(entity.getStatus());
        vo.setRemark(entity.getRemark());
        vo.setCreateTime(entity.getCreateTime());
        vo.setUpdateTime(entity.getUpdateTime());

        Set<Long> scopeIds = collectScopeIds(entity);
        vo.setMemberCount(countMembers(scopeIds));
        vo.setAccountCount(countAccounts(scopeIds));
        vo.setAnchorCount(countAnchors(scopeIds));
        return vo;
    }

    @Override
    public IpGroupStatsVO getStats(Long id) {
        IpGroupDO entity = requireGroup(id);
        Set<Long> scopeIds = collectScopeIds(entity);

        IpGroupStatsVO vo = new IpGroupStatsVO();
        vo.setIpGroupId(id);
        vo.setMemberCount(countMembers(scopeIds));
        vo.setAccountCount(countAccounts(scopeIds));
        vo.setAnchorCount(countAnchors(scopeIds));
        vo.setTotalFollowers(0L);
        vo.setTotalContent(0);
        vo.setTotalLiveHours(0);
        vo.setRoi(0.0);
        return vo;
    }

    @Override
    public List<IpGroupAccountVO> listAccounts(Long id) {
        IpGroupDO entity = requireGroup(id);
        Set<Long> scopeIds = collectScopeIds(entity);
        if (scopeIds.isEmpty()) {
            return Collections.emptyList();
        }

        LambdaQueryWrapper<AccountDO> wrapper = new LambdaQueryWrapper<AccountDO>()
                .eq(AccountDO::getTenantId, entity.getTenantId())
                .in(AccountDO::getIpGroupId, scopeIds)
                .orderByDesc(AccountDO::getId);
        DataScopeSupport.applyIpGroupScope(wrapper, AccountDO::getIpGroupId);

        return accountMapper.selectList(wrapper).stream().map(acc -> {
            IpGroupAccountVO vo = new IpGroupAccountVO();
            vo.setAccountId(acc.getId());
            vo.setAccountName(acc.getAccountName());
            vo.setPlatform(acc.getPlatformType());
            vo.setPlatformText(acc.getPlatformType());
            vo.setFollowerCount(0L);
            vo.setContentCount(0);
            vo.setBoundAt(acc.getLinkedAt());
            return vo;
        }).collect(Collectors.toList());
    }

    @Override
    @Transactional
    @AuditLog(module = "M1-ip-group", action = "create")
    public Long create(IpGroupCreateReq req) {
        Long tenantId = requireTenantId();
        validateGroupName(req.getGroupName());
        assertNameUnique(tenantId, req.getParentId(), req.getGroupName(), null);
        validateGroupTypeAndParent(tenantId, req.getGroupType(), req.getParentId());

        Long leaderUserId = resolveLeaderUserId(req.getLeaderId(), req.getLeaderUserId());
        if (leaderUserId != null) {
            assertLeaderExists(tenantId, leaderUserId);
        }

        IpGroupDO entity = new IpGroupDO();
        entity.setTenantId(tenantId);
        entity.setGroupName(req.getGroupName().trim());
        entity.setGroupType(req.getGroupType());
        entity.setParentId(req.getGroupType() == 1 ? null : req.getParentId());
        entity.setLeaderUserId(leaderUserId);
        entity.setSortOrder(req.getSortOrder() == null ? 0 : req.getSortOrder());
        entity.setStatus(req.getStatus() == null ? 1 : req.getStatus());
        entity.setRemark(req.getRemark());
        entity.setCreator(TenantContextHolder.getUsername());
        entity.setUpdater(TenantContextHolder.getUsername());
        entity.setCreateTime(LocalDateTime.now());
        entity.setUpdateTime(LocalDateTime.now());
        ipGroupMapper.insert(entity);
        return entity.getId();
    }

    @Override
    @Transactional
    @AuditLog(module = "M1-ip-group", action = "update")
    public void update(IpGroupUpdateReq req) {
        IpGroupDO existing = requireGroup(req.getId());
        // P-GATE-UNMOCK S-E: parentId 修改支持（spec 漏字段，已补 IpGroupUpdateReq.parentId）
        // 仅小组可改 parentId；防自引用与子孙引用（防死循环）
        boolean parentIdChanged = false;
        if (req.getParentId() != null && !req.getParentId().equals(existing.getParentId())) {
            if (!Objects.equals(existing.getGroupType(), 2)) {
                throw new ServiceException(OaErrorCodes.IP_GROUP_PARENT_INVALID);
            }
            if (req.getParentId().equals(existing.getId())) {
                throw new ServiceException(OaErrorCodes.IP_GROUP_PARENT_INVALID);
            }
            validateGroupTypeAndParent(existing.getTenantId(), 2, req.getParentId());
            if (isDescendant(req.getParentId(), existing.getId())) {
                throw new ServiceException(OaErrorCodes.IP_GROUP_PARENT_INVALID);
            }
            // 父组变化后重检 name 唯一性（新 parentId 范围下不允许重名）
            if (StrUtil.isNotBlank(req.getGroupName())) {
                assertNameUnique(existing.getTenantId(), req.getParentId(), req.getGroupName(), existing.getId());
            }
            existing.setParentId(req.getParentId());
            parentIdChanged = true;
        }
        if (StrUtil.isNotBlank(req.getGroupName())) {
            validateGroupName(req.getGroupName());
            // parentId 未变时校验当前 parentId 下唯一性；parentId 刚改过则跳过（已在上面校验）
            if (!parentIdChanged) {
                assertNameUnique(existing.getTenantId(), existing.getParentId(), req.getGroupName(), existing.getId());
            }
            existing.setGroupName(req.getGroupName().trim());
        }
        Long leaderUserId = resolveLeaderUserId(req.getLeaderId(), req.getLeaderUserId());
        if (leaderUserId != null) {
            assertLeaderExists(existing.getTenantId(), leaderUserId);
            existing.setLeaderUserId(leaderUserId);
        }
        if (req.getSortOrder() != null) {
            existing.setSortOrder(req.getSortOrder());
        }
        if (req.getStatus() != null) {
            existing.setStatus(req.getStatus());
        }
        if (req.getRemark() != null) {
            existing.setRemark(req.getRemark());
        }
        existing.setUpdater(TenantContextHolder.getUsername());
        existing.setUpdateTime(LocalDateTime.now());
        ipGroupMapper.updateById(existing);
    }

    @Override
    @Transactional
    @AuditLog(module = "M1-ip-group", action = "update-status")
    public void updateStatus(Long id, Integer status) {
        IpGroupDO existing = requireGroup(id);
        existing.setStatus(status);
        existing.setUpdater(TenantContextHolder.getUsername());
        existing.setUpdateTime(LocalDateTime.now());
        ipGroupMapper.updateById(existing);
    }

    @Override
    @Transactional
    @AuditLog(module = "M1-ip-group", action = "delete")
    public void delete(Long id) {
        IpGroupDO entity = requireGroup(id);
        assertDeletable(entity);
        ipGroupMapper.deleteById(id);
    }

    @Override
    public List<IpGroupMemberVO> listMembers(Long id) {
        IpGroupDO entity = requireGroup(id);
        List<IpGroupMemberDO> members = ipGroupMemberMapper.selectList(new LambdaQueryWrapper<IpGroupMemberDO>()
                .eq(IpGroupMemberDO::getTenantId, entity.getTenantId())
                .eq(IpGroupMemberDO::getIpGroupId, id)
                .orderByDesc(IpGroupMemberDO::getId));
        if (members.isEmpty()) {
            return Collections.emptyList();
        }
        Set<Long> userIds = members.stream().map(IpGroupMemberDO::getUserId).collect(Collectors.toSet());
        Map<Long, SysUserDO> userMap = sysUserMapper.selectList(new LambdaQueryWrapper<SysUserDO>()
                        .in(SysUserDO::getId, userIds))
                .stream()
                .collect(Collectors.toMap(SysUserDO::getId, u -> u, (a, b) -> a));
        return members.stream().map(m -> toMemberVO(m, userMap.get(m.getUserId()))).collect(Collectors.toList());
    }

    @Override
    @Transactional
    @AuditLog(module = "M1-ip-group", action = "add-member")
    public void addMember(Long groupId, IpGroupMemberCreateReq req) {
        IpGroupDO entity = requireGroup(groupId);
        assertMemberGroupType(entity);
        SysUserDO user = requireUser(entity.getTenantId(), req.getUserId());
        assertMemberNotExists(entity.getTenantId(), groupId, req.getUserId());

        IpGroupMemberDO member = new IpGroupMemberDO();
        member.setTenantId(entity.getTenantId());
        member.setIpGroupId(groupId);
        member.setUserId(req.getUserId());
        member.setPosition(StrUtil.blankToDefault(req.getPosition(), user.getPosition()));
        member.setIsLeader(Boolean.TRUE.equals(req.getIsLeader()) ? 1 : 0);
        member.setCreator(TenantContextHolder.getUsername());
        member.setUpdater(TenantContextHolder.getUsername());
        member.setCreateTime(LocalDateTime.now());
        member.setUpdateTime(LocalDateTime.now());
        ipGroupMemberMapper.insert(member);
    }

    @Override
    @Transactional
    @AuditLog(module = "M1-ip-group", action = "update-member")
    public void updateMember(Long groupId, Long memberId, IpGroupMemberUpdateReq req) {
        IpGroupMemberDO member = requireMember(groupId, memberId);
        if (req.getPosition() != null) {
            member.setPosition(req.getPosition());
        }
        if (req.getIsLeader() != null) {
            member.setIsLeader(Boolean.TRUE.equals(req.getIsLeader()) ? 1 : 0);
        }
        member.setUpdater(TenantContextHolder.getUsername());
        member.setUpdateTime(LocalDateTime.now());
        ipGroupMemberMapper.updateById(member);
    }

    @Override
    @Transactional
    @AuditLog(module = "M1-ip-group", action = "delete-member")
    public void deleteMember(Long groupId, Long memberId) {
        requireMember(groupId, memberId);
        ipGroupMemberMapper.deleteById(memberId);
    }

    @Override
    @Transactional
    @AuditLog(module = "M1-ip-group", action = "bind-accounts")
    public void bindAccounts(Long groupId, IpGroupAccountBindReq req) {
        IpGroupDO entity = requireGroup(groupId);
        assertAccountBindGroupType(entity);
        Long tenantId = entity.getTenantId();
        for (Long accountId : req.getAccountIds()) {
            AccountDO account = accountMapper.selectById(accountId);
            if (account == null || !Objects.equals(account.getTenantId(), tenantId)) {
                throw new ServiceException(OaErrorCodes.ENTITY_NOT_EXISTS);
            }
            if (account.getIpGroupId() != null && !Objects.equals(account.getIpGroupId(), groupId)) {
                throw new ServiceException(OaErrorCodes.IP_GROUP_ACCOUNT_BOUND);
            }
            account.setIpGroupId(groupId);
            account.setLinkedAt(LocalDateTime.now());
            account.setUpdater(TenantContextHolder.getUsername());
            account.setUpdateTime(LocalDateTime.now());
            accountMapper.updateById(account);
        }
    }

    @Override
    @Transactional
    @AuditLog(module = "M1-ip-group", action = "unbind-account")
    public void unbindAccount(Long groupId, Long accountId) {
        IpGroupDO entity = requireGroup(groupId);
        AccountDO account = accountMapper.selectById(accountId);
        if (account == null || !Objects.equals(account.getTenantId(), entity.getTenantId())) {
            throw new ServiceException(OaErrorCodes.ENTITY_NOT_EXISTS);
        }
        if (!Objects.equals(account.getIpGroupId(), groupId)) {
            throw new ServiceException(OaErrorCodes.ENTITY_NOT_EXISTS);
        }
        accountMapper.update(null, new LambdaUpdateWrapper<AccountDO>()
                .eq(AccountDO::getId, accountId)
                .eq(AccountDO::getTenantId, entity.getTenantId())
                .set(AccountDO::getIpGroupId, null)
                .set(AccountDO::getLinkedAt, null)
                .set(AccountDO::getUpdater, TenantContextHolder.getUsername())
                .set(AccountDO::getUpdateTime, LocalDateTime.now()));
    }

    @Override
    public List<IpGroupAnchorVO> listAnchors(Long id) {
        IpGroupDO entity = requireGroup(id);
        List<IpGroupAnchorRelDO> rels = ipGroupAnchorRelMapper.selectList(new LambdaQueryWrapper<IpGroupAnchorRelDO>()
                .eq(IpGroupAnchorRelDO::getTenantId, entity.getTenantId())
                .eq(IpGroupAnchorRelDO::getIpGroupId, id)
                .orderByDesc(IpGroupAnchorRelDO::getId));
        if (rels.isEmpty()) {
            return Collections.emptyList();
        }
        Set<Long> userIds = rels.stream().map(IpGroupAnchorRelDO::getAnchorUserId).collect(Collectors.toSet());
        Map<Long, SysUserDO> userMap = sysUserMapper.selectList(new LambdaQueryWrapper<SysUserDO>()
                        .in(SysUserDO::getId, userIds))
                .stream()
                .collect(Collectors.toMap(SysUserDO::getId, u -> u, (a, b) -> a));
        return rels.stream().map(rel -> {
            IpGroupAnchorVO vo = new IpGroupAnchorVO();
            vo.setRelId(rel.getId());
            vo.setAnchorUserId(rel.getAnchorUserId());
            SysUserDO user = userMap.get(rel.getAnchorUserId());
            if (user != null) {
                vo.setAnchorUserName(user.getNickname() != null ? user.getNickname() : user.getUsername());
            }
            vo.setAnchorType(rel.getAnchorType());
            vo.setBoundAt(rel.getCreateTime());
            return vo;
        }).collect(Collectors.toList());
    }

    @Override
    @Transactional
    @AuditLog(module = "M1-ip-group", action = "bind-anchors")
    public void bindAnchors(Long groupId, IpGroupAnchorBindReq req) {
        IpGroupDO entity = requireGroup(groupId);
        Long tenantId = entity.getTenantId();
        String anchorType = StrUtil.blankToDefault(req.getAnchorType(), "VIDEO");
        for (Long anchorUserId : req.getAnchorUserIds()) {
            requireUser(tenantId, anchorUserId);
            long exists = ipGroupAnchorRelMapper.selectCount(new LambdaQueryWrapper<IpGroupAnchorRelDO>()
                    .eq(IpGroupAnchorRelDO::getTenantId, tenantId)
                    .eq(IpGroupAnchorRelDO::getIpGroupId, groupId)
                    .eq(IpGroupAnchorRelDO::getAnchorUserId, anchorUserId));
            if (exists > 0) {
                continue;
            }
            IpGroupAnchorRelDO rel = new IpGroupAnchorRelDO();
            rel.setTenantId(tenantId);
            rel.setIpGroupId(groupId);
            rel.setAnchorUserId(anchorUserId);
            rel.setAnchorType(anchorType);
            rel.setCreator(TenantContextHolder.getUsername());
            rel.setUpdater(TenantContextHolder.getUsername());
            rel.setCreateTime(LocalDateTime.now());
            rel.setUpdateTime(LocalDateTime.now());
            ipGroupAnchorRelMapper.insert(rel);
        }
    }

    @Override
    @Transactional
    @AuditLog(module = "M1-ip-group", action = "unbind-anchor")
    public void unbindAnchor(Long groupId, Long anchorUserId) {
        IpGroupDO entity = requireGroup(groupId);
        IpGroupAnchorRelDO rel = ipGroupAnchorRelMapper.selectOne(new LambdaQueryWrapper<IpGroupAnchorRelDO>()
                .eq(IpGroupAnchorRelDO::getTenantId, entity.getTenantId())
                .eq(IpGroupAnchorRelDO::getIpGroupId, groupId)
                .eq(IpGroupAnchorRelDO::getAnchorUserId, anchorUserId));
        if (rel == null) {
            throw new ServiceException(OaErrorCodes.ENTITY_NOT_EXISTS);
        }
        ipGroupAnchorRelMapper.deleteById(rel.getId());
    }

    private void assertAccountBindGroupType(IpGroupDO entity) {
        if (entity.getGroupType() == null || entity.getGroupType() != 2) {
            throw new ServiceException(OaErrorCodes.IP_GROUP_ACCOUNT_SMALL_ONLY);
        }
    }

    private IpGroupMemberVO toMemberVO(IpGroupMemberDO member, SysUserDO user) {
        IpGroupMemberVO vo = new IpGroupMemberVO();
        vo.setMemberId(member.getId());
        vo.setUserId(member.getUserId());
        if (user != null) {
            vo.setUserName(user.getNickname() != null ? user.getNickname() : user.getUsername());
        }
        vo.setPosition(member.getPosition());
        vo.setPositionText(resolvePositionLabel(member.getPosition()));
        vo.setIsLeader(member.getIsLeader() != null && member.getIsLeader() == 1);
        vo.setJoinTime(member.getCreateTime());
        return vo;
    }

    private String resolvePositionLabel(String position) {
        if (StrUtil.isBlank(position)) {
            return "成员";
        }
        SysDictDataDO dict = sysDictDataMapper.selectOne(new LambdaQueryWrapper<SysDictDataDO>()
                .eq(SysDictDataDO::getDictType, "dict_position")
                .eq(SysDictDataDO::getDictValue, position)
                .eq(SysDictDataDO::getStatus, "ENABLED")
                .last("LIMIT 1"));
        return dict != null && StrUtil.isNotBlank(dict.getLabel()) ? dict.getLabel() : position;
    }

    private void validateGroupName(String groupName) {
        if (StrUtil.isBlank(groupName) || groupName.trim().length() > 50) {
            throw new ServiceException(OaErrorCodes.IP_GROUP_NAME_INVALID);
        }
    }

    private void assertNameUnique(Long tenantId, Long parentId, String groupName, Long excludeId) {
        LambdaQueryWrapper<IpGroupDO> wrapper = new LambdaQueryWrapper<IpGroupDO>()
                .eq(IpGroupDO::getTenantId, tenantId)
                .eq(IpGroupDO::getGroupName, groupName.trim());
        if (parentId == null) {
            wrapper.isNull(IpGroupDO::getParentId);
        } else {
            wrapper.eq(IpGroupDO::getParentId, parentId);
        }
        if (excludeId != null) {
            wrapper.ne(IpGroupDO::getId, excludeId);
        }
        if (ipGroupMapper.selectCount(wrapper) > 0) {
            throw new ServiceException(OaErrorCodes.IP_GROUP_NAME_DUPLICATE);
        }
    }

    private void validateGroupTypeAndParent(Long tenantId, Integer groupType, Long parentId) {
        if (groupType == null || (groupType != 1 && groupType != 2)) {
            throw new ServiceException(OaErrorCodes.IP_GROUP_PARENT_INVALID);
        }
        if (groupType == 1) {
            if (parentId != null) {
                throw new ServiceException(OaErrorCodes.IP_GROUP_PARENT_INVALID);
            }
            return;
        }
        if (parentId == null) {
            throw new ServiceException(OaErrorCodes.IP_GROUP_PARENT_INVALID);
        }
        IpGroupDO parent = ipGroupMapper.selectById(parentId);
        if (parent == null || !Objects.equals(parent.getTenantId(), tenantId) || parent.getGroupType() != 1) {
            throw new ServiceException(OaErrorCodes.IP_GROUP_PARENT_INVALID);
        }
    }

    private Long resolveLeaderUserId(Long leaderId, Long leaderUserId) {
        return leaderUserId != null ? leaderUserId : leaderId;
    }

    private void assertLeaderExists(Long tenantId, Long leaderUserId) {
        requireUser(tenantId, leaderUserId);
    }

    private SysUserDO requireUser(Long tenantId, Long userId) {
        SysUserDO user = sysUserMapper.selectById(userId);
        if (user == null || !Objects.equals(user.getTenantId(), tenantId)) {
            throw new ServiceException(OaErrorCodes.IP_GROUP_LEADER_NOT_FOUND);
        }
        return user;
    }

    private void assertDeletable(IpGroupDO entity) {
        Long tenantId = entity.getTenantId();
        Long id = entity.getId();

        long childGroups = ipGroupMapper.selectCount(new LambdaQueryWrapper<IpGroupDO>()
                .eq(IpGroupDO::getTenantId, tenantId)
                .eq(IpGroupDO::getParentId, id));
        if (childGroups > 0) {
            throw new ServiceException(OaErrorCodes.IP_GROUP_HAS_DATA);
        }

        long members = ipGroupMemberMapper.selectCount(new LambdaQueryWrapper<IpGroupMemberDO>()
                .eq(IpGroupMemberDO::getTenantId, tenantId)
                .eq(IpGroupMemberDO::getIpGroupId, id));
        if (members > 0) {
            throw new ServiceException(OaErrorCodes.IP_GROUP_HAS_DATA);
        }

        long accounts = accountMapper.selectCount(new LambdaQueryWrapper<AccountDO>()
                .eq(AccountDO::getTenantId, tenantId)
                .eq(AccountDO::getIpGroupId, id));
        if (accounts > 0) {
            throw new ServiceException(OaErrorCodes.IP_GROUP_HAS_DATA);
        }

        long anchors = ipGroupAnchorRelMapper.selectCount(new LambdaQueryWrapper<IpGroupAnchorRelDO>()
                .eq(IpGroupAnchorRelDO::getTenantId, tenantId)
                .eq(IpGroupAnchorRelDO::getIpGroupId, id));
        if (anchors > 0) {
            throw new ServiceException(OaErrorCodes.IP_GROUP_HAS_DATA);
        }
    }

    private void assertMemberGroupType(IpGroupDO entity) {
        if (entity.getGroupType() != null && entity.getGroupType() == 1) {
            throw new ServiceException(OaErrorCodes.IP_GROUP_PARENT_INVALID.getCode(), "大组不可直接添加成员");
        }
    }

    private void assertMemberNotExists(Long tenantId, Long groupId, Long userId) {
        long count = ipGroupMemberMapper.selectCount(new LambdaQueryWrapper<IpGroupMemberDO>()
                .eq(IpGroupMemberDO::getTenantId, tenantId)
                .eq(IpGroupMemberDO::getIpGroupId, groupId)
                .eq(IpGroupMemberDO::getUserId, userId));
        if (count > 0) {
            throw new ServiceException(OaErrorCodes.DUPLICATE_ENTITY);
        }
    }

    private IpGroupMemberDO requireMember(Long groupId, Long memberId) {
        requireGroup(groupId);
        IpGroupMemberDO member = ipGroupMemberMapper.selectById(memberId);
        if (member == null || !Objects.equals(member.getIpGroupId(), groupId)
                || !Objects.equals(member.getTenantId(), requireTenantId())) {
            throw new ServiceException(OaErrorCodes.ENTITY_NOT_EXISTS);
        }
        return member;
    }

    private IpGroupTreeVO toTreeNode(IpGroupDO entity,
                                     Map<Long, String> nameMap,
                                     Map<Long, String> leaderNameMap,
                                     Map<Long, Integer> memberCounts,
                                     Map<Long, Integer> accountCounts,
                                     Map<Long, Integer> anchorCounts,
                                     Map<Long, List<IpGroupDO>> childrenMap) {
        IpGroupTreeVO vo = new IpGroupTreeVO();
        vo.setId(entity.getId());
        vo.setGroupName(entity.getGroupName());
        vo.setGroupType(entity.getGroupType());
        vo.setParentId(entity.getParentId());
        vo.setParentName(entity.getParentId() == null ? null : nameMap.get(entity.getParentId()));
        vo.setLeaderId(entity.getLeaderUserId());
        vo.setLeaderName(entity.getLeaderUserId() == null ? null : leaderNameMap.get(entity.getLeaderUserId()));
        vo.setStatus(entity.getStatus());
        vo.setCreateTime(entity.getCreateTime());

        Set<Long> scopeIds = collectScopeIds(entity, childrenMap);
        vo.setMemberCount(sumCounts(scopeIds, memberCounts));
        vo.setAccountCount(sumCounts(scopeIds, accountCounts));
        vo.setAnchorCount(sumCounts(scopeIds, anchorCounts));

        List<IpGroupDO> children = childrenMap.getOrDefault(entity.getId(), Collections.emptyList());
        vo.setChildren(children.stream()
                .map(child -> toTreeNode(child, nameMap, leaderNameMap, memberCounts, accountCounts, anchorCounts, childrenMap))
                .collect(Collectors.toList()));
        return vo;
    }

    private Set<Long> collectScopeIds(IpGroupDO entity) {
        if (entity.getGroupType() != null && entity.getGroupType() == 2) {
            return Set.of(entity.getId());
        }
        List<IpGroupDO> children = ipGroupMapper.selectList(new LambdaQueryWrapper<IpGroupDO>()
                .eq(IpGroupDO::getTenantId, entity.getTenantId())
                .eq(IpGroupDO::getParentId, entity.getId()));
        if (children.isEmpty()) {
            return Set.of(entity.getId());
        }
        return children.stream().map(IpGroupDO::getId).collect(Collectors.toSet());
    }

    private Set<Long> collectScopeIds(IpGroupDO entity, Map<Long, List<IpGroupDO>> childrenMap) {
        if (entity.getGroupType() != null && entity.getGroupType() == 2) {
            return Set.of(entity.getId());
        }
        List<IpGroupDO> children = childrenMap.getOrDefault(entity.getId(), Collections.emptyList());
        if (children.isEmpty()) {
            return Set.of(entity.getId());
        }
        return children.stream().map(IpGroupDO::getId).collect(Collectors.toSet());
    }

    /**
     * P-GATE-UNMOCK S-E: 判断 candidateId 是否为 ancestorId 的子孙节点（防 parentId 循环引用）。
     * 沿 candidateId.parentId 链上溯最多 100 层（防止病态数据死循环）。
     */
    private boolean isDescendant(Long candidateId, Long ancestorId) {
        if (candidateId == null || ancestorId == null) {
            return false;
        }
        Long cursor = candidateId;
        for (int i = 0; i < 100; i++) {
            IpGroupDO node = ipGroupMapper.selectById(cursor);
            if (node == null) {
                return false;
            }
            if (Objects.equals(node.getParentId(), ancestorId)) {
                return true;
            }
            if (node.getParentId() == null) {
                return false;
            }
            cursor = node.getParentId();
        }
        return false;
    }

    private int sumCounts(Set<Long> ids, Map<Long, Integer> counts) {
        return ids.stream().mapToInt(id -> counts.getOrDefault(id, 0)).sum();
    }

    private int countMembers(Set<Long> groupIds) {
        if (groupIds.isEmpty()) {
            return 0;
        }
        Long tenantId = requireTenantId();
        return Math.toIntExact(ipGroupMemberMapper.selectCount(new LambdaQueryWrapper<IpGroupMemberDO>()
                .eq(IpGroupMemberDO::getTenantId, tenantId)
                .in(IpGroupMemberDO::getIpGroupId, groupIds)));
    }

    private int countAccounts(Set<Long> groupIds) {
        if (groupIds.isEmpty()) {
            return 0;
        }
        Long tenantId = requireTenantId();
        LambdaQueryWrapper<AccountDO> wrapper = new LambdaQueryWrapper<AccountDO>()
                .eq(AccountDO::getTenantId, tenantId)
                .in(AccountDO::getIpGroupId, groupIds);
        DataScopeSupport.applyIpGroupScope(wrapper, AccountDO::getIpGroupId);
        return Math.toIntExact(accountMapper.selectCount(wrapper));
    }

    private int countAnchors(Set<Long> groupIds) {
        if (groupIds.isEmpty()) {
            return 0;
        }
        Long tenantId = requireTenantId();
        return Math.toIntExact(ipGroupAnchorRelMapper.selectCount(new LambdaQueryWrapper<IpGroupAnchorRelDO>()
                .eq(IpGroupAnchorRelDO::getTenantId, tenantId)
                .in(IpGroupAnchorRelDO::getIpGroupId, groupIds)));
    }

    private Map<Long, Integer> countAccounts(Long tenantId) {
        LambdaQueryWrapper<AccountDO> wrapper = new LambdaQueryWrapper<AccountDO>()
                .eq(AccountDO::getTenantId, tenantId)
                .isNotNull(AccountDO::getIpGroupId);
        DataScopeSupport.applyIpGroupScope(wrapper, AccountDO::getIpGroupId);
        List<AccountDO> accounts = accountMapper.selectList(wrapper);
        Map<Long, Integer> counts = new HashMap<>();
        for (AccountDO account : accounts) {
            counts.merge(account.getIpGroupId(), 1, Integer::sum);
        }
        return counts;
    }

    private Map<Long, Integer> countMembersByGroup(Long tenantId) {
        List<IpGroupMemberDO> rows = ipGroupMemberMapper.selectList(new LambdaQueryWrapper<IpGroupMemberDO>()
                .eq(IpGroupMemberDO::getTenantId, tenantId));
        Map<Long, Integer> counts = new HashMap<>();
        for (IpGroupMemberDO row : rows) {
            counts.merge(row.getIpGroupId(), 1, Integer::sum);
        }
        return counts;
    }

    private Map<Long, Integer> countAnchorsByGroup(Long tenantId) {
        List<IpGroupAnchorRelDO> rows = ipGroupAnchorRelMapper.selectList(new LambdaQueryWrapper<IpGroupAnchorRelDO>()
                .eq(IpGroupAnchorRelDO::getTenantId, tenantId));
        Map<Long, Integer> counts = new HashMap<>();
        for (IpGroupAnchorRelDO row : rows) {
            counts.merge(row.getIpGroupId(), 1, Integer::sum);
        }
        return counts;
    }

    private Map<Long, String> loadLeaderNames(List<IpGroupDO> groups) {
        Set<Long> leaderIds = groups.stream()
                .map(IpGroupDO::getLeaderUserId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        if (leaderIds.isEmpty()) {
            return Collections.emptyMap();
        }
        return sysUserMapper.selectList(new LambdaQueryWrapper<SysUserDO>().in(SysUserDO::getId, leaderIds)).stream()
                .collect(Collectors.toMap(SysUserDO::getId, u -> u.getNickname() != null ? u.getNickname() : u.getUsername(), (a, b) -> a));
    }

    private IpGroupDO requireGroup(Long id) {
        IpGroupDO entity = ipGroupMapper.selectById(id);
        if (entity == null) {
            throw new ServiceException(OaErrorCodes.ENTITY_NOT_EXISTS);
        }
        if (!Objects.equals(entity.getTenantId(), requireTenantId())) {
            throw new ServiceException(OaErrorCodes.TENANT_FORBIDDEN);
        }
        return entity;
    }

    private Long requireTenantId() {
        Long tenantId = TenantContextHolder.getTenantId();
        if (tenantId == null) {
            throw new ServiceException(OaErrorCodes.UNAUTHORIZED.getCode(), "缺少租户上下文");
        }
        return tenantId;
    }
}
