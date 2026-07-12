package cn.iocoder.yudao.module.oa.service.author;



import cn.hutool.core.util.StrUtil;

import cn.iocoder.yudao.framework.common.exception.OaErrorCodes;

import cn.iocoder.yudao.framework.common.exception.ServiceException;

import cn.iocoder.yudao.framework.common.pojo.PageResult;

import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;

import cn.iocoder.yudao.module.oa.api.dto.author.AuthorCreateReq;

import cn.iocoder.yudao.module.oa.api.dto.author.AuthorDashboardVO;

import cn.iocoder.yudao.module.oa.api.dto.author.AuthorUpdateReq;

import cn.iocoder.yudao.module.oa.api.dto.author.AuthorVO;

import cn.iocoder.yudao.module.oa.api.dto.author.OpsUserVO;

import cn.iocoder.yudao.module.oa.dal.dataobject.account.MpAccountDO;

import cn.iocoder.yudao.module.oa.dal.dataobject.auth.SysUserDO;

import cn.iocoder.yudao.module.oa.dal.dataobject.author.AuthorUserDO;

import cn.iocoder.yudao.module.oa.dal.dataobject.author.OaAuthorExtDO;

import cn.iocoder.yudao.module.oa.dal.dataobject.ipgroup.IpGroupDO;

import cn.iocoder.yudao.module.oa.dal.dataobject.operations.ContentDO;

import cn.iocoder.yudao.module.oa.dal.dataobject.operations.FollowerDailyDO;

import cn.iocoder.yudao.module.oa.dal.dataobject.operations.OpsAnchorRelDO;

import cn.iocoder.yudao.module.oa.dal.mysql.account.MpAccountMapper;

import cn.iocoder.yudao.module.oa.dal.mysql.auth.SysUserMapper;

import cn.iocoder.yudao.module.oa.dal.mysql.author.AuthorUserMapper;

import cn.iocoder.yudao.module.oa.dal.mysql.author.OaAuthorExtMapper;

import cn.iocoder.yudao.module.oa.dal.mysql.ipgroup.IpGroupMapper;

import cn.iocoder.yudao.module.oa.dal.mysql.operations.ContentMapper;

import cn.iocoder.yudao.module.oa.dal.mysql.operations.FollowerDailyMapper;

import cn.iocoder.yudao.module.oa.dal.mysql.operations.OpsAnchorRelMapper;

import cn.iocoder.yudao.module.oa.framework.audit.AuditLog;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;



import java.time.LocalDate;

import java.time.LocalDateTime;

