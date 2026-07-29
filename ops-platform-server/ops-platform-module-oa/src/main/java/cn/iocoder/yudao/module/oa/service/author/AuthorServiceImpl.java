package cn.iocoder.yudao.module.oa.service.author;



import cn.hutool.core.util.StrUtil;

import cn.iocoder.yudao.framework.common.exception.OaErrorCodes;

import cn.iocoder.yudao.framework.common.exception.ServiceException;

import cn.iocoder.yudao.framework.common.pojo.PageResult;

import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;

import cn.iocoder.yudao.module.oa.api.dto.author.AuthorCreateReq;

import cn.iocoder.yudao.module.oa.api.dto.author.AuthorDashboardVO;

import cn.iocoder.yudao.module.oa.api.dto.author.AuthorExtUpdateReq;

import cn.iocoder.yudao.module.oa.api.dto.author.AuthorExtVO;

import cn.iocoder.yudao.module.oa.api.dto.author.AuthorUpdateReq;

import cn.iocoder.yudao.module.oa.api.dto.author.AuthorVO;

import cn.iocoder.yudao.module.oa.api.dto.author.OpsUserVO;

import cn.iocoder.yudao.module.oa.dal.dataobject.account.MpAccountDO;

import cn.iocoder.yudao.module.oa.dal.dataobject.auth.SysUserDO;

import cn.iocoder.yudao.module.oa.dal.dataobject.author.AuthorUserDO;

import cn.iocoder.yudao.module.oa.dal.dataobject.author.OaAuthorExtDO;

import cn.iocoder.yudao.module.oa.dal.dataobject.ipgroup.IpGroupAnchorRelDO;
import cn.iocoder.yudao.module.oa.dal.dataobject.ipgroup.IpGroupDO;

import cn.iocoder.yudao.module.oa.dal.dataobject.operations.ContentDO;

import cn.iocoder.yudao.module.oa.dal.dataobject.operations.FollowerDailyDO;

import cn.iocoder.yudao.module.oa.dal.dataobject.operations.OpsAnchorRelDO;

import cn.iocoder.yudao.module.oa.dal.mysql.account.MpAccountMapper;

import cn.iocoder.yudao.module.oa.dal.mysql.auth.SysUserMapper;

import cn.iocoder.yudao.module.oa.dal.mysql.author.OaAuthorExtMapper;

import cn.iocoder.yudao.module.oa.dal.mysql.ipgroup.IpGroupAnchorRelMapper;
import cn.iocoder.yudao.module.oa.dal.mysql.ipgroup.IpGroupMapper;

import cn.iocoder.yudao.module.oa.dal.mysql.operations.ContentMapper;

import cn.iocoder.yudao.module.oa.dal.mysql.operations.FollowerDailyMapper;

import cn.iocoder.yudao.module.oa.dal.mysql.operations.OpsAnchorRelMapper;

import com.mzt.logapi.context.LogRecordContext;
import com.mzt.logapi.service.impl.DiffParseFunction;
import com.mzt.logapi.starter.annotation.LogRecord;

import static cn.iocoder.yudao.module.oa.framework.operatelog.OaLogRecordConstants.*;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;



import java.time.LocalDate;

import java.time.LocalDateTime;

import java.util.ArrayList;

import java.util.Comparator;

import java.util.LinkedHashSet;

import java.util.Collections;

import java.util.List;

import java.util.Map;

import java.util.Objects;

import java.util.Set;

import java.util.stream.Collectors;



/**

 * 作者服务（S1 / ADR-051）：member {@code author_user} SSOT + wd {@code oa_author_ext} 扩展。

 */

@Service

@RequiredArgsConstructor

public class AuthorServiceImpl implements AuthorService {



    private static final String SYNC_SYNCED = "SYNCED";

    private static final String SYNC_ERROR = "ERROR";



    private final OaAuthorExtMapper oaAuthorExtMapper;

    private final MemberAuthorReadService memberAuthorReadService;

    private final IpGroupMapper ipGroupMapper;

    private final IpGroupAnchorRelMapper ipGroupAnchorRelMapper;

    private final MpAccountMapper mpAccountMapper;

    private final SysUserMapper sysUserMapper;

    private final OpsAnchorRelMapper opsAnchorRelMapper;

    private final FollowerDailyMapper followerDailyMapper;

    private final ContentMapper contentMapper;

