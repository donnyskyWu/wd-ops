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
import cn.iocoder.yudao.module.oa.dal.dataobject.author.AuthorUserDO;
import cn.iocoder.yudao.module.oa.dal.dataobject.author.OaAuthorExtDO;
import cn.iocoder.yudao.module.oa.dal.dataobject.ipgroup.IpGroupAnchorRelDO;
import cn.iocoder.yudao.module.oa.dal.dataobject.ipgroup.IpGroupDO;
import cn.iocoder.yudao.module.oa.dal.dataobject.ipgroup.IpGroupMemberDO;
import cn.iocoder.yudao.module.oa.dal.mysql.account.AccountMapper;
import cn.iocoder.yudao.module.oa.dal.mysql.author.OaAuthorExtMapper;
import cn.iocoder.yudao.module.oa.dal.mysql.ipgroup.IpGroupAnchorRelMapper;
import cn.iocoder.yudao.module.oa.dal.mysql.ipgroup.IpGroupMapper;
import cn.iocoder.yudao.module.oa.dal.mysql.ipgroup.IpGroupMemberMapper;
import cn.iocoder.yudao.module.oa.dal.dataobject.dict.SysDictDataDO;
import cn.iocoder.yudao.module.oa.service.dict.DictService;
import cn.iocoder.yudao.module.oa.framework.auth.DataScopeSupport;
import com.mzt.logapi.context.LogRecordContext;
import com.mzt.logapi.service.impl.DiffParseFunction;
import com.mzt.logapi.starter.annotation.LogRecord;

