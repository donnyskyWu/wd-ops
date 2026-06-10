package cn.iocoder.yudao.module.oa.service.operations;

import cn.iocoder.yudao.framework.common.exception.OaErrorCodes;
import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.oa.api.dto.operations.ContentAnalysisVO;
import cn.iocoder.yudao.module.oa.api.dto.operations.ContentStatsVO;
import cn.iocoder.yudao.module.oa.dal.dataobject.account.AccountDO;
import cn.iocoder.yudao.module.oa.dal.dataobject.operations.ContentDO;
import cn.iocoder.yudao.module.oa.dal.mysql.account.AccountMapper;
import cn.iocoder.yudao.module.oa.dal.mysql.operations.ContentMapper;
import cn.iocoder.yudao.module.oa.framework.auth.DataScopeSupport;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
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

    @Override
    public PageResult<ContentAnalysisVO> list(LocalDate startDate, LocalDate endDate, Long ipGroupId,
                                              Long accountId, String platformType, Boolean isHit,
                                              Integer page, Integer size) {
        Long tenantId = requireTenantId();
        Set<Long> accountIds = resolveAccountIds(tenantId, ipGroupId, accountId);
        if (accountIds.isEmpty()) {
            return PageResult.empty();
        }

        LambdaQueryWrapper<ContentDO> wrapper = buildContentWrapper(tenantId, accountIds, startDate, endDate, platformType, isHit, null);
        Page<ContentDO> p = contentMapper.selectPage(
                new Page<>(page == null ? 1 : page, size == null ? 20 : size), wrapper);
        return new PageResult<>(p.getRecords().stream().map(this::toVO).collect(Collectors.toList()), p.getTotal());
    }

    @Override
    public ContentStatsVO stats(LocalDate startDate, LocalDate endDate, Long ipGroupId, Long accountId) {
        Long tenantId = requireTenantId();
        Set<Long> accountIds = resolveAccountIds(tenantId, ipGroupId, accountId);
        ContentStatsVO vo = new ContentStatsVO();
        if (accountIds.isEmpty()) {
            return vo;
        }
        // S-R8 B1: 用 SQL 一次性聚合 5 KPI，避免前端 list.reduce 因分页导致数字跟着翻页变
        LocalDateTime startDt = startDate != null ? startDate.atStartOfDay() : null;
        LocalDateTime endDt = endDate != null ? endDate.atTime(LocalTime.MAX) : null;
        Map<String, Object> row = contentMapper.sumStats(tenantId, accountIds, startDt, endDt);
        vo.setTotalCount(toInt(row.get("totalCount")));
        vo.setHitCount(toInt(row.get("hitCount")));
        vo.setTotalRead(toLong(row.get("totalRead")));
        long totalRead = toLong(row.get("totalRead"));
        long totalCount = toLong(row.get("totalCount"));
        vo.setAvgRead(totalCount == 0 ? 0L : totalRead / totalCount);
        vo.setTotalLikes(toLong(row.get("totalLikes")));
        vo.setTotalComments(toLong(row.get("totalComments")));
        vo.setTotalShares(toLong(row.get("totalShares")));
        return vo;
    }

    @Override
    public List<Map<String, Object>> trend(LocalDate startDate, LocalDate endDate, Long ipGroupId,
                                           Long accountId, Long contentId) {
        Long tenantId = requireTenantId();
        Set<Long> accountIds = resolveAccountIds(tenantId, ipGroupId, accountId);
        if (accountIds.isEmpty()) {
            return Collections.emptyList();
        }
        // S-R8 B4: 详情场景按 contentId 过滤
        LambdaQueryWrapper<ContentDO> wrapper = buildContentWrapper(tenantId, accountIds, startDate, endDate, null, null, contentId);
        List<ContentDO> rows = contentMapper.selectList(wrapper);
        // 按 publishTime 日期分组聚合
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
                                                              String platformType, Boolean isHit,
                                                              Long contentId) {
        LocalDateTime start = startDate != null ? startDate.atStartOfDay() : null;
        LocalDateTime end = endDate != null ? endDate.atTime(LocalTime.MAX) : null;
        LambdaQueryWrapper<ContentDO> wrapper = new LambdaQueryWrapper<ContentDO>()
                .eq(ContentDO::getTenantId, tenantId)
                .in(ContentDO::getAccountId, accountIds)
                .eq(platformType != null, ContentDO::getPlatformType, platformType)
                .eq(isHit != null, ContentDO::getIsHit, Boolean.TRUE.equals(isHit) ? 1 : 0)
                .eq(contentId != null, ContentDO::getId, contentId)
                .ge(start != null, ContentDO::getPublishTime, start)
                .le(end != null, ContentDO::getPublishTime, end)
                .orderByDesc(ContentDO::getPublishTime);
        return wrapper;
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

    private Set<Long> resolveAccountIds(Long tenantId, Long ipGroupId, Long accountId) {
        if (accountId != null) {
            AccountDO account = accountMapper.selectById(accountId);
            if (account == null || !Objects.equals(account.getTenantId(), tenantId)) {
                return Collections.emptySet();
            }
            return Set.of(accountId);
        }
        LambdaQueryWrapper<AccountDO> wrapper = new LambdaQueryWrapper<AccountDO>()
                .eq(AccountDO::getTenantId, tenantId)
                .eq(ipGroupId != null, AccountDO::getIpGroupId, ipGroupId);
        DataScopeSupport.applyIpGroupScope(wrapper, AccountDO::getIpGroupId);
        return accountMapper.selectList(wrapper).stream().map(AccountDO::getId).collect(Collectors.toSet());
    }

    private ContentAnalysisVO toVO(ContentDO content) {
        ContentAnalysisVO vo = new ContentAnalysisVO();
        vo.setId(content.getId());
        vo.setAccountId(content.getAccountId());
        AccountDO account = accountMapper.selectById(content.getAccountId());
        if (account != null) {
            vo.setAccountName(account.getAccountName());
        }
        vo.setTitle(content.getTitle());
        vo.setPlatformType(content.getPlatformType());
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
