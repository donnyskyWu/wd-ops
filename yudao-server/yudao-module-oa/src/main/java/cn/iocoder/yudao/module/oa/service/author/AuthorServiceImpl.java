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
import cn.iocoder.yudao.module.oa.dal.dataobject.account.AccountDO;
import cn.iocoder.yudao.module.oa.dal.dataobject.auth.SysUserDO;
import cn.iocoder.yudao.module.oa.dal.dataobject.author.AuthorDO;
import cn.iocoder.yudao.module.oa.dal.dataobject.ipgroup.IpGroupDO;
import cn.iocoder.yudao.module.oa.dal.dataobject.operations.ContentDO;
import cn.iocoder.yudao.module.oa.dal.dataobject.operations.FollowerDailyDO;
import cn.iocoder.yudao.module.oa.dal.dataobject.operations.OpsAnchorRelDO;
import cn.iocoder.yudao.module.oa.dal.mysql.account.AccountMapper;
import cn.iocoder.yudao.module.oa.dal.mysql.auth.SysUserMapper;
import cn.iocoder.yudao.module.oa.dal.mysql.author.AuthorMapper;
import cn.iocoder.yudao.module.oa.dal.mysql.ipgroup.IpGroupMapper;
import cn.iocoder.yudao.module.oa.dal.mysql.operations.ContentMapper;
import cn.iocoder.yudao.module.oa.dal.mysql.operations.FollowerDailyMapper;
import cn.iocoder.yudao.module.oa.dal.mysql.operations.OpsAnchorRelMapper;
import cn.iocoder.yudao.module.oa.framework.audit.AuditLog;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AuthorServiceImpl implements AuthorService {

    private static final String OFFICIAL_ACCOUNT = "OFFICIAL_ACCOUNT";

    private final AuthorMapper authorMapper;
    private final IpGroupMapper ipGroupMapper;
    private final AccountMapper accountMapper;
    private final SysUserMapper sysUserMapper;
    private final OpsAnchorRelMapper opsAnchorRelMapper;
    private final FollowerDailyMapper followerDailyMapper;
    private final ContentMapper contentMapper;

    @Override
    public PageResult<AuthorVO> list(Long ipGroupId, String keyword, Integer status, Integer pageNo, Integer pageSize) {
        Long tenantId = requireTenantId();
        LambdaQueryWrapper<AuthorDO> wrapper = new LambdaQueryWrapper<AuthorDO>()
                .eq(AuthorDO::getTenantId, tenantId)
                .eq(ipGroupId != null, AuthorDO::getIpGroupId, ipGroupId)
                .like(StrUtil.isNotBlank(keyword), AuthorDO::getAuthorName, keyword)
                .eq(status != null, AuthorDO::getStatus, status)
                .orderByDesc(AuthorDO::getId);
        Page<AuthorDO> page = authorMapper.selectPage(
                new Page<>(pageNo == null ? 1 : pageNo, pageSize == null ? 20 : pageSize), wrapper);
        return new PageResult<>(page.getRecords().stream().map(this::toVO).collect(Collectors.toList()), page.getTotal());
    }

    @Override
    @Transactional
    @AuditLog(module = "M1-author", action = "create")
    public Long create(AuthorCreateReq req) {
        Long tenantId = requireTenantId();
        validateIpGroupSmall(tenantId, req.getIpGroupId());
        if (req.getPrimaryAccountId() != null) {
            validatePrimaryAccount(tenantId, req.getPrimaryAccountId(), null);
        }
        AuthorDO entity = new AuthorDO();
        entity.setTenantId(tenantId);
        entity.setAuthorName(req.getAuthorName().trim());
        entity.setIpGroupId(req.getIpGroupId());
        entity.setAuthorType(req.getAuthorType());
        entity.setPrimaryAccountId(req.getPrimaryAccountId());
        entity.setUserId(req.getUserId());
        entity.setStatus(req.getStatus() == null ? 1 : req.getStatus());
        entity.setRemark(req.getRemark());
        entity.setCreator(TenantContextHolder.getUsername());
        entity.setUpdater(TenantContextHolder.getUsername());
        entity.setCreateTime(LocalDateTime.now());
        entity.setUpdateTime(LocalDateTime.now());
        authorMapper.insert(entity);
        return entity.getId();
    }

    @Override
    @Transactional
    @AuditLog(module = "M1-author", action = "update")
    public void update(AuthorUpdateReq req) {
        AuthorDO existing = requireAuthor(req.getId());
        if (StrUtil.isNotBlank(req.getAuthorName())) {
            existing.setAuthorName(req.getAuthorName().trim());
        }
        if (req.getIpGroupId() != null) {
            validateIpGroupSmall(existing.getTenantId(), req.getIpGroupId());
            existing.setIpGroupId(req.getIpGroupId());
        }
        if (req.getAuthorType() != null) {
            existing.setAuthorType(req.getAuthorType());
        }
        if (req.getPrimaryAccountId() != null) {
            validatePrimaryAccount(existing.getTenantId(), req.getPrimaryAccountId(), existing.getId());
            existing.setPrimaryAccountId(req.getPrimaryAccountId());
        }
        if (req.getUserId() != null) {
            existing.setUserId(req.getUserId());
        }
        if (req.getStatus() != null) {
            existing.setStatus(req.getStatus());
        }
        if (req.getRemark() != null) {
            existing.setRemark(req.getRemark());
        }
        existing.setUpdater(TenantContextHolder.getUsername());
        existing.setUpdateTime(LocalDateTime.now());
        authorMapper.updateById(existing);
    }

    @Override
    @Transactional
    @AuditLog(module = "M1-author", action = "delete")
    public void delete(Long id) {
        requireAuthor(id);
        authorMapper.deleteById(id);
    }

    @Override
    public AuthorDashboardVO dashboard(Long id) {
        AuthorDO author = requireAuthor(id);
        AuthorDashboardVO vo = new AuthorDashboardVO();
        vo.setAuthorId(author.getId());
        vo.setAuthorName(author.getAuthorName());
        IpGroupDO group = ipGroupMapper.selectById(author.getIpGroupId());
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
        // S-R4 修复：作者 KPI 真实填充（spec §4.2.3 §5 "作者数据自动从 oa_follower_daily / oa_content 聚合"）
        // 限制：DB 中 oa_follower_daily / oa_content 仅按 accountId 关联，未直接关联 authorId。
        // 因此按 spec §4.2.5 "一个作者可关联一个主推号" 走 primaryAccountId 聚合（1:1）。
        // 若主推号为空：KPI 全 0、trend 空表（前端空状态）。
        Long accountId = author.getPrimaryAccountId();
        if (accountId != null) {
            // 1) 粉丝数：取最新一日 oa_follower_daily
            FollowerDailyDO latestFollower = followerDailyMapper.selectOne(
                    new LambdaQueryWrapper<FollowerDailyDO>()
                            .eq(FollowerDailyDO::getTenantId, author.getTenantId())
                            .eq(FollowerDailyDO::getAccountId, accountId)
                            .orderByDesc(FollowerDailyDO::getStatDate)
                            .last("LIMIT 1")
            );
            long totalFollowers = latestFollower != null && latestFollower.getFollowerCount() != null
                    ? latestFollower.getFollowerCount() : 0L;
            vo.setFollowerCount(totalFollowers);

            // 2) 30 日粉丝趋势（按 statDate 升序）
            List<FollowerDailyDO> trend30 = followerDailyMapper.selectList(
                    new LambdaQueryWrapper<FollowerDailyDO>()
                            .eq(FollowerDailyDO::getTenantId, author.getTenantId())
                            .eq(FollowerDailyDO::getAccountId, accountId)
                            .ge(FollowerDailyDO::getStatDate, LocalDate.now().minusDays(30))
                            .orderByAsc(FollowerDailyDO::getStatDate)
            );
            List<AuthorDashboardVO.FollowerTrendPoint> trendList = new ArrayList<>();
            for (FollowerDailyDO d : trend30) {
                trendList.add(new AuthorDashboardVO.FollowerTrendPoint(d.getStatDate(), d.getFollowerCount()));
            }
            vo.setFollowerTrend(trendList);

            // 3) 内容数：oa_content 中 accountId 计数
            long contentTotal = contentMapper.selectCount(
                    new LambdaQueryWrapper<ContentDO>()
                            .eq(ContentDO::getTenantId, author.getTenantId())
                            .eq(ContentDO::getAccountId, accountId)
            );
            long contentHit = contentMapper.selectCount(
                    new LambdaQueryWrapper<ContentDO>()
                            .eq(ContentDO::getTenantId, author.getTenantId())
                            .eq(ContentDO::getAccountId, accountId)
                            .eq(ContentDO::getIsHit, 1)
            );
            vo.getContentStats().setTotalCount((int) contentTotal);
            vo.getContentStats().setHitCount((int) contentHit);

            // 4) 直播 / ROI：DB schema 无 author↔live / author↔order 关联字段，留 0 + 标注 "spec gap"
            // TODO S-R5: 补 oa_live_session.authorId + oa_order_attribution.authorId 后再算
        }
        return vo;
    }

    @Override
    public List<OpsUserVO> opsList(Long id) {
        AuthorDO author = requireAuthor(id);
        if (author.getUserId() == null) {
            return Collections.emptyList();
        }
        List<OpsAnchorRelDO> rels = opsAnchorRelMapper.selectList(new LambdaQueryWrapper<OpsAnchorRelDO>()
                .eq(OpsAnchorRelDO::getTenantId, author.getTenantId())
                .eq(OpsAnchorRelDO::getAnchorUserId, author.getUserId())
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
            SysUserDO user = userMap.get(rel.getOpsUserId());
            if (user != null) {
                vo.setOpsUserName(user.getNickname() != null ? user.getNickname() : user.getUsername());
            }
            vo.setIpGroupId(rel.getIpGroupId());
            vo.setStartDate(rel.getStartDate());
            vo.setEndDate(rel.getEndDate());
            return vo;
        }).collect(Collectors.toList());
    }

    private AuthorVO toVO(AuthorDO entity) {
        AuthorVO vo = new AuthorVO();
        vo.setId(entity.getId());
        vo.setAuthorName(entity.getAuthorName());
        vo.setIpGroupId(entity.getIpGroupId());
        IpGroupDO group = ipGroupMapper.selectById(entity.getIpGroupId());
        if (group != null) {
            vo.setIpGroupName(group.getGroupName());
        }
        vo.setAuthorType(entity.getAuthorType());
        vo.setPrimaryAccountId(entity.getPrimaryAccountId());
        if (entity.getPrimaryAccountId() != null) {
            AccountDO account = accountMapper.selectById(entity.getPrimaryAccountId());
            if (account != null) {
                vo.setPrimaryAccountName(account.getAccountName());
            }
        }
        vo.setUserId(entity.getUserId());
        if (entity.getUserId() != null) {
            SysUserDO user = sysUserMapper.selectById(entity.getUserId());
            if (user != null) {
                vo.setUserName(user.getNickname() != null ? user.getNickname() : user.getUsername());
            }
        }
        vo.setStatus(entity.getStatus());
        vo.setRemark(entity.getRemark());
        vo.setCreateTime(entity.getCreateTime());
        return vo;
    }

    private void validateIpGroupSmall(Long tenantId, Long ipGroupId) {
        IpGroupDO group = ipGroupMapper.selectById(ipGroupId);
        if (group == null || !Objects.equals(group.getTenantId(), tenantId)
                || group.getGroupType() == null || group.getGroupType() != 2) {
            throw new ServiceException(OaErrorCodes.AUTHOR_IP_GROUP_MUST_SMALL);
        }
    }

    private void validatePrimaryAccount(Long tenantId, Long primaryAccountId, Long excludeAuthorId) {
        AccountDO account = accountMapper.selectById(primaryAccountId);
        if (account == null || !Objects.equals(account.getTenantId(), tenantId)) {
            throw new ServiceException(OaErrorCodes.ENTITY_NOT_EXISTS);
        }
        if (!OFFICIAL_ACCOUNT.equals(account.getAccountType())) {
            throw new ServiceException(OaErrorCodes.AUTHOR_PRIMARY_TYPE_INVALID);
        }
        LambdaQueryWrapper<AuthorDO> wrapper = new LambdaQueryWrapper<AuthorDO>()
                .eq(AuthorDO::getTenantId, tenantId)
                .eq(AuthorDO::getPrimaryAccountId, primaryAccountId);
        if (excludeAuthorId != null) {
            wrapper.ne(AuthorDO::getId, excludeAuthorId);
        }
        if (authorMapper.selectCount(wrapper) > 0) {
            throw new ServiceException(OaErrorCodes.AUTHOR_PRIMARY_BOUND);
        }
    }

    private AuthorDO requireAuthor(Long id) {
        AuthorDO entity = authorMapper.selectById(id);
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