import static cn.iocoder.yudao.module.oa.framework.operatelog.OaLogRecordConstants.*;
import cn.iocoder.yudao.module.oa.service.author.AuthorResolveSupport;
import cn.iocoder.yudao.module.oa.service.author.MemberAuthorReadService;
import cn.iocoder.yudao.module.oa.service.auth.OpsDataScopeSupport;
import cn.iocoder.yudao.module.oa.enums.IpGroupRoleCodes;
import cn.iocoder.yudao.module.oa.service.support.FootballSystemUserValidator;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
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
    private final DictService dictService;
    private final IpGroupAnchorRelMapper ipGroupAnchorRelMapper;
    private final OaAuthorExtMapper oaAuthorExtMapper;
    private final AccountMapper accountMapper;
    private final FootballSystemUserValidator footballSystemUserValidator;
    private final AuthorResolveSupport authorResolveSupport;
    private final MemberAuthorReadService memberAuthorReadService;
    private final IpGroupAccessSupport ipGroupAccessSupport;
    private final OpsDataScopeSupport opsDataScopeSupport;

    @Override
    public List<IpGroupTreeVO> getTree() {
        opsDataScopeSupport.assertIpGroupManagementListAccess();
        Long tenantId = requireTenantId();
        Set<Long> scopeIds = opsDataScopeSupport.resolveIpGroupManagementScopeIds();
        if (scopeIds != null && scopeIds.size() == 1 && scopeIds.contains(-1L)) {
            return Collections.emptyList();
        }
        List<IpGroupDO> all = ipGroupMapper.selectList(new LambdaQueryWrapper<IpGroupDO>()
                .eq(IpGroupDO::getTenantId, tenantId)
                .orderByAsc(IpGroupDO::getSortOrder)
                .orderByAsc(IpGroupDO::getId));
        if (all.isEmpty()) {
            return Collections.emptyList();
        }
        if (scopeIds != null) {
            return buildTreeForScopeIds(all, scopeIds);
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
    public List<IpGroupListVO> listLedByCurrentUser() {
        Long tenantId = requireTenantId();
        Set<Long> userIds = ipGroupAccessSupport.resolveMembershipUserIds(tenantId);
        if (userIds.isEmpty()) {
            return Collections.emptyList();
        }

        Map<Long, IpGroupDO> ledGroups = new java.util.LinkedHashMap<>();
        ipGroupMapper.selectList(new LambdaQueryWrapper<IpGroupDO>()
                        .eq(IpGroupDO::getTenantId, tenantId)
                        .eq(IpGroupDO::getGroupType, 2)
                        .eq(IpGroupDO::getStatus, 1)
                        .in(IpGroupDO::getLeaderUserId, userIds)
                        .orderByAsc(IpGroupDO::getSortOrder)
                        .orderByAsc(IpGroupDO::getId))
                .forEach(group -> ledGroups.put(group.getId(), group));

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
                        ledGroups.put(group.getId(), group);
                    }
                });

        if (ledGroups.isEmpty()) {
            return Collections.emptyList();
        }

        List<IpGroupDO> groups = new java.util.ArrayList<>(ledGroups.values());
        Map<Long, String> leaderNameMap = loadLeaderNames(groups);
        return groups.stream().map(group -> {
            IpGroupListVO vo = new IpGroupListVO();
            vo.setId(group.getId());
            vo.setGroupName(group.getGroupName());
            vo.setGroupType(group.getGroupType());
            vo.setParentId(group.getParentId());
            vo.setLeaderName(group.getLeaderUserId() == null ? null : leaderNameMap.get(group.getLeaderUserId()));
            vo.setStatus(group.getStatus());
            vo.setLevel(group.getLevel());
            vo.setCreateTime(group.getCreateTime());
            return vo;
        }).collect(Collectors.toList());
    }

    @Override
    public List<Long> listLeaderCandidateUserIds() {
        Long tenantId = requireTenantId();
        return footballSystemUserValidator.listPresentableUserIdsByRoleCode(
                tenantId, IpGroupRoleCodes.IP_GROUP_LEADER);
    }

    @Override
    public List<IpGroupTreeVO> getAccessibleTree() {
        if (ipGroupAccessSupport.hasUnrestrictedIpGroupAccess()) {
            return getTree();
        }
        Long tenantId = requireTenantId();
        Set<Long> accessibleIds = ipGroupAccessSupport.resolveAccessibleIpGroupIds(tenantId);
        if (accessibleIds == null || accessibleIds.isEmpty()) {
            return Collections.emptyList();
        }

        List<IpGroupDO> all = ipGroupMapper.selectList(new LambdaQueryWrapper<IpGroupDO>()
                .eq(IpGroupDO::getTenantId, tenantId)
                .orderByAsc(IpGroupDO::getSortOrder)
                .orderByAsc(IpGroupDO::getId));
        if (all.isEmpty()) {
            return Collections.emptyList();
        }

        Map<Long, IpGroupDO> allMap = all.stream()
                .collect(Collectors.toMap(IpGroupDO::getId, g -> g, (a, b) -> a, LinkedHashMap::new));
        Set<Long> visibleIds = new LinkedHashSet<>();
        for (Long accessibleId : accessibleIds) {
            Long cursor = accessibleId;
            for (int depth = 0; depth < 100 && cursor != null; depth++) {
                visibleIds.add(cursor);
                IpGroupDO node = allMap.get(cursor);
                cursor = node == null ? null : node.getParentId();
            }
        }

        List<IpGroupDO> filtered = all.stream()
                .filter(g -> visibleIds.contains(g.getId()))
                .collect(Collectors.toList());
        if (filtered.isEmpty()) {
            return Collections.emptyList();
        }

        Map<Long, String> nameMap = filtered.stream()
                .collect(Collectors.toMap(IpGroupDO::getId, IpGroupDO::getGroupName, (a, b) -> a));
        Map<Long, String> leaderNameMap = loadLeaderNames(filtered);
        Map<Long, Integer> memberCounts = countMembersByGroup(tenantId);
        Map<Long, Integer> accountCounts = countAccounts(tenantId);
        Map<Long, Integer> anchorCounts = countAnchorsByGroup(tenantId);
        Map<Long, List<IpGroupDO>> childrenMap = filtered.stream()
                .filter(g -> g.getParentId() != null && visibleIds.contains(g.getParentId()))
                .collect(Collectors.groupingBy(IpGroupDO::getParentId));

        return filtered.stream()
                .filter(g -> g.getParentId() == null || !visibleIds.contains(g.getParentId()))
                .map(root -> toTreeNode(root, nameMap, leaderNameMap, memberCounts, accountCounts, anchorCounts, childrenMap))
                .collect(Collectors.toList());
    }

    @Override
    public PageResult<IpGroupListVO> listPage(
            String groupName, Integer groupType, Integer status,
            Integer pageNum, Integer pageSize) {
        opsDataScopeSupport.assertIpGroupManagementListAccess();
        Long tenantId = requireTenantId();
        Set<Long> scopeIds = opsDataScopeSupport.resolveIpGroupManagementScopeIds();
        if (scopeIds != null && scopeIds.size() == 1 && scopeIds.contains(-1L)) {
            return PageResult.empty();
        }
        LambdaQueryWrapper<IpGroupDO> wrapper = new LambdaQueryWrapper<IpGroupDO>()
                .eq(IpGroupDO::getTenantId, tenantId)
                .like(StrUtil.isNotBlank(groupName), IpGroupDO::getGroupName, groupName)
                .eq(groupType != null, IpGroupDO::getGroupType, groupType)
                .eq(status != null, IpGroupDO::getStatus, status)
                .orderByAsc(IpGroupDO::getSortOrder)
                .orderByDesc(IpGroupDO::getId);
        if (scopeIds != null) {
            wrapper.in(IpGroupDO::getId, scopeIds);
        }
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
            vo.setLevel(d.getLevel());
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
        Long leaderUserId = entity.getLeaderUserId();
        vo.setLeaderId(leaderUserId == null ? null : footballSystemUserValidator.resolvePresentableUserId(leaderUserId));
        vo.setLeaderName(leaderUserId == null ? null : leaderNameMap.get(leaderUserId));
        vo.setSortOrder(entity.getSortOrder());
        vo.setStatus(entity.getStatus());
        vo.setLevel(entity.getLevel());
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
    @LogRecord(type = M1_IP_GROUP_TYPE, subType = M1_IP_GROUP_CREATE_SUB_TYPE, bizNo = "{{#ipGroup.id}}",
            success = M1_IP_GROUP_CREATE_SUCCESS)
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
        entity.setLevel(req.getLevel());
        entity.setRemark(req.getRemark());
        entity.setCreator(TenantContextHolder.getUsername());
        entity.setUpdater(TenantContextHolder.getUsername());
        entity.setCreateTime(LocalDateTime.now());
        entity.setUpdateTime(LocalDateTime.now());
        ipGroupMapper.insert(entity);
        LogRecordContext.putVariable("ipGroup", entity);
        return entity.getId();
    }

    @Override
    @Transactional
    @LogRecord(type = M1_IP_GROUP_TYPE, subType = M1_IP_GROUP_UPDATE_SUB_TYPE, bizNo = "{{#ipGroup.id}}",
            success = M1_IP_GROUP_UPDATE_SUCCESS)
    public void update(IpGroupUpdateReq req) {
        IpGroupDO existing = requireGroup(req.getId());
        LogRecordContext.putVariable(DiffParseFunction.OLD_OBJECT, toUpdateReq(existing));
        LogRecordContext.putVariable("ipGroup", existing);
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
        if (req.getLevel() != null) {
            existing.setLevel(StrUtil.blankToDefault(req.getLevel(), null));
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
    @LogRecord(type = M1_IP_GROUP_TYPE, subType = M1_IP_GROUP_UPDATE_STATUS_SUB_TYPE, bizNo = "{{#ipGroup.id}}",
            success = M1_IP_GROUP_UPDATE_STATUS_SUCCESS)
    public void updateStatus(Long id, Integer status) {
        IpGroupDO existing = requireGroup(id);
        LogRecordContext.putVariable("ipGroup", existing);
        existing.setStatus(status);
        existing.setUpdater(TenantContextHolder.getUsername());
        existing.setUpdateTime(LocalDateTime.now());
        ipGroupMapper.updateById(existing);
    }

    @Override
    @Transactional
    @LogRecord(type = M1_IP_GROUP_TYPE, subType = M1_IP_GROUP_DELETE_SUB_TYPE, bizNo = "{{#ipGroup.id}}",
            success = M1_IP_GROUP_DELETE_SUCCESS)
    public void delete(Long id) {
        IpGroupDO entity = requireGroup(id);
        LogRecordContext.putVariable("ipGroup", entity);
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
        Map<Long, String> userNames = footballSystemUserValidator.loadNicknames(userIds);
        return members.stream().map(m -> {
            Long presentableUserId = footballSystemUserValidator.resolvePresentableUserId(m.getUserId());
            String userName = footballSystemUserValidator.resolveMemberDisplayName(m.getUserId(), userNames.get(m.getUserId()));
            IpGroupMemberVO vo = toMemberVO(m, userName);
            vo.setUserId(presentableUserId);
            return vo;
        }).collect(Collectors.toList());
    }

    @Override
    @Transactional
    @LogRecord(type = M1_IP_GROUP_TYPE, subType = M1_IP_GROUP_ADD_MEMBER_SUB_TYPE, bizNo = "{{#ipGroup.id}}",
            success = M1_IP_GROUP_ADD_MEMBER_SUCCESS)
    public void addMember(Long groupId, IpGroupMemberCreateReq req) {
        IpGroupDO entity = requireGroup(groupId);
        LogRecordContext.putVariable("ipGroup", entity);
        assertMemberGroupType(entity);
        assertUserExists(entity.getTenantId(), req.getUserId());
        assertMemberNotExists(entity.getTenantId(), groupId, req.getUserId());

        IpGroupMemberDO member = new IpGroupMemberDO();
        member.setTenantId(entity.getTenantId());
        member.setIpGroupId(groupId);
        member.setUserId(req.getUserId());
        member.setPosition(StrUtil.blankToDefault(req.getPosition(),
                footballSystemUserValidator.resolveLegacyPosition(req.getUserId())));
        member.setIsLeader(Boolean.TRUE.equals(req.getIsLeader()) ? 1 : 0);
        member.setCreator(TenantContextHolder.getUsername());
        member.setUpdater(TenantContextHolder.getUsername());
        member.setCreateTime(LocalDateTime.now());
        member.setUpdateTime(LocalDateTime.now());
        ipGroupMemberMapper.insert(member);
    }

    @Override
    @Transactional
    @LogRecord(type = M1_IP_GROUP_TYPE, subType = M1_IP_GROUP_UPDATE_MEMBER_SUB_TYPE, bizNo = "{{#ipGroup.id}}",
            success = M1_IP_GROUP_UPDATE_MEMBER_SUCCESS)
    public void updateMember(Long groupId, Long memberId, IpGroupMemberUpdateReq req) {
        IpGroupDO entity = requireGroup(groupId);
        LogRecordContext.putVariable("ipGroup", entity);
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
    @LogRecord(type = M1_IP_GROUP_TYPE, subType = M1_IP_GROUP_DELETE_MEMBER_SUB_TYPE, bizNo = "{{#ipGroup.id}}",
            success = M1_IP_GROUP_DELETE_MEMBER_SUCCESS)
    public void deleteMember(Long groupId, Long memberId) {
        IpGroupDO entity = requireGroup(groupId);
        LogRecordContext.putVariable("ipGroup", entity);
        requireMember(groupId, memberId);
        ipGroupMemberMapper.deleteById(memberId);
    }

    @Override
    @Transactional
    @LogRecord(type = M1_IP_GROUP_TYPE, subType = M1_IP_GROUP_BIND_ACCOUNTS_SUB_TYPE, bizNo = "{{#ipGroup.id}}",
            success = M1_IP_GROUP_BIND_ACCOUNTS_SUCCESS)
    public void bindAccounts(Long groupId, IpGroupAccountBindReq req) {
        IpGroupDO entity = requireGroup(groupId);
        LogRecordContext.putVariable("ipGroup", entity);
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
    @LogRecord(type = M1_IP_GROUP_TYPE, subType = M1_IP_GROUP_UNBIND_ACCOUNT_SUB_TYPE, bizNo = "{{#ipGroup.id}}",
            success = M1_IP_GROUP_UNBIND_ACCOUNT_SUCCESS)
    public void unbindAccount(Long groupId, Long accountId) {
        IpGroupDO entity = requireGroup(groupId);
        LogRecordContext.putVariable("ipGroup", entity);
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
        Set<Long> authorUserIds = rels.stream().map(IpGroupAnchorRelDO::getAnchorUserId).collect(Collectors.toSet());
        Map<Long, AuthorUserDO> authors = memberAuthorReadService.loadByIds(authorUserIds);
        IpGroupDO group = entity;
        String groupLevel = group.getLevel();
        return rels.stream().map(rel -> {
            IpGroupAnchorVO vo = new IpGroupAnchorVO();
            vo.setRelId(rel.getId());
            vo.setAnchorUserId(rel.getAnchorUserId());
            vo.setAuthorId(rel.getAnchorUserId());
            AuthorUserDO author = authors.get(rel.getAnchorUserId());
            String name = author != null ? author.getNickname() : null;
            vo.setAnchorUserName(name);
            vo.setAuthorName(name);
            vo.setAnchorType(rel.getAnchorType());
            if (author != null) {
                vo.setAuthorLevel(author.getAuthorLevel());
            }
            vo.setIpGroupLevel(groupLevel);
            vo.setBoundAt(rel.getCreateTime());
            return vo;
        }).collect(Collectors.toList());
    }

    @Override
    @Transactional
    @LogRecord(type = M1_IP_GROUP_TYPE, subType = M1_IP_GROUP_BIND_ANCHORS_SUB_TYPE, bizNo = "{{#ipGroup.id}}",
            success = M1_IP_GROUP_BIND_ANCHORS_SUCCESS)
    public void bindAnchors(Long groupId, IpGroupAnchorBindReq req) {
        IpGroupDO entity = requireGroup(groupId);
        LogRecordContext.putVariable("ipGroup", entity);
        Long tenantId = entity.getTenantId();
        for (Long anchorUserId : req.getAnchorUserIds()) {
            assertAuthorExists(tenantId, anchorUserId);
            String anchorType = StrUtil.blankToDefault(req.getAnchorType(), resolveAuthorAnchorType(anchorUserId));
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
    @LogRecord(type = M1_IP_GROUP_TYPE, subType = M1_IP_GROUP_UNBIND_ANCHOR_SUB_TYPE, bizNo = "{{#ipGroup.id}}",
            success = M1_IP_GROUP_UNBIND_ANCHOR_SUCCESS)
    public void unbindAnchor(Long groupId, Long anchorUserId) {
        IpGroupDO entity = requireGroup(groupId);
        LogRecordContext.putVariable("ipGroup", entity);
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

    private IpGroupMemberVO toMemberVO(IpGroupMemberDO member, String userName) {
        IpGroupMemberVO vo = new IpGroupMemberVO();
        vo.setMemberId(member.getId());
        vo.setUserId(member.getUserId());
        vo.setUserName(userName);
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
        return dictService.listByType("dict_position").stream()
                .filter(row -> position.equals(row.getDictValue()))
                .map(SysDictDataDO::getLabel)
                .filter(StrUtil::isNotBlank)
                .findFirst()
                .orElse(position);
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
        assertUserExists(tenantId, leaderUserId);
        footballSystemUserValidator.assertHasRoleCode(
                leaderUserId, tenantId, IpGroupRoleCodes.IP_GROUP_LEADER,
                OaErrorCodes.IP_GROUP_LEADER_ROLE_REQUIRED);
    }

    private void assertUserExists(Long tenantId, Long userId) {
        footballSystemUserValidator.assertInTenant(userId, tenantId, OaErrorCodes.IP_GROUP_LEADER_NOT_FOUND.getMsg());
    }

    private String resolveAuthorAnchorType(Long authorUserId) {
        OaAuthorExtDO ext = oaAuthorExtMapper.selectById(authorUserId);
        if (ext != null && StrUtil.isNotBlank(ext.getAuthorType())) {
            return ext.getAuthorType();
        }
        return "VIDEO";
    }

    private void assertAuthorExists(Long tenantId, Long authorUserId) {
        authorResolveSupport.requireAuthorUser(authorUserId, tenantId);
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
        vo.setLevel(entity.getLevel());
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
        Map<Long, String> batchNames = footballSystemUserValidator.loadNicknames(leaderIds);
        Map<Long, String> names = new HashMap<>();
        for (Long leaderId : leaderIds) {
            names.put(leaderId, footballSystemUserValidator.resolveMemberDisplayName(leaderId, batchNames.get(leaderId)));
        }
        return names;
    }

    private IpGroupDO requireGroup(Long id) {
        IpGroupDO entity = ipGroupMapper.selectById(id);
        if (entity == null) {
            throw new ServiceException(OaErrorCodes.ENTITY_NOT_EXISTS);
        }
        if (!Objects.equals(entity.getTenantId(), requireTenantId())) {
            throw new ServiceException(OaErrorCodes.TENANT_FORBIDDEN);
        }
        opsDataScopeSupport.assertIpGroupLedReadable(entity.getId());
        return entity;
    }

    /** 6159 IP 组长树：含管辖组及其祖先链 */
    private List<IpGroupTreeVO> buildTreeForScopeIds(List<IpGroupDO> all, Set<Long> scopeIds) {
        Map<Long, IpGroupDO> allMap = all.stream()
                .collect(Collectors.toMap(IpGroupDO::getId, g -> g, (a, b) -> a, LinkedHashMap::new));
        Set<Long> visibleIds = new LinkedHashSet<>();
        for (Long scopeId : scopeIds) {
            Long cursor = scopeId;
            for (int depth = 0; depth < 100 && cursor != null; depth++) {
                visibleIds.add(cursor);
                IpGroupDO node = allMap.get(cursor);
                cursor = node == null ? null : node.getParentId();
            }
        }
        List<IpGroupDO> filtered = all.stream()
                .filter(g -> visibleIds.contains(g.getId()))
                .collect(Collectors.toList());
        if (filtered.isEmpty()) {
            return Collections.emptyList();
        }
        Map<Long, String> nameMap = filtered.stream()
                .collect(Collectors.toMap(IpGroupDO::getId, IpGroupDO::getGroupName, (a, b) -> a));
        Map<Long, String> leaderNameMap = loadLeaderNames(filtered);
        Long tenantId = requireTenantId();
        Map<Long, Integer> memberCounts = countMembersByGroup(tenantId);
        Map<Long, Integer> accountCounts = countAccounts(tenantId);
        Map<Long, Integer> anchorCounts = countAnchorsByGroup(tenantId);
        Map<Long, List<IpGroupDO>> childrenMap = filtered.stream()
                .filter(g -> g.getParentId() != null && visibleIds.contains(g.getParentId()))
                .collect(Collectors.groupingBy(IpGroupDO::getParentId));
        return filtered.stream()
                .filter(g -> g.getParentId() == null || !visibleIds.contains(g.getParentId()))
                .map(root -> toTreeNode(root, nameMap, leaderNameMap, memberCounts, accountCounts, anchorCounts, childrenMap))
                .collect(Collectors.toList());
    }

    private Long requireTenantId() {
        Long tenantId = TenantContextHolder.getTenantId();
        if (tenantId == null) {
            throw new ServiceException(OaErrorCodes.UNAUTHORIZED.getCode(), "缺少租户上下文");
        }
        return tenantId;
    }

    private static IpGroupUpdateReq toUpdateReq(IpGroupDO entity) {
        IpGroupUpdateReq req = new IpGroupUpdateReq();
        req.setId(entity.getId());
        req.setGroupName(entity.getGroupName());
        req.setParentId(entity.getParentId());
        req.setLeaderUserId(entity.getLeaderUserId());
        req.setSortOrder(entity.getSortOrder());
        req.setStatus(entity.getStatus());
        req.setLevel(entity.getLevel());
        req.setRemark(entity.getRemark());
        return req;
    }
}
