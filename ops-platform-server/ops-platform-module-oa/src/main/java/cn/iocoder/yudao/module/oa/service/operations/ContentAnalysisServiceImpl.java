package cn.iocoder.yudao.module.oa.service.operations;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.exception.OaErrorCodes;
import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.oa.api.dto.operations.ContentAnalysisVO;
import cn.iocoder.yudao.module.oa.api.dto.operations.ContentStatsVO;
import cn.iocoder.yudao.module.oa.dal.dataobject.account.AccountDO;
import cn.iocoder.yudao.module.oa.dal.dataobject.ipgroup.IpGroupDO;
import cn.iocoder.yudao.module.oa.dal.dataobject.operations.ContentDO;
import cn.iocoder.yudao.module.oa.dal.mysql.account.AccountMapper;
import cn.iocoder.yudao.module.oa.dal.mysql.ipgroup.IpGroupMapper;
import cn.iocoder.yudao.module.oa.dal.mysql.operations.ContentMapper;
import cn.iocoder.yudao.module.oa.service.collect.display.CollectedDataMergeSupport;
import cn.iocoder.yudao.module.oa.service.collect.display.CollectedDataQueryService;
import cn.iocoder.yudao.module.oa.framework.auth.DataScopeSupport;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ContentAnalysisServiceImpl implements ContentAnalysisService {

    private final ContentMapper contentMapper;
    private final AccountMapper accountMapper;
    private final IpGroupMapper ipGroupMapper;
    private final CollectedDataQueryService collectedDataQueryService;

    @Override
    public PageResult<ContentAnalysisVO> list(LocalDate startDate, LocalDate endDate, Long ipGroupId,
                                              Long accountId, String platformType, String contentType,
                                              Boolean isHit, Integer page, Integer size) {
        Long tenantId = requireTenantId();
        Set<Long> accountIds = resolveAccountIds(tenantId, ipGroupId, accountId, platformType);
        if (accountIds.isEmpty()) {
            return PageResult.empty();
        }

        LambdaQueryWrapper<ContentDO> wrapper = buildContentWrapper(tenantId, accountIds, startDate, endDate,
                contentType, isHit, null);
        List<ContentAnalysisVO> legacy = toVOList(contentMapper.selectList(wrapper));
        PageResult<ContentAnalysisVO> collected = collectedDataQueryService.pageCollectedContents(
                tenantId, accountIds, platformType, contentType, null, startDate, endDate, 1, Integer.MAX_VALUE);
        PageResult<ContentAnalysisVO> merged = CollectedDataMergeSupport.mergeAndPage(
                legacy, collected.getList(), page, size);
        enrichContentAccountMeta(merged.getList());
        return merged;
    }

    private void enrichContentAccountMeta(List<ContentAnalysisVO> rows) {
        if (rows == null || rows.isEmpty()) {
            return;
        }
        Set<Long> missingMetaAccountIds = rows.stream()
                .filter(vo -> vo.getAccountName() == null && vo.getAccountId() != null)
                .map(ContentAnalysisVO::getAccountId)
                .collect(Collectors.toSet());
        if (missingMetaAccountIds.isEmpty()) {
            return;
        }
        Map<Long, AccountDO> accountMap = accountMapper.selectBatchIds(missingMetaAccountIds).stream()
                .collect(Collectors.toMap(AccountDO::getId, a -> a, (a, b) -> a));
        Set<Long> ipGroupIds = accountMap.values().stream()
                .map(AccountDO::getIpGroupId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<Long, String> ipGroupNameMap = ipGroupIds.isEmpty()
                ? Collections.emptyMap()
                : ipGroupMapper.selectBatchIds(ipGroupIds).stream()
                .collect(Collectors.toMap(IpGroupDO::getId, IpGroupDO::getGroupName, (a, b) -> a));
        for (ContentAnalysisVO vo : rows) {
            if (vo.getAccountName() != null || vo.getAccountId() == null) {
                continue;
            }
            AccountDO account = accountMap.get(vo.getAccountId());
            if (account == null) {
                continue;
            }
            vo.setAccountName(account.getAccountName());
            if (account.getIpGroupId() != null) {
                vo.setIpGroupName(ipGroupNameMap.get(account.getIpGroupId()));
            }
            if (StrUtil.isBlank(vo.getPlatformType())) {
                vo.setPlatformType(account.getPlatformType());
            }
        }
    }

    @Override
    public ContentStatsVO stats(LocalDate startDate, LocalDate endDate, Long ipGroupId, Long accountId,
                                String platformType, String contentType) {
        Long tenantId = requireTenantId();
        Set<Long> accountIds = resolveAccountIds(tenantId, ipGroupId, accountId, platformType);
        ContentStatsVO vo = new ContentStatsVO();
        if (accountIds.isEmpty()) {
            return vo;
        }
        LocalDateTime startDt = startDate != null ? startDate.atStartOfDay() : null;
        LocalDateTime endDt = endDate != null ? endDate.atTime(LocalTime.MAX) : null;
        Map<String, Object> row = contentMapper.sumStats(tenantId, accountIds, startDt, endDt,
                null, contentType);
        ContentStatsVO legacy = new ContentStatsVO();
        legacy.setTotalCount(toInt(row.get("totalCount")));
        legacy.setHitCount(toInt(row.get("hitCount")));
        legacy.setTotalRead(toLong(row.get("totalRead")));
        long totalRead = toLong(row.get("totalRead"));
        long totalCount = toLong(row.get("totalCount"));
        legacy.setAvgRead(totalCount == 0 ? 0L : totalRead / totalCount);
        legacy.setTotalLikes(toLong(row.get("totalLikes")));
        legacy.setTotalComments(toLong(row.get("totalComments")));
        legacy.setTotalShares(toLong(row.get("totalShares")));
        ContentStatsVO collected = collectedDataQueryService.aggregateStats(
                tenantId, accountIds, platformType, contentType, startDate, endDate);
        return CollectedDataMergeSupport.mergeStats(legacy, collected);
    }

    @Override
    public List<Map<String, Object>> trend(LocalDate startDate, LocalDate endDate, Long ipGroupId,
                                           Long accountId, Long contentId) {
        Long tenantId = requireTenantId();
        Set<Long> accountIds = resolveAccountIds(tenantId, ipGroupId, accountId, null);
        if (accountIds.isEmpty()) {
            return Collections.emptyList();
        }
        LambdaQueryWrapper<ContentDO> wrapper = buildContentWrapper(tenantId, accountIds, startDate, endDate,
                null, null, contentId);
        List<ContentDO> rows = contentMapper.selectList(wrapper);
        return rows.stream()
                .filter(c -> c.getPublishTime() != null)
                .collect(Collectors.groupingBy(c -> c.getPublishTime().toLocalDate().toString()))
                .entrySet().stream()
                .sorted(java.util.Map.Entry.comparingByKey())
                .map(e -> {
                    java.util.Map<String, Object> map = new java.util.HashMap<>();
                    map.put("date", e.getKey());
                    map.put("count", e.getValue().size());
                    map.put("readCount", e.getValue().stream().mapToLong(c -> c.getReadCount() == null ? 0L : c.getReadCount()).sum());
                    map.put("likeCount", e.getValue().stream().mapToInt(c -> c.getLikeCount() == null ? 0 : c.getLikeCount()).sum());
                    map.put("commentCount", e.getValue().stream().mapToInt(c -> c.getCommentCount() == null ? 0 : c.getCommentCount()).sum());
                    map.put("forwardCount", e.getValue().stream().mapToInt(c -> c.getForwardCount() == null ? 0 : c.getForwardCount()).sum());
                    return map;
                })
                .collect(Collectors.toList());
    }

    private LambdaQueryWrapper<ContentDO> buildContentWrapper(Long tenantId, Set<Long> accountIds,
                                                              LocalDate startDate, LocalDate endDate,
                                                              String contentType, Boolean isHit, Long contentId) {
        LocalDateTime start = startDate != null ? startDate.atStartOfDay() : null;
        LocalDateTime end = endDate != null ? endDate.atTime(LocalTime.MAX) : null;
        return new LambdaQueryWrapper<ContentDO>()
                .eq(ContentDO::getTenantId, tenantId)
                .in(ContentDO::getAccountId, accountIds)
                .eq(StrUtil.isNotBlank(contentType), ContentDO::getContentType, contentType)
                .eq(isHit != null, ContentDO::getIsHit, Boolean.TRUE.equals(isHit) ? 1 : 0)
                .eq(contentId != null, ContentDO::getId, contentId)
                .ge(start != null, ContentDO::getPublishTime, start)
                .le(end != null, ContentDO::getPublishTime, end)
                .orderByDesc(ContentDO::getPublishTime);
    }

    private static Integer toInt(Object v) {
        if (v == null) return 0;
        if (v instanceof Number n) return n.intValue();
        try { return Integer.parseInt(v.toString()); } catch (Exception e) { return 0; }
    }

    private static Long toLong(Object v) {
        if (v == null) return 0L;
        if (v instanceof Number n) return n.longValue();
        try { return Long.parseLong(v.toString()); } catch (Exception e) { return 0L; }
    }

    /**
     * 作品 → 账号 → IP 组：筛选与展示均经 account 关联，不在 content 上直接 join ip_group。
     */
    private Set<Long> resolveAccountIds(Long tenantId, Long ipGroupId, Long accountId, String platformType) {
        if (accountId != null) {
            AccountDO account = accountMapper.selectById(accountId);
            if (account == null || !Objects.equals(account.getTenantId(), tenantId)) {
                return Collections.emptySet();
            }
            if (ipGroupId != null && !Objects.equals(account.getIpGroupId(), ipGroupId)) {
                return Collections.emptySet();
            }
            if (StrUtil.isNotBlank(platformType) && !platformType.equals(account.getPlatformType())) {
                return Collections.emptySet();
            }
            return Set.of(accountId);
        }
        LambdaQueryWrapper<AccountDO> wrapper = new LambdaQueryWrapper<AccountDO>()
                .eq(AccountDO::getTenantId, tenantId)
                .eq(ipGroupId != null, AccountDO::getIpGroupId, ipGroupId)
                .eq(StrUtil.isNotBlank(platformType), AccountDO::getPlatformType, platformType);
        DataScopeSupport.applyIpGroupScope(wrapper, AccountDO::getIpGroupId);
        return accountMapper.selectList(wrapper).stream().map(AccountDO::getId).collect(Collectors.toSet());
    }

    private List<ContentAnalysisVO> toVOList(List<ContentDO> contents) {
        if (contents == null || contents.isEmpty()) {
            return Collections.emptyList();
        }
        Set<Long> accountIds = contents.stream()
                .map(ContentDO::getAccountId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<Long, AccountDO> accountMap = accountIds.isEmpty()
                ? Collections.emptyMap()
                : accountMapper.selectBatchIds(accountIds).stream()
                .collect(Collectors.toMap(AccountDO::getId, a -> a, (a, b) -> a));
        Set<Long> ipGroupIds = accountMap.values().stream()
                .map(AccountDO::getIpGroupId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<Long, String> ipGroupNameMap = ipGroupIds.isEmpty()
                ? Collections.emptyMap()
                : ipGroupMapper.selectBatchIds(ipGroupIds).stream()
                .collect(Collectors.toMap(IpGroupDO::getId, IpGroupDO::getGroupName, (a, b) -> a));
        return contents.stream()
                .map(c -> toVO(c, accountMap.get(c.getAccountId()), ipGroupNameMap))
                .collect(Collectors.toList());
    }

    private ContentAnalysisVO toVO(ContentDO content, AccountDO account, Map<Long, String> ipGroupNameMap) {
        ContentAnalysisVO vo = new ContentAnalysisVO();
        vo.setId(content.getId());
        vo.setAccountId(content.getAccountId());
        if (account != null) {
            vo.setAccountName(account.getAccountName());
            if (account.getIpGroupId() != null) {
                vo.setIpGroupName(ipGroupNameMap.get(account.getIpGroupId()));
            }
            vo.setPlatformType(StrUtil.isNotBlank(content.getPlatformType())
                    ? content.getPlatformType()
                    : account.getPlatformType());
        } else {
            vo.setPlatformType(content.getPlatformType());
        }
        vo.setTitle(content.getTitle());
        vo.setContentType(content.getContentType());
        vo.setPublishTime(content.getPublishTime());
        vo.setReadCount(content.getReadCount());
        vo.setLikeCount(content.getLikeCount());
        vo.setCommentCount(content.getCommentCount());
        vo.setForwardCount(content.getForwardCount());
        vo.setIsHit(content.getIsHit() != null && content.getIsHit() == 1);
        vo.setDataSource(content.getDataSource());
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

