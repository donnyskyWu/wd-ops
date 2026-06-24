package cn.iocoder.yudao.module.oa.service.collect.display;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.oa.api.dto.operations.ContentAnalysisVO;
import cn.iocoder.yudao.module.oa.api.dto.operations.ContentStatsVO;
import cn.iocoder.yudao.module.oa.api.dto.operations.FollowerAnalysisVO;
import cn.iocoder.yudao.module.oa.api.dto.operations.InternalContentVO;
import cn.iocoder.yudao.module.oa.dal.dataobject.analytics.AccountStatusLogDO;
import cn.iocoder.yudao.module.oa.dal.dataobject.collect.DouyinFollowerDO;
import cn.iocoder.yudao.module.oa.dal.dataobject.collect.DouyinVideoDO;
import cn.iocoder.yudao.module.oa.dal.dataobject.collect.KuaishouVideoDO;
import cn.iocoder.yudao.module.oa.dal.dataobject.collect.WechatMpArticleDO;
import cn.iocoder.yudao.module.oa.dal.dataobject.collect.WechatMpFollowerDO;
import cn.iocoder.yudao.module.oa.dal.dataobject.collect.WechatVideoWorkDO;
import cn.iocoder.yudao.module.oa.dal.dataobject.collect.XiaohongshuNoteDO;
import cn.iocoder.yudao.module.oa.dal.mysql.analytics.AccountStatusLogMapper;
import cn.iocoder.yudao.module.oa.dal.mysql.collect.DouyinFollowerMapper;
import cn.iocoder.yudao.module.oa.dal.mysql.collect.DouyinVideoMapper;
import cn.iocoder.yudao.module.oa.dal.mysql.collect.KuaishouVideoMapper;
import cn.iocoder.yudao.module.oa.dal.mysql.collect.WechatMpArticleMapper;
import cn.iocoder.yudao.module.oa.dal.mysql.collect.WechatMpFollowerMapper;
import cn.iocoder.yudao.module.oa.dal.mysql.collect.WechatVideoWorkMapper;
import cn.iocoder.yudao.module.oa.dal.mysql.collect.XiaohongshuNoteMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.support.SFunction;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CollectedDataQueryServiceImpl implements CollectedDataQueryService {

    private static final String CONTENT_TYPE_ARTICLE = "ARTICLE";
    private static final String CONTENT_TYPE_SHORT_VIDEO = "SHORT_VIDEO";
    private static final String PLATFORM_WECHAT_OFFICIAL = "WECHAT_OFFICIAL";
    private static final String PLATFORM_WECHAT_VIDEO = "WECHAT_VIDEO";
    private static final String PLATFORM_DOUYIN = "DOUYIN";
    private static final String PLATFORM_KUAISHOU = "KUAISHOU";
    private static final String PLATFORM_XIAOHONGSHU = "XIAOHONGSHU";

    private final AccountStatusLogMapper accountStatusLogMapper;
    private final WechatMpArticleMapper wechatMpArticleMapper;
    private final WechatMpFollowerMapper wechatMpFollowerMapper;
    private final WechatVideoWorkMapper wechatVideoWorkMapper;
    private final DouyinVideoMapper douyinVideoMapper;
    private final DouyinFollowerMapper douyinFollowerMapper;
    private final KuaishouVideoMapper kuaishouVideoMapper;
    private final XiaohongshuNoteMapper xiaohongshuNoteMapper;

    @Override
    public boolean supportsPlatform(String platformType) {
        return StrUtil.isNotBlank(platformType) && COLLECTOR_PLATFORMS.contains(platformType);
    }

    @Override
    public Long latestFollowerCount(Long tenantId, Long accountId) {
        AccountStatusLogDO latest = accountStatusLogMapper.selectOne(new LambdaQueryWrapper<AccountStatusLogDO>()
                .eq(AccountStatusLogDO::getTenantId, tenantId)
                .eq(AccountStatusLogDO::getAccountId, accountId)
                .orderByDesc(AccountStatusLogDO::getStatDate)
                .last("LIMIT 1"));
        return latest != null && latest.getFollowerCount() != null ? latest.getFollowerCount() : null;
    }

    @Override
    public int workCount(Long tenantId, Long accountId, String platformType) {
        if (!supportsPlatform(platformType)) {
            return 0;
        }
        return switch (platformType) {
            case PLATFORM_WECHAT_OFFICIAL -> Math.toIntExact(wechatMpArticleMapper.selectCount(
                    accountEq(WechatMpArticleDO::getTenantId, WechatMpArticleDO::getAccountId, tenantId, accountId)));
            case PLATFORM_WECHAT_VIDEO -> Math.toIntExact(wechatVideoWorkMapper.selectCount(
                    accountEq(WechatVideoWorkDO::getTenantId, WechatVideoWorkDO::getAccountId, tenantId, accountId)));
            case PLATFORM_DOUYIN -> Math.toIntExact(douyinVideoMapper.selectCount(
                    accountEq(DouyinVideoDO::getTenantId, DouyinVideoDO::getAccountId, tenantId, accountId)));
            case PLATFORM_KUAISHOU -> Math.toIntExact(kuaishouVideoMapper.selectCount(
                    accountEq(KuaishouVideoDO::getTenantId, KuaishouVideoDO::getAccountId, tenantId, accountId)));
            case PLATFORM_XIAOHONGSHU -> Math.toIntExact(xiaohongshuNoteMapper.selectCount(
                    accountEq(XiaohongshuNoteDO::getTenantId, XiaohongshuNoteDO::getAccountId, tenantId, accountId)));
            default -> 0;
        };
    }

    @Override
    public List<FollowerAnalysisVO> listFollowerStats(Long tenantId, Long accountId, String platformType,
                                                      String accountName, String ipGroupName,
                                                      LocalDate startDate, LocalDate endDate) {
        LambdaQueryWrapper<AccountStatusLogDO> wrapper = new LambdaQueryWrapper<AccountStatusLogDO>()
                .eq(AccountStatusLogDO::getTenantId, tenantId)
                .eq(AccountStatusLogDO::getAccountId, accountId)
                .ge(startDate != null, AccountStatusLogDO::getStatDate, startDate)
                .le(endDate != null, AccountStatusLogDO::getStatDate, endDate)
                .orderByAsc(AccountStatusLogDO::getStatDate);
        List<AccountStatusLogDO> rows = accountStatusLogMapper.selectList(wrapper);
        if (!rows.isEmpty()) {
            return buildDailyFollowerStats(rows, accountId, accountName, ipGroupName);
        }
        if (PLATFORM_WECHAT_OFFICIAL.equals(platformType)) {
            return listIndividualFollowersFromMp(tenantId, accountId, accountName, ipGroupName);
        }
        if (PLATFORM_DOUYIN.equals(platformType)) {
            return listIndividualFollowersFromDouyin(tenantId, accountId, accountName, ipGroupName);
        }
        return Collections.emptyList();
    }

    @Override
    public PageResult<ContentAnalysisVO> pageCollectedContents(Long tenantId, Collection<Long> accountIds,
                                                               String platformType, String contentType,
                                                               String keyword, LocalDate startDate, LocalDate endDate,
                                                               Integer pageNo, Integer pageSize) {
        List<ContentAnalysisVO> all = loadCollectedContentRows(tenantId, accountIds, platformType,
                contentType, keyword, startDate, endDate);
        return paginate(all, pageNo, pageSize);
    }

    @Override
    public PageResult<InternalContentVO> pageInternalContents(Long tenantId, Collection<Long> accountIds,
                                                              String platformType, String contentType,
                                                              String keyword, LocalDate startDate, LocalDate endDate,
                                                              Integer pageNo, Integer pageSize) {
        List<ContentAnalysisVO> all = loadCollectedContentRows(tenantId, accountIds, platformType,
                contentType, keyword, startDate, endDate);
        List<InternalContentVO> internal = all.stream().map(this::toInternalVO).collect(Collectors.toList());
        return paginate(internal, pageNo, pageSize);
    }

    @Override
    public ContentStatsVO aggregateStats(Long tenantId, Collection<Long> accountIds, String platformType,
                                         String contentType, LocalDate startDate, LocalDate endDate) {
        List<ContentAnalysisVO> rows = loadCollectedContentRows(tenantId, accountIds, platformType,
                contentType, null, startDate, endDate);
        ContentStatsVO vo = new ContentStatsVO();
        vo.setTotalCount(rows.size());
        vo.setHitCount(0);
        long totalRead = rows.stream().mapToLong(r -> r.getReadCount() == null ? 0L : r.getReadCount()).sum();
        vo.setTotalRead(totalRead);
        vo.setAvgRead(rows.isEmpty() ? 0L : totalRead / rows.size());
        vo.setTotalLikes(rows.stream().mapToLong(r -> r.getLikeCount() == null ? 0L : r.getLikeCount()).sum());
        vo.setTotalComments(rows.stream().mapToLong(r -> r.getCommentCount() == null ? 0L : r.getCommentCount()).sum());
        vo.setTotalShares(rows.stream().mapToLong(r -> r.getForwardCount() == null ? 0L : r.getForwardCount()).sum());
        return vo;
    }

    @Override
    public List<Map<String, Object>> contentTrendByDay(Long tenantId, Long accountId, String platformType,
                                                       LocalDate startDate, LocalDate endDate) {
        List<ContentAnalysisVO> rows = loadCollectedContentRows(tenantId, Set.of(accountId), platformType,
                null, null, startDate, endDate);
        return rows.stream()
                .filter(r -> r.getPublishTime() != null)
                .collect(Collectors.groupingBy(r -> r.getPublishTime().toLocalDate()))
                .entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(entry -> {
                    Map<String, Object> point = new HashMap<>();
                    point.put("date", entry.getKey().toString());
                    point.put("workCount", entry.getValue().size());
                    point.put("value", entry.getValue().size());
                    return point;
                })
                .collect(Collectors.toList());
    }

    private List<ContentAnalysisVO> loadCollectedContentRows(Long tenantId, Collection<Long> accountIds,
                                                             String platformType, String contentType,
                                                             String keyword, LocalDate startDate, LocalDate endDate) {
        if (accountIds == null || accountIds.isEmpty()) {
            return Collections.emptyList();
        }
        Set<String> platforms = resolvePlatforms(platformType);
        List<ContentAnalysisVO> rows = new ArrayList<>();
        for (String platform : platforms) {
            if (!supportsPlatform(platform)) {
                continue;
            }
            rows.addAll(loadPlatformContents(tenantId, accountIds, platform, contentType, keyword, startDate, endDate));
        }
        rows.sort(Comparator.comparing(ContentAnalysisVO::getPublishTime,
                Comparator.nullsLast(Comparator.reverseOrder())));
        return rows;
    }

    private Set<String> resolvePlatforms(String platformType) {
        if (StrUtil.isBlank(platformType) || "ALL".equalsIgnoreCase(platformType)) {
            return COLLECTOR_PLATFORMS;
        }
        return Set.of(platformType);
    }

    private List<ContentAnalysisVO> loadPlatformContents(Long tenantId, Collection<Long> accountIds,
                                                         String platformType, String contentType,
                                                         String keyword, LocalDate startDate, LocalDate endDate) {
        if (StrUtil.isNotBlank(contentType)) {
            if (CONTENT_TYPE_ARTICLE.equals(contentType) && !PLATFORM_WECHAT_OFFICIAL.equals(platformType)) {
                return Collections.emptyList();
            }
            if (CONTENT_TYPE_SHORT_VIDEO.equals(contentType) && PLATFORM_WECHAT_OFFICIAL.equals(platformType)) {
                return Collections.emptyList();
            }
        }
        return switch (platformType) {
            case PLATFORM_WECHAT_OFFICIAL -> loadMpArticles(tenantId, accountIds, contentType, keyword, startDate, endDate);
            case PLATFORM_WECHAT_VIDEO -> loadWechatVideos(tenantId, accountIds, contentType, keyword, startDate, endDate);
            case PLATFORM_DOUYIN -> loadDouyinVideos(tenantId, accountIds, contentType, keyword, startDate, endDate);
            case PLATFORM_KUAISHOU -> loadKuaishouVideos(tenantId, accountIds, contentType, keyword, startDate, endDate);
            case PLATFORM_XIAOHONGSHU -> loadXhsNotes(tenantId, accountIds, contentType, keyword, startDate, endDate);
            default -> Collections.emptyList();
        };
    }

    private List<ContentAnalysisVO> loadMpArticles(Long tenantId, Collection<Long> accountIds, String contentType,
                                                   String keyword, LocalDate startDate, LocalDate endDate) {
        if (CONTENT_TYPE_SHORT_VIDEO.equals(contentType)) {
            return Collections.emptyList();
        }
        LambdaQueryWrapper<WechatMpArticleDO> wrapper = new LambdaQueryWrapper<WechatMpArticleDO>()
                .eq(WechatMpArticleDO::getTenantId, tenantId)
                .in(WechatMpArticleDO::getAccountId, accountIds)
                .like(StrUtil.isNotBlank(keyword), WechatMpArticleDO::getTitle, keyword)
                .orderByDesc(WechatMpArticleDO::getPublishedAt);
        if (startDate != null) {
            wrapper.ge(WechatMpArticleDO::getPublishedAt, startAt(startDate));
        }
        if (endDate != null) {
            wrapper.le(WechatMpArticleDO::getPublishedAt, endAt(endDate));
        }
        return wechatMpArticleMapper.selectList(wrapper).stream().map(this::toContentVO).collect(Collectors.toList());
    }

    private List<ContentAnalysisVO> loadWechatVideos(Long tenantId, Collection<Long> accountIds, String contentType,
                                                     String keyword, LocalDate startDate, LocalDate endDate) {
        if (CONTENT_TYPE_ARTICLE.equals(contentType)) {
            return Collections.emptyList();
        }
        LambdaQueryWrapper<WechatVideoWorkDO> wrapper = new LambdaQueryWrapper<WechatVideoWorkDO>()
                .eq(WechatVideoWorkDO::getTenantId, tenantId)
                .in(WechatVideoWorkDO::getAccountId, accountIds)
                .like(StrUtil.isNotBlank(keyword), WechatVideoWorkDO::getTitle, keyword)
                .orderByDesc(WechatVideoWorkDO::getPublishedAt);
        if (startDate != null) {
            wrapper.ge(WechatVideoWorkDO::getPublishedAt, startAt(startDate));
        }
        if (endDate != null) {
            wrapper.le(WechatVideoWorkDO::getPublishedAt, endAt(endDate));
        }
        return wechatVideoWorkMapper.selectList(wrapper).stream().map(this::toContentVO).collect(Collectors.toList());
    }

    private List<ContentAnalysisVO> loadDouyinVideos(Long tenantId, Collection<Long> accountIds, String contentType,
                                                     String keyword, LocalDate startDate, LocalDate endDate) {
        if (CONTENT_TYPE_ARTICLE.equals(contentType)) {
            return Collections.emptyList();
        }
        LambdaQueryWrapper<DouyinVideoDO> wrapper = new LambdaQueryWrapper<DouyinVideoDO>()
                .eq(DouyinVideoDO::getTenantId, tenantId)
                .in(DouyinVideoDO::getAccountId, accountIds)
                .like(StrUtil.isNotBlank(keyword), DouyinVideoDO::getTitle, keyword)
                .orderByDesc(DouyinVideoDO::getPublishedAt);
        if (startDate != null) {
            wrapper.ge(DouyinVideoDO::getPublishedAt, startAt(startDate));
        }
        if (endDate != null) {
            wrapper.le(DouyinVideoDO::getPublishedAt, endAt(endDate));
        }
        return douyinVideoMapper.selectList(wrapper).stream().map(this::toContentVO).collect(Collectors.toList());
    }

    private List<ContentAnalysisVO> loadKuaishouVideos(Long tenantId, Collection<Long> accountIds, String contentType,
                                                       String keyword, LocalDate startDate, LocalDate endDate) {
        if (CONTENT_TYPE_ARTICLE.equals(contentType)) {
            return Collections.emptyList();
        }
        LambdaQueryWrapper<KuaishouVideoDO> wrapper = new LambdaQueryWrapper<KuaishouVideoDO>()
                .eq(KuaishouVideoDO::getTenantId, tenantId)
                .in(KuaishouVideoDO::getAccountId, accountIds)
                .like(StrUtil.isNotBlank(keyword), KuaishouVideoDO::getTitle, keyword)
                .orderByDesc(KuaishouVideoDO::getPublishedAt);
        if (startDate != null) {
            wrapper.ge(KuaishouVideoDO::getPublishedAt, startAt(startDate));
        }
        if (endDate != null) {
            wrapper.le(KuaishouVideoDO::getPublishedAt, endAt(endDate));
        }
        return kuaishouVideoMapper.selectList(wrapper).stream().map(this::toContentVO).collect(Collectors.toList());
    }

    private List<ContentAnalysisVO> loadXhsNotes(Long tenantId, Collection<Long> accountIds, String contentType,
                                                 String keyword, LocalDate startDate, LocalDate endDate) {
        if (CONTENT_TYPE_ARTICLE.equals(contentType)) {
            return Collections.emptyList();
        }
        LambdaQueryWrapper<XiaohongshuNoteDO> wrapper = new LambdaQueryWrapper<XiaohongshuNoteDO>()
                .eq(XiaohongshuNoteDO::getTenantId, tenantId)
                .in(XiaohongshuNoteDO::getAccountId, accountIds)
                .like(StrUtil.isNotBlank(keyword), XiaohongshuNoteDO::getTitle, keyword)
                .orderByDesc(XiaohongshuNoteDO::getPublishedAt);
        if (startDate != null) {
            wrapper.ge(XiaohongshuNoteDO::getPublishedAt, startAt(startDate));
        }
        if (endDate != null) {
            wrapper.le(XiaohongshuNoteDO::getPublishedAt, endAt(endDate));
        }
        return xiaohongshuNoteMapper.selectList(wrapper).stream().map(this::toContentVO).collect(Collectors.toList());
    }

    private ContentAnalysisVO toContentVO(WechatMpArticleDO row) {
        ContentAnalysisVO vo = baseContentVO(row.getId(), row.getAccountId(), PLATFORM_WECHAT_OFFICIAL,
                CONTENT_TYPE_ARTICLE, row.getTitle(), row.getPublishedAt());
        vo.setReadCount(row.getReadCount() != null ? row.getReadCount().longValue() : 0L);
        vo.setLikeCount(row.getLikeCount());
        vo.setForwardCount(row.getShareCount());
        vo.setCommentCount(0);
        return vo;
    }

    private ContentAnalysisVO toContentVO(WechatVideoWorkDO row) {
        ContentAnalysisVO vo = baseContentVO(row.getId(), row.getAccountId(), PLATFORM_WECHAT_VIDEO,
                CONTENT_TYPE_SHORT_VIDEO, row.getTitle(), row.getPublishedAt());
        vo.setReadCount(row.getPlayCount() != null ? row.getPlayCount().longValue() : 0L);
        vo.setLikeCount(row.getLikeCount());
        vo.setCommentCount(row.getCommentCount());
        vo.setForwardCount(row.getShareCount());
        return vo;
    }

    private ContentAnalysisVO toContentVO(DouyinVideoDO row) {
        ContentAnalysisVO vo = baseContentVO(row.getId(), row.getAccountId(), PLATFORM_DOUYIN,
                CONTENT_TYPE_SHORT_VIDEO, row.getTitle(), row.getPublishedAt());
        vo.setReadCount(row.getPlayCount() != null ? row.getPlayCount().longValue() : 0L);
        vo.setLikeCount(row.getLikeCount());
        vo.setCommentCount(row.getCommentCount());
        vo.setForwardCount(row.getShareCount());
        return vo;
    }

    private ContentAnalysisVO toContentVO(KuaishouVideoDO row) {
        ContentAnalysisVO vo = baseContentVO(row.getId(), row.getAccountId(), PLATFORM_KUAISHOU,
                CONTENT_TYPE_SHORT_VIDEO, row.getTitle(), row.getPublishedAt());
        vo.setReadCount(row.getPlayCount() != null ? row.getPlayCount().longValue() : 0L);
        vo.setLikeCount(row.getLikeCount());
        vo.setCommentCount(row.getCommentCount());
        vo.setForwardCount(row.getShareCount());
        return vo;
    }

    private ContentAnalysisVO toContentVO(XiaohongshuNoteDO row) {
        ContentAnalysisVO vo = baseContentVO(row.getId(), row.getAccountId(), PLATFORM_XIAOHONGSHU,
                CONTENT_TYPE_SHORT_VIDEO, row.getTitle(), row.getPublishedAt());
        vo.setReadCount(row.getPlayCount() != null ? row.getPlayCount().longValue() : 0L);
        vo.setLikeCount(row.getLikeCount());
        vo.setCommentCount(row.getCommentCount());
        vo.setForwardCount(row.getShareCount());
        return vo;
    }

    private ContentAnalysisVO baseContentVO(Long id, Long accountId, String platformType, String contentType,
                                            String title, LocalDateTime publishTime) {
        ContentAnalysisVO vo = new ContentAnalysisVO();
        vo.setId(id);
        vo.setAccountId(accountId);
        vo.setPlatformType(platformType);
        vo.setContentType(contentType);
        vo.setTitle(title);
        vo.setPublishTime(publishTime);
        vo.setDataSource(DATA_SOURCE_COLLECT);
        vo.setIsHit(false);
        return vo;
    }

    private InternalContentVO toInternalVO(ContentAnalysisVO source) {
        InternalContentVO vo = new InternalContentVO();
        vo.setId(source.getId());
        vo.setAccountId(source.getAccountId());
        vo.setAccountName(source.getAccountName());
        vo.setTitle(source.getTitle());
        vo.setPlatformType(source.getPlatformType());
        vo.setContentType(source.getContentType());
        vo.setPublishTime(source.getPublishTime());
        vo.setReadCount(source.getReadCount());
        vo.setLikeCount(source.getLikeCount());
        vo.setDataSource(source.getDataSource());
        vo.setIsHit(Boolean.TRUE.equals(source.getIsHit()));
        return vo;
    }

    private List<FollowerAnalysisVO> buildDailyFollowerStats(List<AccountStatusLogDO> rows, Long accountId,
                                                             String accountName, String ipGroupName) {
        List<FollowerAnalysisVO> result = new ArrayList<>();
        Long previous = null;
        for (AccountStatusLogDO row : rows) {
            FollowerAnalysisVO vo = new FollowerAnalysisVO();
            vo.setStatDate(row.getStatDate());
            vo.setAccountId(accountId);
            vo.setAccountName(accountName);
            vo.setIpGroupName(ipGroupName);
            long current = row.getFollowerCount() != null ? row.getFollowerCount() : 0L;
            vo.setFollowerCount(current);
            if (previous == null) {
                vo.setNewFollower(0);
                vo.setUnfollowCount(0);
                vo.setNetGrowth(0);
                vo.setGrowthRate(BigDecimal.ZERO);
            } else {
                long delta = current - previous;
                vo.setNetGrowth((int) delta);
                vo.setNewFollower(delta > 0 ? (int) delta : 0);
                vo.setUnfollowCount(delta < 0 ? (int) -delta : 0);
                vo.setGrowthRate(previous == 0 ? BigDecimal.ZERO
                        : BigDecimal.valueOf(delta).divide(BigDecimal.valueOf(previous), 4, RoundingMode.HALF_UP));
            }
            result.add(vo);
            previous = current;
        }
        return result;
    }

    private List<FollowerAnalysisVO> listIndividualFollowersFromMp(Long tenantId, Long accountId,
                                                                   String accountName, String ipGroupName) {
        return wechatMpFollowerMapper.selectList(new LambdaQueryWrapper<WechatMpFollowerDO>()
                        .eq(WechatMpFollowerDO::getTenantId, tenantId)
                        .eq(WechatMpFollowerDO::getAccountId, accountId)
                        .orderByDesc(WechatMpFollowerDO::getSubscribedAt))
                .stream()
                .map(row -> individualFollowerRow(accountId, accountName, ipGroupName,
                        row.getSubscribedAt() != null ? row.getSubscribedAt().toLocalDate() : LocalDate.now()))
                .collect(Collectors.toList());
    }

    private List<FollowerAnalysisVO> listIndividualFollowersFromDouyin(Long tenantId, Long accountId,
                                                                       String accountName, String ipGroupName) {
        return douyinFollowerMapper.selectList(new LambdaQueryWrapper<DouyinFollowerDO>()
                        .eq(DouyinFollowerDO::getTenantId, tenantId)
                        .eq(DouyinFollowerDO::getAccountId, accountId)
                        .orderByDesc(DouyinFollowerDO::getFollowedAt))
                .stream()
                .map(row -> individualFollowerRow(accountId, accountName, ipGroupName,
                        row.getFollowedAt() != null ? row.getFollowedAt().toLocalDate() : LocalDate.now()))
                .collect(Collectors.toList());
    }

    private FollowerAnalysisVO individualFollowerRow(Long accountId, String accountName, String ipGroupName,
                                                     LocalDate statDate) {
        FollowerAnalysisVO vo = new FollowerAnalysisVO();
        vo.setAccountId(accountId);
        vo.setAccountName(accountName);
        vo.setIpGroupName(ipGroupName);
        vo.setStatDate(statDate);
        vo.setFollowerCount(1L);
        vo.setNewFollower(1);
        vo.setUnfollowCount(0);
        vo.setNetGrowth(1);
        vo.setGrowthRate(BigDecimal.ZERO);
        return vo;
    }

    private <T> LambdaQueryWrapper<T> accountEq(SFunction<T, Long> tenantFn, SFunction<T, Long> accountFn,
                                                Long tenantId, Long accountId) {
        return new LambdaQueryWrapper<T>().eq(tenantFn, tenantId).eq(accountFn, accountId);
    }

    private LocalDateTime startAt(LocalDate startDate) {
        return startDate.atStartOfDay();
    }

    private LocalDateTime endAt(LocalDate endDate) {
        return endDate.atTime(LocalTime.MAX);
    }

    private <T> PageResult<T> paginate(List<T> rows, Integer pageNo, Integer pageSize) {
        int page = pageNo == null || pageNo < 1 ? 1 : pageNo;
        int size = pageSize == null || pageSize < 1 ? 20 : pageSize;
        int from = Math.min((page - 1) * size, rows.size());
        int to = Math.min(from + size, rows.size());
        return new PageResult<>(rows.subList(from, to), (long) rows.size());
    }
}