import java.util.ArrayList;

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



    private final AuthorUserMapper authorUserMapper;

    private final OaAuthorExtMapper oaAuthorExtMapper;

    private final IpGroupMapper ipGroupMapper;

    private final MpAccountMapper mpAccountMapper;

    private final SysUserMapper sysUserMapper;

    private final OpsAnchorRelMapper opsAnchorRelMapper;

    private final FollowerDailyMapper followerDailyMapper;

    private final ContentMapper contentMapper;

    private final AuthorResolveSupport authorResolveSupport;



    @Override

    public PageResult<AuthorVO> list(Long ipGroupId, String keyword, Integer status, Integer pageNo, Integer pageSize) {

        Long tenantId = requireTenantId();

        Set<Long> scopedUserIds = null;

        if (ipGroupId != null) {

            List<OaAuthorExtDO> scoped = oaAuthorExtMapper.selectList(new LambdaQueryWrapper<OaAuthorExtDO>()

                    .eq(OaAuthorExtDO::getTenantId, tenantId)

                    .eq(OaAuthorExtDO::getIpGroupId, ipGroupId));

            scopedUserIds = scoped.stream().map(OaAuthorExtDO::getAuthorUserId).collect(Collectors.toSet());

            if (scopedUserIds.isEmpty()) {

                return new PageResult<>(Collections.emptyList(), 0L);

            }

        }



        LambdaQueryWrapper<AuthorUserDO> wrapper = new LambdaQueryWrapper<AuthorUserDO>()

                .eq(AuthorUserDO::getTenantId, tenantId)

                .like(StrUtil.isNotBlank(keyword), AuthorUserDO::getNickname, keyword)

                .in(scopedUserIds != null, AuthorUserDO::getId, scopedUserIds)

                .orderByDesc(AuthorUserDO::getId);

        if (status != null) {

            wrapper.eq(AuthorUserDO::getStatus, toMemberStatus(status));

        }



        Page<AuthorUserDO> page = authorUserMapper.selectPage(

                new Page<>(pageNo == null ? 1 : pageNo, pageSize == null ? 20 : pageSize), wrapper);



        List<Long> userIds = page.getRecords().stream().map(AuthorUserDO::getId).toList();

        Map<Long, OaAuthorExtDO> extMap = loadExtMap(tenantId, userIds);



        List<AuthorVO> list = page.getRecords().stream()

                .map(u -> toVO(u, extMap.get(u.getId())))

                .collect(Collectors.toList());

        return new PageResult<>(list, page.getTotal());

    }



    @Override

    @AuditLog(module = "M1-author", action = "create")

    public Long create(AuthorCreateReq req) {

        Long tenantId = requireTenantId();

        validateIpGroupSmall(tenantId, req.getIpGroupId());

        if (req.getPrimaryAccountId() != null) {

            validatePrimaryMpAccount(tenantId, req.getPrimaryAccountId(), null);

        }



        AuthorUserDO user = new AuthorUserDO();

        user.setTenantId(tenantId);

        user.setNickname(req.getAuthorName().trim());

        user.setUserId(req.getUserId());

        user.setStatus(toMemberStatus(req.getStatus() == null ? 1 : req.getStatus()));

        user.setCreator(TenantContextHolder.getUsername());

        user.setUpdater(TenantContextHolder.getUsername());

        user.setCreateTime(LocalDateTime.now());

        user.setUpdateTime(LocalDateTime.now());

        authorUserMapper.insert(user);



        try {

            oaAuthorExtMapper.insert(buildExt(user.getId(), tenantId, req));

        } catch (Exception ex) {

            markExtError(user.getId(), tenantId, ex.getMessage());

            throw new ServiceException(OaErrorCodes.BAD_REQUEST.getCode(), "作者扩展写入失败: " + ex.getMessage());

        }

        return user.getId();

    }



    @Override

    @AuditLog(module = "M1-author", action = "update")

    public void update(AuthorUpdateReq req) {

        Long tenantId = requireTenantId();

        AuthorUserDO user = authorResolveSupport.requireAuthorUser(req.getId(), tenantId);

        OaAuthorExtDO ext = oaAuthorExtMapper.selectById(req.getId());



        if (StrUtil.isNotBlank(req.getAuthorName())) {

            user.setNickname(req.getAuthorName().trim());

        }

        if (req.getUserId() != null) {

            user.setUserId(req.getUserId());

        }

        if (req.getStatus() != null) {

            user.setStatus(toMemberStatus(req.getStatus()));

        }

        user.setUpdater(TenantContextHolder.getUsername());

        user.setUpdateTime(LocalDateTime.now());

        authorUserMapper.updateById(user);



        if (ext == null) {

            ext = new OaAuthorExtDO();

            ext.setAuthorUserId(req.getId());

            ext.setTenantId(tenantId);

            ext.setSyncStatus(SYNC_SYNCED);

            ext.setCreator(TenantContextHolder.getUsername());

            ext.setCreateTime(LocalDateTime.now());

        }

        if (req.getIpGroupId() != null) {

            validateIpGroupSmall(tenantId, req.getIpGroupId());

            ext.setIpGroupId(req.getIpGroupId());

        }

        if (req.getAuthorType() != null) {

            ext.setAuthorType(req.getAuthorType());

        }

        if (req.getPrimaryAccountId() != null) {

            validatePrimaryMpAccount(tenantId, req.getPrimaryAccountId(), req.getId());

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

        if (oaAuthorExtMapper.selectById(ext.getAuthorUserId()) == null) {

            oaAuthorExtMapper.insert(ext);

        } else {

            oaAuthorExtMapper.updateById(ext);

        }

    }



    @Override

    @AuditLog(module = "M1-author", action = "delete")

    public void delete(Long id) {

        Long tenantId = requireTenantId();

        authorResolveSupport.requireAuthorUser(id, tenantId);

        OaAuthorExtDO ext = oaAuthorExtMapper.selectById(id);

        if (ext != null) {

            oaAuthorExtMapper.deleteById(id);

        }

    }



    @Override

    public AuthorDashboardVO dashboard(Long id) {

        Long tenantId = requireTenantId();

        AuthorUserDO user = authorResolveSupport.requireAuthorUser(id, tenantId);

        OaAuthorExtDO ext = oaAuthorExtMapper.selectById(id);



        AuthorDashboardVO vo = new AuthorDashboardVO();

        vo.setAuthorId(id);

        vo.setAuthorName(user.getNickname());



        if (ext != null && ext.getIpGroupId() != null) {

            IpGroupDO group = ipGroupMapper.selectById(ext.getIpGroupId());

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



    private AuthorVO toVO(AuthorUserDO user, OaAuthorExtDO ext) {

        AuthorVO vo = new AuthorVO();

        vo.setId(user.getId());

        vo.setAuthorUserId(user.getId());

        vo.setAuthorName(user.getNickname());

        vo.setNickname(user.getNickname());

        vo.setUserId(user.getUserId());

        vo.setCreateTime(user.getCreateTime());

        vo.setStatus(ext != null && ext.getStatus() != null

                ? ext.getStatus() : fromMemberStatus(user.getStatus()));



        if (ext != null) {

            vo.setIpGroupId(ext.getIpGroupId());

            vo.setAuthorType(ext.getAuthorType());

            vo.setPrimaryAccountId(ext.getPrimaryMpAccountId());

            vo.setRemark(ext.getRemark());

            if (ext.getIpGroupId() != null) {

                IpGroupDO group = ipGroupMapper.selectById(ext.getIpGroupId());

                if (group != null) {

                    vo.setIpGroupName(group.getGroupName());

                }

            }

            if (ext.getPrimaryMpAccountId() != null) {

                MpAccountDO mp = mpAccountMapper.selectById(ext.getPrimaryMpAccountId());

                if (mp != null) {

                    vo.setPrimaryAccountName(mp.getName());

                }

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



    private void validateIpGroupSmall(Long tenantId, Long ipGroupId) {

        IpGroupDO group = ipGroupMapper.selectById(ipGroupId);

        if (group == null || !Objects.equals(group.getTenantId(), tenantId)

                || group.getGroupType() == null || group.getGroupType() != 2) {

            throw new ServiceException(OaErrorCodes.AUTHOR_IP_GROUP_MUST_SMALL);

        }

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

}


