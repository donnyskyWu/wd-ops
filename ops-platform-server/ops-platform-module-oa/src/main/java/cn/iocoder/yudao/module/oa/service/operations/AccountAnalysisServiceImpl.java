package cn.iocoder.yudao.module.oa.service.operations;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.exception.OaErrorCodes;
import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.oa.api.dto.operations.AccountAnalysisVO;
import cn.iocoder.yudao.module.oa.api.dto.operations.ContentAnalysisVO;
import cn.iocoder.yudao.module.oa.api.dto.operations.FollowerAnalysisVO;
import cn.iocoder.yudao.module.oa.dal.dataobject.account.AccountDO;
import cn.iocoder.yudao.module.oa.dal.dataobject.ipgroup.IpGroupDO;
import cn.iocoder.yudao.module.oa.dal.dataobject.operations.ContentDO;
import cn.iocoder.yudao.module.oa.dal.dataobject.operations.FollowerDailyDO;
import cn.iocoder.yudao.module.oa.dal.mysql.account.AccountMapper;
import cn.iocoder.yudao.module.oa.dal.mysql.ipgroup.IpGroupMapper;
import cn.iocoder.yudao.module.oa.dal.mysql.operations.ContentMapper;
import cn.iocoder.yudao.module.oa.dal.mysql.operations.FollowerDailyMapper;
import cn.iocoder.yudao.module.oa.dal.dataobject.personal.PersonalWechatAccountDO;
import cn.iocoder.yudao.module.oa.dal.dataobject.personal.PersonalWechatDailyStatsDO;
import cn.iocoder.yudao.module.oa.dal.mysql.personal.PersonalWechatAccountMapper;
import cn.iocoder.yudao.module.oa.service.collect.display.CollectedDataMergeSupport;
import cn.iocoder.yudao.module.oa.service.collect.display.CollectedDataQueryService;
import cn.iocoder.yudao.module.oa.service.personal.PersonalWechatCollectStatusService;
import cn.iocoder.yudao.module.oa.service.personal.PersonalWechatDailyStatsService;
import cn.iocoder.yudao.module.oa.framework.auth.DataScopeSupport;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AccountAnalysisServiceImpl implements AccountAnalysisService {

    private static final String PLATFORM_PERSONAL_WECHAT = "WECHAT_PERSONAL";
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ISO_LOCAL_DATE;

    private final AccountMapper accountMapper;
    private final IpGroupMapper ipGroupMapper;
    private final FollowerDailyMapper followerDailyMapper;
    private final ContentMapper contentMapper;
    private final PersonalWechatAccountMapper personalWechatAccountMapper;
    private final PersonalWechatDailyStatsService personalWechatDailyStatsService;
    private final CollectedDataQueryService collectedDataQueryService;

    @Override
    public PageResult<AccountAnalysisVO> list(String platform, Long ipGroupId, String keyword,
                                              Integer pageNo, Integer pageSize) {
        return list(platform, ipGroupId, keyword, pageNo, pageSize, null);
    }

    @Override
    public PageResult<AccountAnalysisVO> list(String platform, Long ipGroupId, String keyword,
                                              Integer pageNo, Integer pageSize, LocalDate statDate) {
        if (PLATFORM_PERSONAL_WECHAT.equals(platform)) {
            return listPersonalWechat(keyword, pageNo, pageSize, statDate);
        }
        Long tenantId = requireTenantId();
        LambdaQueryWrapper<AccountDO> wrapper = new LambdaQueryWrapper<AccountDO>()
                .eq(AccountDO::getTenantId, tenantId)
                .eq(StrUtil.isNotBlank(platform) && !"ALL".equalsIgnoreCase(platform),
                        AccountDO::getPlatformType, platform)
                .eq(ipGroupId != null, AccountDO::getIpGroupId, ipGroupId)
                .like(StrUtil.isNotBlank(keyword), AccountDO::getAccountName, keyword)
                .orderByDesc(AccountDO::getId);
        DataScopeSupport.applyIpGroupScope(wrapper, AccountDO::getIpGroupId);

        Page<AccountDO> page = accountMapper.selectPage(
                new Page<>(pageNo == null ? 1 : pageNo, pageSize == null ? 20 : pageSize), wrapper);
        List<AccountAnalysisVO> list = page.getRecords().stream().map(this::toVO).collect(Collectors.toList());
        return new PageResult<>(list, page.getTotal());
    }

    private PageResult<AccountAnalysisVO> listPersonalWechat(String keyword, Integer pageNo,
                                                             Integer pageSize, LocalDate statDate) {
        Long tenantId = requireTenantId();
        LambdaQueryWrapper<PersonalWechatAccountDO> wrapper = new LambdaQueryWrapper<PersonalWechatAccountDO>()
                .eq(PersonalWechatAccountDO::getTenantId, tenantId)
                .and(StrUtil.isNotBlank(keyword), w -> w
                        .like(PersonalWechatAccountDO::getAccountName, keyword)
                        .or()
                        .like(PersonalWechatAccountDO::getWechatId, keyword))
                .orderByDesc(PersonalWechatAccountDO::getId);
        Page<PersonalWechatAccountDO> page = personalWechatAccountMapper.selectPage(
                new Page<>(pageNo == null ? 1 : pageNo, pageSize == null ? 20 : pageSize), wrapper);
        List<AccountAnalysisVO> list = page.getRecords().stream()
                .map(entity -> toPersonalWechatVO(entity, statDate))
                .collect(Collectors.toList());
        return new PageResult<>(list, page.getTotal());
    }

    private AccountAnalysisVO toPersonalWechatVO(PersonalWechatAccountDO entity, LocalDate statDate) {
        AccountAnalysisVO vo = new AccountAnalysisVO();
        vo.setAccountId(entity.getId());
        vo.setAccountName(entity.getAccountName());
        vo.setPlatformType(PLATFORM_PERSONAL_WECHAT);
        vo.setStatus(entity.getStatus());
        vo.setCollectStatus(StrUtil.blankToDefault(entity.getCollectStatus(),
                PersonalWechatCollectStatusService.PENDING));

        PersonalWechatDailyStatsDO stats = statDate != null
                ? personalWechatDailyStatsService.findStatsOnDate(entity.getId(), statDate)
                : personalWechatDailyStatsService.findLatestStats(entity.getId());
        if (stats == null && statDate != null) {
            stats = personalWechatDailyStatsService.findLatestStats(entity.getId());
        }
        if (stats != null) {
            vo.setFollowerCount(stats.getTotalFriends() != null ? stats.getTotalFriends().longValue() : 0L);
            vo.setMessagesSent(stats.getMessagesSent());
            vo.setMessagesReceived(stats.getMessagesReceived());
            vo.setContentCount(safeInt(stats.getMessagesSent()) + safeInt(stats.getMessagesReceived()));
            if (stats.getStatDate() != null) {
                vo.setStatDate(stats.getStatDate().format(DATE_FMT));
            }
        } else {
            vo.setFollowerCount(0L);
            vo.setMessagesSent(0);
            vo.setMessagesReceived(0);
            vo.setContentCount(0);
        }
        return vo;
    }

    private int safeInt(Integer value) {
        return value != null ? value : 0;
    }

    @Override
    public List<FollowerAnalysisVO> listAccountFollowers(Long accountId, LocalDate startDate, LocalDate endDate) {
        Long tenantId = requireTenantId();
        AccountDO account = accountMapper.selectById(accountId);
        if (account == null || !account.getTenantId().equals(tenantId)) {
            throw new ServiceException(OaErrorCodes.ENTITY_NOT_EXISTS.getCode(), "账号不存在");
        }
        String ipGroupName = null;
        if (account.getIpGroupId() != null) {
            IpGroupDO g = ipGroupMapper.selectById(account.getIpGroupId());
            if (g != null) ipGroupName = g.getGroupName();
        }
        final String finalIpGroupName = ipGroupName;
        LambdaQueryWrapper<FollowerDailyDO> wrapper = new LambdaQueryWrapper<FollowerDailyDO>()
                .eq(FollowerDailyDO::getTenantId, tenantId)
                .eq(FollowerDailyDO::getAccountId, accountId)
                .ge(startDate != null, FollowerDailyDO::getStatDate, startDate)
                .le(endDate != null, FollowerDailyDO::getStatDate, endDate)
                .orderByDesc(FollowerDailyDO::getStatDate);
        DataScopeSupport.applyIpGroupScope(wrapper, FollowerDailyDO::getAccountId);
        List<FollowerAnalysisVO> legacy = followerDailyMapper.selectList(wrapper).stream().map(d -> {
            FollowerAnalysisVO vo = new FollowerAnalysisVO();
            vo.setStatDate(d.getStatDate());
            vo.setAccountId(d.getAccountId());
            vo.setAccountName(account.getAccountName());
            vo.setIpGroupName(finalIpGroupName);
            vo.setFollowerCount(d.getFollowerCount());
            vo.setNewFollower(d.getNewFollower());
            vo.setUnfollowCount(d.getUnfollowCount());
            vo.setNetGrowth(d.getNetGrowth());
            vo.setGrowthRate(d.getGrowthRate());
            return vo;
        }).collect(Collectors.toList());
        if (!legacy.isEmpty() || !collectedDataQueryService.supportsPlatform(account.getPlatformType())) {
            return legacy;
        }
        return collectedDataQueryService.listFollowerStats(tenantId, accountId, account.getPlatformType(),
                account.getAccountName(), finalIpGroupName, startDate, endDate);
    }

    @Override
    public PageResult<ContentAnalysisVO> listAccountContents(Long accountId, Integer pageNo, Integer pageSize) {
        Long tenantId = requireTenantId();
        AccountDO account = accountMapper.selectById(accountId);
        if (account == null || !account.getTenantId().equals(tenantId)) {
            throw new ServiceException(OaErrorCodes.ENTITY_NOT_EXISTS.getCode(), "账号不存在");
        }
        LambdaQueryWrapper<ContentDO> wrapper = new LambdaQueryWrapper<ContentDO>()
                .eq(ContentDO::getTenantId, tenantId)
                .eq(ContentDO::getAccountId, accountId)
                .orderByDesc(ContentDO::getPublishTime);
        DataScopeSupport.applyIpGroupScope(wrapper, ContentDO::getAccountId);
        List<ContentAnalysisVO> legacy = contentMapper.selectList(wrapper).stream()
                .map(this::toContentVO).collect(Collectors.toList());
        if (!collectedDataQueryService.supportsPlatform(account.getPlatformType())) {
            return CollectedDataMergeSupport.page(legacy, pageNo, pageSize);
        }
        PageResult<ContentAnalysisVO> collected = collectedDataQueryService.pageCollectedContents(
                tenantId, List.of(accountId), account.getPlatformType(), null, null, null, null, 1, Integer.MAX_VALUE);
        return CollectedDataMergeSupport.mergeAndPage(legacy, collected.getList(), pageNo, pageSize);
    }

    @Override
    public List<Map<String, Object>> accountFollowerTrend(Long accountId, LocalDate startDate, LocalDate endDate) {
        LocalDate end = endDate != null ? endDate : LocalDate.now();
        LocalDate start = startDate != null ? startDate : end.minusDays(29);
        return listAccountFollowers(accountId, start, end).stream()
                .sorted((a, b) -> a.getStatDate().compareTo(b.getStatDate()))
                .map(row -> {
                    Map<String, Object> point = new HashMap<>();
                    point.put("date", row.getStatDate().toString());
                    point.put("followerCount", row.getFollowerCount());
                    point.put("value", row.getFollowerCount());
                    return point;
                })
                .collect(Collectors.toList());
    }

    @Override
    public List<Map<String, Object>> accountContentTrend(Long accountId, LocalDate startDate, LocalDate endDate) {
        Long tenantId = requireTenantId();
        AccountDO account = accountMapper.selectById(accountId);
        if (account == null || !account.getTenantId().equals(tenantId)) {
            throw new ServiceException(OaErrorCodes.ENTITY_NOT_EXISTS.getCode(), "账号不存在");
        }
        LocalDate end = endDate != null ? endDate : LocalDate.now();
        LocalDate start = startDate != null ? startDate : end.minusDays(29);
        LambdaQueryWrapper<ContentDO> wrapper = new LambdaQueryWrapper<ContentDO>()
                .eq(ContentDO::getTenantId, tenantId)
                .eq(ContentDO::getAccountId, accountId)
                .ge(ContentDO::getPublishTime, start.atStartOfDay())
                .le(ContentDO::getPublishTime, end.plusDays(1).atStartOfDay());
        List<Map<String, Object>> legacy = contentMapper.selectList(wrapper).stream()
                .filter(c -> c.getPublishTime() != null)
                .collect(Collectors.groupingBy(c -> c.getPublishTime().toLocalDate()))
                .entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(e -> {
                    Map<String, Object> point = new HashMap<>();
                    point.put("date", e.getKey().toString());
                    point.put("workCount", e.getValue().size());
                    point.put("value", e.getValue().size());
                    return point;
                })
                .collect(Collectors.toList());
        if (!legacy.isEmpty() || !collectedDataQueryService.supportsPlatform(account.getPlatformType())) {
            return legacy;
        }
        return collectedDataQueryService.contentTrendByDay(tenantId, accountId, account.getPlatformType(), start, end);
    }

    private ContentAnalysisVO toContentVO(ContentDO c) {
        ContentAnalysisVO vo = new ContentAnalysisVO();
        vo.setId(c.getId());
        vo.setAccountId(c.getAccountId());
        vo.setAccountName(null); // 列表已用 accountName 上下文
        vo.setTitle(c.getTitle());
        vo.setPlatformType(c.getPlatformType());
        vo.setContentType(c.getContentType());
        vo.setPublishTime(c.getPublishTime());
        vo.setReadCount(c.getReadCount());
        vo.setLikeCount(c.getLikeCount());
        vo.setCommentCount(c.getCommentCount());
        vo.setForwardCount(c.getForwardCount());
        vo.setIsHit(c.getIsHit() != null && c.getIsHit() == 1);
        vo.setDataSource(c.getDataSource());
        return vo;
    }

    private AccountAnalysisVO toVO(AccountDO account) {
        AccountAnalysisVO vo = new AccountAnalysisVO();
        vo.setAccountId(account.getId());
        vo.setAccountName(account.getAccountName());
        vo.setPlatformType(account.getPlatformType());
        vo.setIpGroupId(account.getIpGroupId());
        if (account.getIpGroupId() != null) {
            IpGroupDO group = ipGroupMapper.selectById(account.getIpGroupId());
            if (group != null) {
                vo.setIpGroupName(group.getGroupName());
            }
        }
        vo.setAccountType(account.getAccountType());
        vo.setStatus(account.getStatus());

        FollowerDailyDO latest = followerDailyMapper.selectOne(new LambdaQueryWrapper<FollowerDailyDO>()
                .eq(FollowerDailyDO::getTenantId, account.getTenantId())
                .eq(FollowerDailyDO::getAccountId, account.getId())
                .orderByDesc(FollowerDailyDO::getStatDate)
                .last("LIMIT 1"));
        long followerCount = latest != null && latest.getFollowerCount() != null ? latest.getFollowerCount() : 0L;
        if (followerCount == 0L && collectedDataQueryService.supportsPlatform(account.getPlatformType())) {
            Long collectedFollowers = collectedDataQueryService.latestFollowerCount(account.getTenantId(), account.getId());
            if (collectedFollowers != null) {
                followerCount = collectedFollowers;
            }
        }
        vo.setFollowerCount(followerCount);

        long contentCount = contentMapper.selectCount(new LambdaQueryWrapper<ContentDO>()
                .eq(ContentDO::getTenantId, account.getTenantId())
                .eq(ContentDO::getAccountId, account.getId()));
        if (collectedDataQueryService.supportsPlatform(account.getPlatformType())) {
            contentCount += collectedDataQueryService.workCount(account.getTenantId(), account.getId(),
                    account.getPlatformType());
        }
        vo.setContentCount(Math.toIntExact(contentCount));
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