    private final AuthorResolveSupport authorResolveSupport;



    @Override

    public PageResult<AuthorVO> list(Long ipGroupId, String keyword, Integer status, Integer pageNo, Integer pageSize) {

        Long tenantId = requireTenantId();

        int pn = pageNo == null ? 1 : pageNo;

        int ps = pageSize == null ? 20 : pageSize;

        Set<Long> scopedUserIds = null;

        if (ipGroupId != null) {

            List<IpGroupAnchorRelDO> scoped = ipGroupAnchorRelMapper.selectList(new LambdaQueryWrapper<IpGroupAnchorRelDO>()

                    .eq(IpGroupAnchorRelDO::getTenantId, tenantId)

                    .eq(IpGroupAnchorRelDO::getIpGroupId, ipGroupId)

                    .orderByAsc(IpGroupAnchorRelDO::getId));

            scopedUserIds = scoped.stream().map(IpGroupAnchorRelDO::getAnchorUserId).collect(Collectors.toSet());

            if (scopedUserIds.isEmpty()) {

                return new PageResult<>(Collections.emptyList(), 0L);

            }

        }



        List<AuthorUserDO> candidates;

        if (StrUtil.isNotBlank(keyword)) {

            candidates = memberAuthorReadService.listByKeyword(keyword, tenantId);

            if (scopedUserIds != null) {

                Set<Long> scope = scopedUserIds;

                candidates = candidates.stream()

                        .filter(u -> scope.contains(u.getId()))

                        .collect(Collectors.toList());

            }

        } else if (scopedUserIds != null) {

            candidates = new ArrayList<>(memberAuthorReadService.loadByIds(scopedUserIds).values());

            candidates = candidates.stream()

                    .filter(u -> Objects.equals(u.getTenantId(), tenantId))

                    .collect(Collectors.toList());

        } else {

            candidates = loadTenantAuthorCandidates(tenantId);

        }



        if (status != null) {

            int memberStatus = toMemberStatus(status);

            candidates = candidates.stream()

                    .filter(u -> Objects.equals(u.getStatus(), memberStatus))

                    .collect(Collectors.toCollection(ArrayList::new));

        } else {

            candidates = new ArrayList<>(candidates);

        }



        candidates.sort(Comparator.comparing(AuthorUserDO::getId, Comparator.nullsLast(Comparator.naturalOrder())).reversed());

        long total = candidates.size();

        int fromIndex = Math.max(0, (pn - 1) * ps);

        if (fromIndex >= total) {

            return new PageResult<>(Collections.emptyList(), total);

        }

        int toIndex = Math.min(fromIndex + ps, candidates.size());

        List<AuthorUserDO> pageRecords = candidates.subList(fromIndex, toIndex);



        List<Long> userIds = pageRecords.stream().map(AuthorUserDO::getId).toList();

        Map<Long, OaAuthorExtDO> extMap = loadExtMap(tenantId, userIds);

        for (Long userId : userIds) {

            if (!extMap.containsKey(userId)) {

                extMap.put(userId, ensureExtRow(userId, tenantId));

            }

        }

        Map<Long, Long> displayIpGroupMap = loadDisplayIpGroupMap(tenantId, userIds, ipGroupId);

        List<AuthorVO> list = pageRecords.stream()

                .map(u -> toVO(u, extMap.get(u.getId()), displayIpGroupMap.get(u.getId())))

                .collect(Collectors.toList());

        return new PageResult<>(list, total);

    }



    @Override

    @LogRecord(type = M1_AUTHOR_TYPE, subType = M1_AUTHOR_CREATE_SUB_TYPE, bizNo = BIZ_NO_NONE,
            success = M1_AUTHOR_CREATE_SUCCESS)
    public Long create(AuthorCreateReq req) {

        throw new ServiceException(OaErrorCodes.AUTHOR_CRUD_DEPRECATED);

    }



    @Override

    @LogRecord(type = M1_AUTHOR_TYPE, subType = M1_AUTHOR_UPDATE_SUB_TYPE, bizNo = BIZ_NO_NONE,
            success = M1_AUTHOR_UPDATE_SUCCESS)
    public void update(AuthorUpdateReq req) {

        throw new ServiceException(OaErrorCodes.AUTHOR_CRUD_DEPRECATED);

    }



    @Override

    @LogRecord(type = M1_AUTHOR_TYPE, subType = M1_AUTHOR_DELETE_SUB_TYPE, bizNo = "{{#id}}",
            success = M1_AUTHOR_DELETE_SUCCESS)
    public void delete(Long id) {

        throw new ServiceException(OaErrorCodes.AUTHOR_CRUD_DEPRECATED);

    }



    @Override

    public AuthorExtVO getExt(Long authorUserId) {

        Long tenantId = requireTenantId();

        AuthorUserDO user = authorResolveSupport.requireAuthorUser(authorUserId, tenantId);

        OaAuthorExtDO ext = ensureExtRow(authorUserId, tenantId);

        return toExtVO(user, ext, authorResolveSupport.resolveDisplayIpGroupId(authorUserId, tenantId));

    }



    @Override

    @LogRecord(type = M1_AUTHOR_TYPE, subType = M1_AUTHOR_UPDATE_EXT_SUB_TYPE, bizNo = "{{#authorUserId}}",
            success = M1_AUTHOR_UPDATE_EXT_SUCCESS)
    public void updateExt(Long authorUserId, AuthorExtUpdateReq req) {

        Long tenantId = requireTenantId();

        AuthorUserDO author = authorResolveSupport.requireAuthorUser(authorUserId, tenantId);
        LogRecordContext.putVariable("author", author);

        OaAuthorExtDO ext = ensureExtRow(authorUserId, tenantId);
        LogRecordContext.putVariable(DiffParseFunction.OLD_OBJECT, toExtUpdateReq(ext));

        if (req.getIpGroupId() != null) {

            throw new ServiceException(OaErrorCodes.AUTHOR_IP_GROUP_MANAGED_IN_IP_GROUP);

        }

        if (req.getAuthorType() != null) {

            ext.setAuthorType(req.getAuthorType());

        }

        if (req.getPrimaryAccountId() != null) {

            validatePrimaryMpAccount(tenantId, req.getPrimaryAccountId(), authorUserId);

            ext.setPrimaryMpAccountId(req.getPrimaryAccountId());

        }

        if (req.getStatus() != null) {

            ext.setStatus(req.getStatus());

        }

        if (req.getRemark() != null) {

            ext.setRemark(req.getRemark());

        }

        ext.setUpdater(TenantContextHolder.getUsername());

        ext.setUpdateTime(LocalDateTime.now());

        ext.setSyncStatus(SYNC_SYNCED);

        ext.setSyncError(null);

        oaAuthorExtMapper.updateById(ext);

    }



    @Override

    public AuthorDashboardVO dashboard(Long id) {

        Long tenantId = requireTenantId();

        AuthorUserDO user = authorResolveSupport.requireAuthorUser(id, tenantId);

        OaAuthorExtDO ext = ensureExtRow(id, tenantId);



        AuthorDashboardVO vo = new AuthorDashboardVO();

        vo.setAuthorId(id);

        vo.setAuthorName(user.getNickname());



        Long displayIpGroupId = authorResolveSupport.resolveDisplayIpGroupId(id, tenantId);

        if (displayIpGroupId != null) {

            IpGroupDO group = ipGroupMapper.selectById(displayIpGroupId);

            if (group != null) {

                String parentName = null;

                if (group.getParentId() != null) {

                    IpGroupDO parent = ipGroupMapper.selectById(group.getParentId());

                    if (parent != null) {

                        parentName = parent.getGroupName();

                    }

                }

                vo.setIpGroupName(parentName == null ? group.getGroupName() : parentName + "/" + group.getGroupName());

            }

        }



        Long accountId = ext != null ? ext.getPrimaryMpAccountId() : null;

        if (accountId != null) {

            FollowerDailyDO latestFollower = followerDailyMapper.selectOne(

                    new LambdaQueryWrapper<FollowerDailyDO>()

                            .eq(FollowerDailyDO::getTenantId, tenantId)

                            .eq(FollowerDailyDO::getAccountId, accountId)

                            .orderByDesc(FollowerDailyDO::getStatDate)

                            .last("LIMIT 1"));

            long totalFollowers = latestFollower != null && latestFollower.getFollowerCount() != null

                    ? latestFollower.getFollowerCount() : 0L;

            vo.setFollowerCount(totalFollowers);



            List<FollowerDailyDO> trend30 = followerDailyMapper.selectList(

                    new LambdaQueryWrapper<FollowerDailyDO>()

                            .eq(FollowerDailyDO::getTenantId, tenantId)

                            .eq(FollowerDailyDO::getAccountId, accountId)

                            .ge(FollowerDailyDO::getStatDate, LocalDate.now().minusDays(30))

                            .orderByAsc(FollowerDailyDO::getStatDate));

            List<AuthorDashboardVO.FollowerTrendPoint> trendList = new ArrayList<>();

            for (FollowerDailyDO d : trend30) {

                trendList.add(new AuthorDashboardVO.FollowerTrendPoint(d.getStatDate(), d.getFollowerCount()));

            }

            vo.setFollowerTrend(trendList);



            long contentTotal = contentMapper.selectCount(

                    new LambdaQueryWrapper<ContentDO>()

                            .eq(ContentDO::getTenantId, tenantId)

                            .eq(ContentDO::getAccountId, accountId));

            long contentHit = contentMapper.selectCount(

                    new LambdaQueryWrapper<ContentDO>()

                            .eq(ContentDO::getTenantId, tenantId)

                            .eq(ContentDO::getAccountId, accountId)

                            .eq(ContentDO::getIsHit, 1));

            vo.getContentStats().setTotalCount((int) contentTotal);

            vo.getContentStats().setHitCount((int) contentHit);

        }

        return vo;

    }



    @Override

    public List<OpsUserVO> opsList(Long id) {

        Long tenantId = requireTenantId();

        AuthorUserDO user = authorResolveSupport.requireAuthorUser(id, tenantId);

        if (user.getUserId() == null) {

            return Collections.emptyList();

        }

        List<OpsAnchorRelDO> rels = opsAnchorRelMapper.selectList(new LambdaQueryWrapper<OpsAnchorRelDO>()

                .eq(OpsAnchorRelDO::getTenantId, tenantId)

                .eq(OpsAnchorRelDO::getAnchorUserId, user.getUserId())

                .orderByDesc(OpsAnchorRelDO::getId));

        if (rels.isEmpty()) {

            return Collections.emptyList();

        }

        Set<Long> opsIds = rels.stream().map(OpsAnchorRelDO::getOpsUserId).collect(Collectors.toSet());

        Map<Long, SysUserDO> userMap = sysUserMapper.selectList(new LambdaQueryWrapper<SysUserDO>()

                        .in(SysUserDO::getId, opsIds))

                .stream()

                .collect(Collectors.toMap(SysUserDO::getId, u -> u, (a, b) -> a));

        return rels.stream().map(rel -> {

            OpsUserVO vo = new OpsUserVO();

            vo.setOpsUserId(rel.getOpsUserId());

            SysUserDO sysUser = userMap.get(rel.getOpsUserId());

            if (sysUser != null) {

                vo.setOpsUserName(sysUser.getNickname() != null ? sysUser.getNickname() : sysUser.getUsername());

            }

            vo.setIpGroupId(rel.getIpGroupId());

            vo.setStartDate(rel.getStartDate());

            vo.setEndDate(rel.getEndDate());

            return vo;

        }).collect(Collectors.toList());

    }



    private AuthorVO toVO(AuthorUserDO user, OaAuthorExtDO ext, Long displayIpGroupId) {

        AuthorVO vo = new AuthorVO();

        vo.setId(user.getId());

        vo.setAuthorUserId(user.getId());

        vo.setAuthorName(user.getNickname());

        vo.setNickname(user.getNickname());

        vo.setUserId(user.getUserId());

        vo.setAuthorLevel(user.getAuthorLevel());

        vo.setCreateTime(user.getCreateTime());

        vo.setStatus(ext != null && ext.getStatus() != null

                ? ext.getStatus() : fromMemberStatus(user.getStatus()));



        if (ext != null) {

            vo.setAuthorType(ext.getAuthorType());

            vo.setPrimaryAccountId(ext.getPrimaryMpAccountId());

            vo.setRemark(ext.getRemark());

            if (ext.getPrimaryMpAccountId() != null) {

                MpAccountDO mp = mpAccountMapper.selectById(ext.getPrimaryMpAccountId());

                if (mp != null) {

                    vo.setPrimaryAccountName(mp.getName());

                }

            }

        }

        if (displayIpGroupId != null) {

            vo.setIpGroupId(displayIpGroupId);

            IpGroupDO group = ipGroupMapper.selectById(displayIpGroupId);

            if (group != null) {

                vo.setIpGroupName(group.getGroupName());

                vo.setIpGroupLevel(group.getLevel());

            }

        }



        if (user.getUserId() != null) {

            SysUserDO sysUser = sysUserMapper.selectById(user.getUserId());

            if (sysUser != null) {

                vo.setUserName(sysUser.getNickname() != null ? sysUser.getNickname() : sysUser.getUsername());

            }

        }

        return vo;

    }



    private OaAuthorExtDO buildExt(Long authorUserId, Long tenantId, AuthorCreateReq req) {

        OaAuthorExtDO ext = new OaAuthorExtDO();

        ext.setAuthorUserId(authorUserId);

        ext.setTenantId(tenantId);

        ext.setIpGroupId(req.getIpGroupId());

        ext.setAuthorType(req.getAuthorType());

        ext.setPrimaryMpAccountId(req.getPrimaryAccountId());

        ext.setStatus(req.getStatus() == null ? 1 : req.getStatus());

        ext.setRemark(req.getRemark());

        ext.setSyncStatus(SYNC_SYNCED);

        ext.setCreator(TenantContextHolder.getUsername());

        ext.setUpdater(TenantContextHolder.getUsername());

        ext.setCreateTime(LocalDateTime.now());

        ext.setUpdateTime(LocalDateTime.now());

        return ext;

    }



    private void markExtError(Long authorUserId, Long tenantId, String message) {

        OaAuthorExtDO err = new OaAuthorExtDO();

        err.setAuthorUserId(authorUserId);

        err.setTenantId(tenantId);

        err.setIpGroupId(0L);

        err.setStatus(0);

        err.setSyncStatus(SYNC_ERROR);

        err.setSyncError(StrUtil.sub(message, 0, 512));

        err.setCreator(TenantContextHolder.getUsername());

        err.setUpdater(TenantContextHolder.getUsername());

        err.setCreateTime(LocalDateTime.now());

        err.setUpdateTime(LocalDateTime.now());

        try {

            oaAuthorExtMapper.insert(err);

        } catch (Exception ignored) {

            // best-effort error marker

        }

    }



    private Map<Long, OaAuthorExtDO> loadExtMap(Long tenantId, List<Long> userIds) {

        if (userIds.isEmpty()) {

            return Collections.emptyMap();

        }

        return oaAuthorExtMapper.selectList(new LambdaQueryWrapper<OaAuthorExtDO>()

                        .eq(OaAuthorExtDO::getTenantId, tenantId)

                        .in(OaAuthorExtDO::getAuthorUserId, userIds))

                .stream()

                .collect(Collectors.toMap(OaAuthorExtDO::getAuthorUserId, e -> e, (a, b) -> a));

    }



    /**
     * Lazy-create {@code oa_author_ext} when Football 新建作者尚未写入扩展行。
     */
    private OaAuthorExtDO ensureExtRow(Long authorUserId, Long tenantId) {

        OaAuthorExtDO ext = oaAuthorExtMapper.selectById(authorUserId);

        if (ext != null) {

            return ext;

        }

        ext = new OaAuthorExtDO();

        ext.setAuthorUserId(authorUserId);

        ext.setTenantId(tenantId);

        ext.setStatus(1);

        ext.setSyncStatus(SYNC_SYNCED);

        ext.setCreator(TenantContextHolder.getUsername());

        ext.setUpdater(TenantContextHolder.getUsername());

        ext.setCreateTime(LocalDateTime.now());

        ext.setUpdateTime(LocalDateTime.now());

        oaAuthorExtMapper.insert(ext);

        return ext;

    }



    private AuthorExtVO toExtVO(AuthorUserDO user, OaAuthorExtDO ext, Long displayIpGroupId) {

        AuthorExtVO vo = new AuthorExtVO();

        vo.setAuthorUserId(user.getId());

        vo.setAuthorName(user.getNickname());

        if (ext != null) {

            vo.setAuthorType(ext.getAuthorType());

            vo.setPrimaryAccountId(ext.getPrimaryMpAccountId());

            vo.setStatus(ext.getStatus());

            vo.setRemark(ext.getRemark());

            if (ext.getPrimaryMpAccountId() != null) {

                MpAccountDO mp = mpAccountMapper.selectById(ext.getPrimaryMpAccountId());

                if (mp != null) {

                    vo.setPrimaryAccountName(mp.getName());

                }

            }

        }

        if (displayIpGroupId != null) {

            vo.setIpGroupId(displayIpGroupId);

            IpGroupDO group = ipGroupMapper.selectById(displayIpGroupId);

            if (group != null) {

                vo.setIpGroupName(group.getGroupName());

            }

        }

        return vo;

    }



    private Map<Long, Long> loadDisplayIpGroupMap(Long tenantId, List<Long> authorUserIds, Long listFilterIpGroupId) {

        if (listFilterIpGroupId != null) {

            return authorUserIds.stream()

                    .collect(Collectors.toMap(id -> id, id -> listFilterIpGroupId, (a, b) -> a));

        }

        return authorResolveSupport.loadDisplayIpGroupIdByAuthor(tenantId, authorUserIds);

    }



    /**
     * 无 keyword / IP 组筛选时的作者候选集：anchor_rel ∪ oa_author_ext → Feign getAuthors。
     * Football 暂无 tenant 级 page RPC（G-MEM-02 缺口）；未绑定作者可能不在列表中。
     */
    private List<AuthorUserDO> loadTenantAuthorCandidates(Long tenantId) {

        Set<Long> authorUserIds = new LinkedHashSet<>();

        ipGroupAnchorRelMapper.selectList(new LambdaQueryWrapper<IpGroupAnchorRelDO>()

                        .eq(IpGroupAnchorRelDO::getTenantId, tenantId))

                .forEach(rel -> authorUserIds.add(rel.getAnchorUserId()));

        oaAuthorExtMapper.selectList(new LambdaQueryWrapper<OaAuthorExtDO>()

                        .eq(OaAuthorExtDO::getTenantId, tenantId))

                .forEach(ext -> authorUserIds.add(ext.getAuthorUserId()));

        if (authorUserIds.isEmpty()) {

            return Collections.emptyList();

        }

        return memberAuthorReadService.loadByIds(authorUserIds).values().stream()

                .filter(u -> Objects.equals(u.getTenantId(), tenantId))

                .sorted(Comparator.comparing(AuthorUserDO::getId, Comparator.nullsLast(Comparator.naturalOrder())).reversed())

                .collect(Collectors.toList());

    }



    private void validatePrimaryMpAccount(Long tenantId, Long mpAccountId, Long excludeAuthorUserId) {

        MpAccountDO mp = mpAccountMapper.selectById(mpAccountId);

        if (mp == null || !Objects.equals(mp.getTenantId(), tenantId)) {

            throw new ServiceException(OaErrorCodes.ENTITY_NOT_EXISTS);

        }

        LambdaQueryWrapper<OaAuthorExtDO> wrapper = new LambdaQueryWrapper<OaAuthorExtDO>()

                .eq(OaAuthorExtDO::getTenantId, tenantId)

                .eq(OaAuthorExtDO::getPrimaryMpAccountId, mpAccountId);

        if (excludeAuthorUserId != null) {

            wrapper.ne(OaAuthorExtDO::getAuthorUserId, excludeAuthorUserId);

        }

        if (oaAuthorExtMapper.selectCount(wrapper) > 0) {

            throw new ServiceException(OaErrorCodes.AUTHOR_PRIMARY_BOUND);

        }

    }



    private int toMemberStatus(int opsStatus) {

        return opsStatus == 1 ? 0 : 1;

    }



    private int fromMemberStatus(Integer memberStatus) {

        if (memberStatus == null || memberStatus == 0) {

            return 1;

        }

        return 0;

    }



    private Long requireTenantId() {

        Long tenantId = TenantContextHolder.getTenantId();

        if (tenantId == null) {

            throw new ServiceException(OaErrorCodes.UNAUTHORIZED.getCode(), "缺少租户上下文");

        }

        return tenantId;

    }

    private static AuthorExtUpdateReq toExtUpdateReq(OaAuthorExtDO ext) {
        AuthorExtUpdateReq req = new AuthorExtUpdateReq();
        req.setAuthorType(ext.getAuthorType());
        req.setPrimaryAccountId(ext.getPrimaryMpAccountId());
        req.setStatus(ext.getStatus());
        req.setRemark(ext.getRemark());
        return req;
    }

}


