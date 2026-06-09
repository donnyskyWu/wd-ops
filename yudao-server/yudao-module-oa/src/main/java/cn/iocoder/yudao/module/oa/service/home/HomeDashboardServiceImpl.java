package cn.iocoder.yudao.module.oa.service.home;

import cn.iocoder.yudao.framework.common.exception.OaErrorCodes;
import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.oa.api.dto.home.DashboardAccountOverviewVO;
import cn.iocoder.yudao.module.oa.api.dto.home.DashboardAlertItemVO;
import cn.iocoder.yudao.module.oa.api.dto.home.DashboardContentOverviewVO;
import cn.iocoder.yudao.module.oa.api.dto.home.DashboardHomeKpiVO;
import cn.iocoder.yudao.module.oa.api.dto.home.DashboardTodoItemVO;
import cn.iocoder.yudao.module.oa.api.dto.home.HomeMetricsVO;
import cn.iocoder.yudao.module.oa.api.dto.home.PlatformDistVO;
import cn.iocoder.yudao.module.oa.api.dto.home.QuickActionVO;
import cn.iocoder.yudao.module.oa.api.dto.home.TodoVO;
import cn.iocoder.yudao.module.oa.api.dto.home.TrendPointVO;
import cn.iocoder.yudao.module.oa.dal.dataobject.account.AccountDO;
import cn.iocoder.yudao.module.oa.dal.dataobject.analytics.AccountStatusLogDO;
import cn.iocoder.yudao.module.oa.dal.dataobject.author.AuthorDO;
import cn.iocoder.yudao.module.oa.dal.dataobject.content.ProductionContentDO;
import cn.iocoder.yudao.module.oa.dal.dataobject.home.HomeAlertDO;
import cn.iocoder.yudao.module.oa.dal.dataobject.ipgroup.IpGroupDO;
import cn.iocoder.yudao.module.oa.dal.dataobject.operations.ContentDO;
import cn.iocoder.yudao.module.oa.dal.dataobject.operations.FollowerDailyDO;
import cn.iocoder.yudao.module.oa.dal.dataobject.perf.PerfRecordDO;
import cn.iocoder.yudao.module.oa.dal.dataobject.sop.SopReviewDO;
import cn.iocoder.yudao.module.oa.dal.dataobject.sop.TaskDO;
import cn.iocoder.yudao.module.oa.dal.mysql.account.AccountMapper;
import cn.iocoder.yudao.module.oa.dal.mysql.analytics.AccountStatusLogMapper;
import cn.iocoder.yudao.module.oa.dal.mysql.author.AuthorMapper;
import cn.iocoder.yudao.module.oa.dal.mysql.content.ProductionContentMapper;
import cn.iocoder.yudao.module.oa.dal.mysql.home.HomeAlertMapper;
import cn.iocoder.yudao.module.oa.dal.mysql.ipgroup.IpGroupMapper;
import cn.iocoder.yudao.module.oa.dal.mysql.operations.ContentMapper;
import cn.iocoder.yudao.module.oa.dal.mysql.operations.FollowerDailyMapper;
import cn.iocoder.yudao.module.oa.dal.mysql.perf.PerfRecordMapper;
import cn.iocoder.yudao.module.oa.dal.mysql.sop.SopReviewMapper;
import cn.iocoder.yudao.module.oa.dal.mysql.sop.TaskMapper;
import cn.iocoder.yudao.module.oa.framework.auth.LoginUser;
import cn.iocoder.yudao.module.oa.framework.auth.LoginUserContext;
import cn.iocoder.yudao.module.oa.service.dict.DictService;
import cn.iocoder.yudao.module.oa.service.support.OaTenantSupport;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class HomeDashboardServiceImpl implements HomeDashboardService {

    private static final long CACHE_TTL_MS = 5 * 60 * 1000L;
    private static final ConcurrentHashMap<String, CacheEntry<?>> CACHE = new ConcurrentHashMap<>();

    private static final String TREND_CONTENT = "CONTENT";
    private static final String TREND_FOLLOWER = "FOLLOWER";

    private final IpGroupMapper ipGroupMapper;
    private final AuthorMapper authorMapper;
    private final ContentMapper contentMapper;
    private final TaskMapper taskMapper;
    private final PerfRecordMapper perfRecordMapper;
    private final AccountMapper accountMapper;
    private final FollowerDailyMapper followerDailyMapper;
    private final ProductionContentMapper productionContentMapper;
    private final SopReviewMapper sopReviewMapper;
    private final AccountStatusLogMapper accountStatusLogMapper;
    private final HomeAlertMapper homeAlertMapper;
    private final DictService dictService;

    @Override
    public DashboardHomeKpiVO getKpi(Long ipGroupId, LocalDate startDate, LocalDate endDate) {
        return cached("kpi", ipGroupId, startDate, endDate, null, null, null, () -> {
            QueryContext ctx = buildContext(ipGroupId, resolveDateRange(startDate, endDate, 0));
            List<AccountDO> accounts = queryAccounts(ctx);
            long totalAccounts = accounts.size();
            long totalFollowers = sumLatestFollowers(ctx.tenantId(), accounts);
            LocalDate today = LocalDate.now();
            long todayContent = countContents(ctx.tenantId(), ctx.accountIds(), today, today);
            long yesterdayContent = countContents(ctx.tenantId(), ctx.accountIds(), today.minusDays(1), today.minusDays(1));
            long pendingReview = countPendingReviews(ctx);
            long alertCount = countAlerts(ctx);

            DashboardHomeKpiVO vo = new DashboardHomeKpiVO();
            vo.setTotalAccounts(totalAccounts);
            vo.setAccountChangeRate(BigDecimal.ZERO);
            vo.setTotalFollowers(totalFollowers);
            vo.setFollowerChangeRate(calcFollowerChangeRate(ctx.tenantId(), accounts));
            vo.setTodayContentCount(todayContent);
            vo.setContentChangeRate(calcChangeRate(todayContent, yesterdayContent));
            vo.setPendingReviewCount(pendingReview);
            vo.setAlertCount(alertCount);
            return vo;
        });
    }

    @Override
    public List<DashboardAccountOverviewVO> getAccountOverview(Long ipGroupId) {
        return cached("account-overview", ipGroupId, null, null, null, null, null, () -> {
            QueryContext ctx = buildContext(ipGroupId, null);
            List<AccountDO> accounts = queryAccounts(ctx);
            Map<String, List<AccountDO>> byPlatform = accounts.stream()
                    .collect(Collectors.groupingBy(AccountDO::getPlatformType));
            List<DashboardAccountOverviewVO> result = new ArrayList<>();
            for (Map.Entry<String, List<AccountDO>> entry : byPlatform.entrySet()) {
                DashboardAccountOverviewVO vo = new DashboardAccountOverviewVO();
                vo.setPlatformType(entry.getKey());
                vo.setAccountCount((long) entry.getValue().size());
                vo.setFollowerCount(sumLatestFollowers(ctx.tenantId(), entry.getValue()));
                result.add(vo);
            }
            result.sort(Comparator.comparing(DashboardAccountOverviewVO::getAccountCount).reversed());
            return result;
        });
    }

    @Override
    public List<DashboardContentOverviewVO> getContentOverview(Long ipGroupId) {
        return cached("content-overview", ipGroupId, null, null, null, null, null, () -> {
            QueryContext ctx = buildContext(ipGroupId, null);
            LocalDate end = LocalDate.now();
            LocalDate start = end.minusDays(6);
            List<ContentDO> contents = contentMapper.selectList(new LambdaQueryWrapper<ContentDO>()
                    .eq(ContentDO::getTenantId, ctx.tenantId())
                    .in(!ctx.accountIds().isEmpty(), ContentDO::getAccountId, ctx.accountIds())
                    .ge(ContentDO::getPublishTime, start.atStartOfDay())
                    .le(ContentDO::getPublishTime, end.plusDays(1).atStartOfDay()));
            Map<LocalDate, DashboardContentOverviewVO> dayMap = new LinkedHashMap<>();
            for (LocalDate d = start; !d.isAfter(end); d = d.plusDays(1)) {
                DashboardContentOverviewVO row = new DashboardContentOverviewVO();
                row.setDate(d.toString());
                row.setWechatCount(0L);
                row.setDouyinCount(0L);
                row.setKuaishouCount(0L);
                row.setXiaohongshuCount(0L);
                row.setVideoAccountCount(0L);
                dayMap.put(d, row);
            }
            for (ContentDO content : contents) {
                if (content.getPublishTime() == null) {
                    continue;
                }
                LocalDate day = content.getPublishTime().toLocalDate();
                DashboardContentOverviewVO row = dayMap.get(day);
                if (row == null) {
                    continue;
                }
                incrementPlatformCount(row, content.getPlatformType());
            }
            return new ArrayList<>(dayMap.values());
        });
    }

    @Override
    public List<DashboardAlertItemVO> getAlertList(Long ipGroupId, Integer limit) {
        int top = limit == null ? 5 : Math.min(limit, 50);
        return cached("alert-list", ipGroupId, null, null, null, null, top, () -> {
            QueryContext ctx = buildContext(ipGroupId, null);
            List<DashboardAlertItemVO> alerts = new ArrayList<>();
            long seq = 1L;
            List<HomeAlertDO> seeded = homeAlertMapper.selectList(new LambdaQueryWrapper<HomeAlertDO>()
                    .eq(HomeAlertDO::getTenantId, ctx.tenantId())
                    .eq(HomeAlertDO::getStatus, "PENDING")
                    .orderByDesc(HomeAlertDO::getTriggerTime)
                    .last("LIMIT " + top));
            for (HomeAlertDO row : seeded) {
                DashboardAlertItemVO vo = new DashboardAlertItemVO();
                vo.setAlertId(row.getId());
                vo.setAlertLevel(row.getAlertLevel());
                vo.setAlertContent(row.getAlertContent());
                vo.setTriggerTime(row.getTriggerTime());
                alerts.add(vo);
            }
            if (alerts.size() < top) {
                List<AccountStatusLogDO> warnings = accountStatusLogMapper.selectList(
                        new LambdaQueryWrapper<AccountStatusLogDO>()
                                .eq(AccountStatusLogDO::getTenantId, ctx.tenantId())
                                .eq(AccountStatusLogDO::getStatus, "WARNING")
                                .in(!ctx.accountIds().isEmpty(), AccountStatusLogDO::getAccountId, ctx.accountIds())
                                .orderByDesc(AccountStatusLogDO::getStatDate)
                                .last("LIMIT " + (top - alerts.size())));
                for (AccountStatusLogDO row : warnings) {
                    DashboardAlertItemVO vo = new DashboardAlertItemVO();
                    vo.setAlertId(seq++);
                    vo.setAlertLevel("WARNING");
                    vo.setAlertContent("账号" + row.getAccountId() + "状态异常（" + row.getStatDate() + "）");
                    vo.setTriggerTime(row.getStatDate().atStartOfDay());
                    alerts.add(vo);
                }
            }
            return alerts.stream().limit(top).collect(Collectors.toList());
        });
    }

    @Override
    public List<DashboardTodoItemVO> getTodoList(Long ipGroupId) {
        return cached("todo-list", ipGroupId, null, null, null, null, null, () -> {
            QueryContext ctx = buildContext(ipGroupId, null);
            List<DashboardTodoItemVO> items = new ArrayList<>();
            long sopPending = sopReviewMapper.selectCount(new LambdaQueryWrapper<SopReviewDO>()
                    .eq(SopReviewDO::getTenantId, ctx.tenantId())
                    .eq(SopReviewDO::getStatus, "PENDING")).longValue();
            if (sopPending > 0) {
                DashboardTodoItemVO vo = new DashboardTodoItemVO();
                vo.setType("REVIEW");
                vo.setTitle("SOP 待审核");
                vo.setCount(sopPending);
                vo.setRoute("/ops/sop/review");
                items.add(vo);
            }
            long draftPerf = perfRecordMapper.selectCount(new LambdaQueryWrapper<PerfRecordDO>()
                    .eq(PerfRecordDO::getTenantId, ctx.tenantId())
                    .eq(PerfRecordDO::getStatus, "DRAFT")
                    .eq(ctx.ipGroupId() != null, PerfRecordDO::getIpGroupId, ctx.ipGroupId())).longValue();
            if (draftPerf > 0) {
                DashboardTodoItemVO vo = new DashboardTodoItemVO();
                vo.setType("TASK");
                vo.setTitle("绩效草稿待处理");
                vo.setCount(draftPerf);
                vo.setRoute("/ops/perf/record");
                items.add(vo);
            }
            long contentReview = productionContentMapper.selectCount(new LambdaQueryWrapper<ProductionContentDO>()
                    .eq(ProductionContentDO::getTenantId, ctx.tenantId())
                    .in(ProductionContentDO::getStatus, "PENDING_FIRST_REVIEW", "PENDING_SECOND_REVIEW")).longValue();
            if (contentReview > 0) {
                DashboardTodoItemVO vo = new DashboardTodoItemVO();
                vo.setType("REVIEW");
                vo.setTitle("内容待审核");
                vo.setCount(contentReview);
                vo.setRoute("/ops/content/review");
                items.add(vo);
            }
            long expiredTasks = taskMapper.selectCount(new LambdaQueryWrapper<TaskDO>()
                    .eq(TaskDO::getTenantId, ctx.tenantId())
                    .in(TaskDO::getStatus, "PENDING", "IN_PROGRESS", "PENDING_REVIEW")
                    .lt(TaskDO::getSlaDeadline, LocalDateTime.now())
                    .eq(ctx.ipGroupId() != null, TaskDO::getIpGroupId, ctx.ipGroupId())).longValue();
            if (expiredTasks > 0) {
                DashboardTodoItemVO vo = new DashboardTodoItemVO();
                vo.setType("EXPIRE");
                vo.setTitle("超时任务");
                vo.setCount(expiredTasks);
                vo.setRoute("/ops/sop/task");
                items.add(vo);
            }
            return items;
        });
    }

    @Override
    public HomeMetricsVO getMetrics(Long ipGroupId, LocalDate startDate, LocalDate endDate) {
        return cached("metrics", ipGroupId, startDate, endDate, null, null, null, () -> {
            DateRange range = resolveDateRange(startDate, endDate, 0);
            QueryContext ctx = buildContext(ipGroupId, range);
            long authorCount = authorMapper.selectCount(new LambdaQueryWrapper<AuthorDO>()
                    .eq(AuthorDO::getTenantId, ctx.tenantId())
                    .eq(AuthorDO::getStatus, 1)
                    .eq(ctx.ipGroupId() != null, AuthorDO::getIpGroupId, ctx.ipGroupId()));
            long contentCount = countContents(ctx.tenantId(), ctx.accountIds(), range.start(), range.end());
            List<TaskDO> tasks = taskMapper.selectList(new LambdaQueryWrapper<TaskDO>()
                    .eq(TaskDO::getTenantId, ctx.tenantId())
                    .eq(ctx.ipGroupId() != null, TaskDO::getIpGroupId, ctx.ipGroupId()));
            BigDecimal sopRate = calcSopCompletionRate(tasks);
            String avgGrade = calcAvgPerfGrade(ctx);

            HomeMetricsVO vo = new HomeMetricsVO();
            vo.setTotalAuthors((int) authorCount);
            vo.setTotalContent((int) contentCount);
            vo.setSopCompletionRate(sopRate);
            vo.setAvgPerfGrade(avgGrade);
            return vo;
        });
    }

    @Override
    public List<TrendPointVO> getTrend(Long ipGroupId, LocalDate startDate, LocalDate endDate,
                                       String platformType, String type) {
        validatePlatformType(platformType);
        String trendType = type == null || type.isBlank() ? TREND_CONTENT : type.toUpperCase();
        if (!TREND_CONTENT.equals(trendType) && !TREND_FOLLOWER.equals(trendType)) {
            throw new ServiceException(OaErrorCodes.HOME_TREND_TYPE_INVALID);
        }
        String finalTrendType = trendType;
        return cached("trend", ipGroupId, startDate, endDate, platformType, trendType, null, () -> {
            DateRange range = resolveDateRange(startDate, endDate, 7);
            QueryContext ctx = buildContext(ipGroupId, range);
            if (TREND_FOLLOWER.equals(finalTrendType)) {
                return buildFollowerTrend(ctx, range, platformType);
            }
            return buildContentTrend(ctx, range, platformType);
        });
    }

    @Override
    public List<PlatformDistVO> getPlatformDist(Long ipGroupId, LocalDate startDate, LocalDate endDate) {
        return cached("platform-dist", ipGroupId, startDate, endDate, null, null, null, () -> {
            DateRange range = resolveDateRange(startDate, endDate, 7);
            QueryContext ctx = buildContext(ipGroupId, range);
            List<ContentDO> contents = contentMapper.selectList(new LambdaQueryWrapper<ContentDO>()
                    .eq(ContentDO::getTenantId, ctx.tenantId())
                    .in(!ctx.accountIds().isEmpty(), ContentDO::getAccountId, ctx.accountIds())
                    .ge(ContentDO::getPublishTime, range.start().atStartOfDay())
                    .le(ContentDO::getPublishTime, range.end().plusDays(1).atStartOfDay()));
            Map<String, Long> counts = contents.stream()
                    .collect(Collectors.groupingBy(ContentDO::getPlatformType, Collectors.counting()));
            long total = counts.values().stream().mapToLong(Long::longValue).sum();
            List<PlatformDistVO> result = new ArrayList<>();
            for (Map.Entry<String, Long> entry : counts.entrySet()) {
                PlatformDistVO vo = new PlatformDistVO();
                vo.setPlatform(entry.getKey());
                vo.setCount(entry.getValue());
                vo.setPercentage(total == 0 ? BigDecimal.ZERO
                        : BigDecimal.valueOf(entry.getValue() * 100.0 / total).setScale(2, RoundingMode.HALF_UP));
                result.add(vo);
            }
            result.sort(Comparator.comparing(PlatformDistVO::getCount).reversed());
            return result;
        });
    }

    @Override
    public List<TodoVO> getTodos(Long ipGroupId, Integer limit) {
        int top = limit == null ? 10 : Math.min(limit, 50);
        return cached("todos", ipGroupId, null, null, null, null, top, () -> {
            QueryContext ctx = buildContext(ipGroupId, null);
            List<TodoVO> todos = new ArrayList<>();
            List<SopReviewDO> reviews = sopReviewMapper.selectList(new LambdaQueryWrapper<SopReviewDO>()
                    .eq(SopReviewDO::getTenantId, ctx.tenantId())
                    .eq(SopReviewDO::getStatus, "PENDING")
                    .orderByDesc(SopReviewDO::getCreateTime)
                    .last("LIMIT " + top));
            for (SopReviewDO review : reviews) {
                TaskDO task = taskMapper.selectById(review.getTaskId());
                TodoVO vo = new TodoVO();
                vo.setTitle(task == null ? "SOP 待审核" : "SOP《" + task.getPlanName() + "》待审核");
                vo.setSource("SOP");
                vo.setTime(toOffset(review.getCreateTime()));
                vo.setActionUrl("/ops/sop/review/" + review.getId());
                todos.add(vo);
            }
            if (todos.size() < top) {
                List<ProductionContentDO> contents = productionContentMapper.selectList(
                        new LambdaQueryWrapper<ProductionContentDO>()
                                .eq(ProductionContentDO::getTenantId, ctx.tenantId())
                                .in(ProductionContentDO::getStatus, "PENDING_FIRST_REVIEW", "PENDING_SECOND_REVIEW")
                                .orderByDesc(ProductionContentDO::getCreateTime)
                                .last("LIMIT " + (top - todos.size())));
                for (ProductionContentDO content : contents) {
                    TodoVO vo = new TodoVO();
                    vo.setTitle("内容《" + content.getTitle() + "》待审核");
                    vo.setSource("PUBLISH");
                    vo.setTime(toOffset(content.getCreateTime()));
                    vo.setActionUrl("/ops/content/review/" + content.getId());
                    todos.add(vo);
                }
            }
            if (todos.size() < top) {
                List<PerfRecordDO> drafts = perfRecordMapper.selectList(new LambdaQueryWrapper<PerfRecordDO>()
                        .eq(PerfRecordDO::getTenantId, ctx.tenantId())
                        .eq(PerfRecordDO::getStatus, "DRAFT")
                        .eq(ctx.ipGroupId() != null, PerfRecordDO::getIpGroupId, ctx.ipGroupId())
                        .orderByDesc(PerfRecordDO::getCreateTime)
                        .last("LIMIT " + (top - todos.size())));
                for (PerfRecordDO record : drafts) {
                    TodoVO vo = new TodoVO();
                    vo.setTitle("绩效草稿待核算（用户" + record.getTargetUserId() + "）");
                    vo.setSource("PERF");
                    vo.setTime(toOffset(record.getCreateTime()));
                    vo.setActionUrl("/ops/perf/record/" + record.getId());
                    todos.add(vo);
                }
            }
            return todos.stream().limit(top).collect(Collectors.toList());
        });
    }

    @Override
    public List<QuickActionVO> getQuickActions() {
        return cached("quick-actions", null, null, null, null, null, null, () -> {
            LoginUser user = LoginUserContext.getRequired();
            Set<String> authorities = user.getAuthorities() == null ? Set.of() : user.getAuthorities();
            List<QuickActionVO> all = List.of(
                    action("IP 组管理", "icon-ip-group", "/ops/ip-group", "oa:ip-group:list"),
                    action("作者管理", "icon-author", "/ops/author", "oa:author:list"),
                    action("账号管理", "icon-account", "/ops/account", "oa:account:list"),
                    action("SOP 管理", "icon-sop", "/ops/sop", "oa:sop:list"),
                    action("绩效管理", "icon-perf", "/ops/perf", "oa:perf:list"),
                    action("数据报表", "icon-report", "/ops/report", "oa:report:list"),
                    action("用户管理", "icon-user", "/ops/system/user", "oa:user:list"),
                    action("租户管理", "icon-tenant", "/ops/system/tenant", "oa:tenant:list")
            );
            return all.stream()
                    .filter(a -> authorities.contains(a.getPermission()))
                    .limit(8)
                    .collect(Collectors.toList());
        });
    }

    @Override
    public void refresh() {
        Long tenantId = OaTenantSupport.requireTenantId();
        Long userId = TenantContextHolder.getUserId();
        String prefix = tenantId + ":" + userId + ":";
        CACHE.keySet().removeIf(key -> key.startsWith(prefix));
    }

    private QuickActionVO action(String name, String icon, String url, String permission) {
        QuickActionVO vo = new QuickActionVO();
        vo.setName(name);
        vo.setIcon(icon);
        vo.setUrl(url);
        vo.setPermission(permission);
        return vo;
    }

    private List<TrendPointVO> buildContentTrend(QueryContext ctx, DateRange range, String platformType) {
        List<ContentDO> contents = contentMapper.selectList(new LambdaQueryWrapper<ContentDO>()
                .eq(ContentDO::getTenantId, ctx.tenantId())
                .in(!ctx.accountIds().isEmpty(), ContentDO::getAccountId, ctx.accountIds())
                .eq(platformType != null, ContentDO::getPlatformType, platformType)
                .ge(ContentDO::getPublishTime, range.start().atStartOfDay())
                .le(ContentDO::getPublishTime, range.end().plusDays(1).atStartOfDay()));
        Map<String, Long> grouped = new HashMap<>();
        for (ContentDO content : contents) {
            if (content.getPublishTime() == null) {
                continue;
            }
            String key = content.getPublishTime().toLocalDate() + "|" + content.getPlatformType();
            grouped.merge(key, 1L, Long::sum);
        }
        List<TrendPointVO> points = new ArrayList<>();
        for (Map.Entry<String, Long> entry : grouped.entrySet()) {
            String[] parts = entry.getKey().split("\\|", 2);
            TrendPointVO vo = new TrendPointVO();
            vo.setDate(parts[0]);
            vo.setPlatform(parts.length > 1 ? parts[1] : null);
            vo.setCount(entry.getValue());
            points.add(vo);
        }
        points.sort(Comparator.comparing(TrendPointVO::getDate).thenComparing(TrendPointVO::getPlatform));
        return points;
    }

    private List<TrendPointVO> buildFollowerTrend(QueryContext ctx, DateRange range, String platformType) {
        List<AccountDO> accounts = queryAccounts(ctx).stream()
                .filter(a -> platformType == null || platformType.equals(a.getPlatformType()))
                .collect(Collectors.toList());
        if (accounts.isEmpty()) {
            return List.of();
        }
        Set<Long> accountIds = accounts.stream().map(AccountDO::getId).collect(Collectors.toSet());
        Map<Long, String> platformMap = accounts.stream()
                .collect(Collectors.toMap(AccountDO::getId, AccountDO::getPlatformType, (a, b) -> a));
        List<FollowerDailyDO> rows = followerDailyMapper.selectList(new LambdaQueryWrapper<FollowerDailyDO>()
                .eq(FollowerDailyDO::getTenantId, ctx.tenantId())
                .in(FollowerDailyDO::getAccountId, accountIds)
                .ge(FollowerDailyDO::getStatDate, range.start())
                .le(FollowerDailyDO::getStatDate, range.end()));
        Map<String, Long> grouped = new HashMap<>();
        for (FollowerDailyDO row : rows) {
            String platform = platformMap.getOrDefault(row.getAccountId(), "UNKNOWN");
            String key = row.getStatDate() + "|" + platform;
            long net = row.getNetGrowth() == null ? 0L : row.getNetGrowth();
            grouped.merge(key, net, Long::sum);
        }
        List<TrendPointVO> points = new ArrayList<>();
        for (Map.Entry<String, Long> entry : grouped.entrySet()) {
            String[] parts = entry.getKey().split("\\|", 2);
            TrendPointVO vo = new TrendPointVO();
            vo.setDate(parts[0]);
            vo.setPlatform(parts.length > 1 ? parts[1] : null);
            vo.setCount(entry.getValue());
            points.add(vo);
        }
        points.sort(Comparator.comparing(TrendPointVO::getDate).thenComparing(TrendPointVO::getPlatform));
        return points;
    }

    private BigDecimal calcSopCompletionRate(List<TaskDO> tasks) {
        if (tasks.isEmpty()) {
            return BigDecimal.ZERO;
        }
        long done = tasks.stream().filter(t -> "DONE".equals(t.getStatus())).count();
        return BigDecimal.valueOf(done * 100.0 / tasks.size()).setScale(2, RoundingMode.HALF_UP);
    }

    private String calcAvgPerfGrade(QueryContext ctx) {
        List<PerfRecordDO> records = perfRecordMapper.selectList(new LambdaQueryWrapper<PerfRecordDO>()
                .eq(PerfRecordDO::getTenantId, ctx.tenantId())
                .isNotNull(PerfRecordDO::getTotalScore)
                .eq(ctx.ipGroupId() != null, PerfRecordDO::getIpGroupId, ctx.ipGroupId()));
        if (records.isEmpty()) {
            return "-";
        }
        double avg = records.stream()
                .map(PerfRecordDO::getTotalScore)
                .filter(Objects::nonNull)
                .mapToDouble(BigDecimal::doubleValue)
                .average()
                .orElse(0);
        return scoreToGrade(avg);
    }

    private String scoreToGrade(double score) {
        if (score >= 90) {
            return "S";
        }
        if (score >= 80) {
            return "A";
        }
        if (score >= 70) {
            return "B";
        }
        if (score >= 60) {
            return "C";
        }
        return "D";
    }

    private long countPendingReviews(QueryContext ctx) {
        long sop = sopReviewMapper.selectCount(new LambdaQueryWrapper<SopReviewDO>()
                .eq(SopReviewDO::getTenantId, ctx.tenantId())
                .eq(SopReviewDO::getStatus, "PENDING"));
        long content = productionContentMapper.selectCount(new LambdaQueryWrapper<ProductionContentDO>()
                .eq(ProductionContentDO::getTenantId, ctx.tenantId())
                .in(ProductionContentDO::getStatus, "PENDING_FIRST_REVIEW", "PENDING_SECOND_REVIEW"));
        return sop + content;
    }

    private long countAlerts(QueryContext ctx) {
        long seeded = homeAlertMapper.selectCount(new LambdaQueryWrapper<HomeAlertDO>()
                .eq(HomeAlertDO::getTenantId, ctx.tenantId())
                .eq(HomeAlertDO::getStatus, "PENDING"));
        long warnings = accountStatusLogMapper.selectCount(new LambdaQueryWrapper<AccountStatusLogDO>()
                .eq(AccountStatusLogDO::getTenantId, ctx.tenantId())
                .eq(AccountStatusLogDO::getStatus, "WARNING")
                .in(!ctx.accountIds().isEmpty(), AccountStatusLogDO::getAccountId, ctx.accountIds()));
        return seeded + warnings;
    }

    private long countContents(Long tenantId, Set<Long> accountIds, LocalDate start, LocalDate end) {
        return contentMapper.selectCount(new LambdaQueryWrapper<ContentDO>()
                .eq(ContentDO::getTenantId, tenantId)
                .in(!accountIds.isEmpty(), ContentDO::getAccountId, accountIds)
                .ge(ContentDO::getPublishTime, start.atStartOfDay())
                .le(ContentDO::getPublishTime, end.plusDays(1).atStartOfDay()));
    }

    private List<AccountDO> queryAccounts(QueryContext ctx) {
        return accountMapper.selectList(new LambdaQueryWrapper<AccountDO>()
                .eq(AccountDO::getTenantId, ctx.tenantId())
                .eq(ctx.ipGroupId() != null, AccountDO::getIpGroupId, ctx.ipGroupId()));
    }

    private long sumLatestFollowers(Long tenantId, List<AccountDO> accounts) {
        long sum = 0L;
        for (AccountDO account : accounts) {
            sum += latestFollower(tenantId, account.getId());
        }
        return sum;
    }

    private long latestFollower(Long tenantId, Long accountId) {
        FollowerDailyDO latest = followerDailyMapper.selectOne(new LambdaQueryWrapper<FollowerDailyDO>()
                .eq(FollowerDailyDO::getTenantId, tenantId)
                .eq(FollowerDailyDO::getAccountId, accountId)
                .orderByDesc(FollowerDailyDO::getStatDate)
                .last("LIMIT 1"));
        return latest == null || latest.getFollowerCount() == null ? 0L : latest.getFollowerCount();
    }

    private BigDecimal calcFollowerChangeRate(Long tenantId, List<AccountDO> accounts) {
        LocalDate today = LocalDate.now();
        LocalDate weekAgo = today.minusDays(7);
        long current = sumLatestFollowers(tenantId, accounts);
        long past = 0L;
        for (AccountDO account : accounts) {
            FollowerDailyDO row = followerDailyMapper.selectOne(new LambdaQueryWrapper<FollowerDailyDO>()
                    .eq(FollowerDailyDO::getTenantId, tenantId)
                    .eq(FollowerDailyDO::getAccountId, account.getId())
                    .le(FollowerDailyDO::getStatDate, weekAgo)
                    .orderByDesc(FollowerDailyDO::getStatDate)
                    .last("LIMIT 1"));
            if (row != null && row.getFollowerCount() != null) {
                past += row.getFollowerCount();
            }
        }
        return calcChangeRate(current, past);
    }

    private BigDecimal calcChangeRate(long current, long previous) {
        if (previous == 0) {
            return current > 0 ? BigDecimal.valueOf(100) : BigDecimal.ZERO;
        }
        return BigDecimal.valueOf((current - previous) * 100.0 / previous).setScale(2, RoundingMode.HALF_UP);
    }

    private void incrementPlatformCount(DashboardContentOverviewVO row, String platformType) {
        if (platformType == null) {
            return;
        }
        switch (platformType) {
            case "WECHAT_OFFICIAL" -> row.setWechatCount(row.getWechatCount() + 1);
            case "DOUYIN" -> row.setDouyinCount(row.getDouyinCount() + 1);
            case "KUAISHOU" -> row.setKuaishouCount(row.getKuaishouCount() + 1);
            case "XIAOHONGSHU" -> row.setXiaohongshuCount(row.getXiaohongshuCount() + 1);
            case "WECHAT_VIDEO" -> row.setVideoAccountCount(row.getVideoAccountCount() + 1);
            default -> {
            }
        }
    }

    private void validatePlatformType(String platformType) {
        if (platformType == null || platformType.isBlank()) {
            return;
        }
        if (!dictService.isValidValue("dict_platform_type", platformType)) {
            throw new ServiceException(OaErrorCodes.DICT_VALUE_INVALID);
        }
    }

    private void validateIpGroup(Long ipGroupId) {
        if (ipGroupId == null) {
            return;
        }
        IpGroupDO group = ipGroupMapper.selectById(ipGroupId);
        if (group == null) {
            throw new ServiceException(OaErrorCodes.ENTITY_NOT_EXISTS);
        }
        if (!Objects.equals(group.getTenantId(), OaTenantSupport.requireTenantId())) {
            throw new ServiceException(OaErrorCodes.TENANT_FORBIDDEN);
        }
        if (group.getStatus() == null || group.getStatus() != 1) {
            throw new ServiceException(OaErrorCodes.ENTITY_DISABLED);
        }
    }

    private DateRange resolveDateRange(LocalDate startDate, LocalDate endDate, int defaultDays) {
        LocalDate end = endDate == null ? LocalDate.now() : endDate;
        LocalDate start = startDate == null ? (defaultDays > 0 ? end.minusDays(defaultDays - 1L) : end) : startDate;
        if (end.isBefore(start)) {
            throw new ServiceException(OaErrorCodes.FINANCE_DATE_RANGE_INVALID);
        }
        return new DateRange(start, end);
    }

    private QueryContext buildContext(Long ipGroupId, DateRange range) {
        validateIpGroup(ipGroupId);
        Long tenantId = OaTenantSupport.requireTenantId();
        List<AccountDO> accounts = accountMapper.selectList(new LambdaQueryWrapper<AccountDO>()
                .eq(AccountDO::getTenantId, tenantId)
                .eq(ipGroupId != null, AccountDO::getIpGroupId, ipGroupId));
        Set<Long> accountIds = accounts.stream().map(AccountDO::getId).collect(Collectors.toCollection(HashSet::new));
        return new QueryContext(tenantId, ipGroupId, accountIds, range);
    }

    private OffsetDateTime toOffset(LocalDateTime time) {
        if (time == null) {
            return OffsetDateTime.now(ZoneOffset.ofHours(8));
        }
        return time.atOffset(ZoneOffset.ofHours(8));
    }

    @SuppressWarnings("unchecked")
    private <T> T cached(String endpoint, Long ipGroupId, LocalDate startDate, LocalDate endDate,
                         String platformType, String type, Integer limit, Supplier<T> loader) {
        String key = buildCacheKey(endpoint, ipGroupId, startDate, endDate, platformType, type, limit);
        CacheEntry<?> entry = CACHE.get(key);
        if (entry != null && !entry.isExpired()) {
            return (T) entry.value();
        }
        T value = loader.get();
        CACHE.put(key, new CacheEntry<>(value, System.currentTimeMillis() + CACHE_TTL_MS));
        return value;
    }

    private String buildCacheKey(String endpoint, Long ipGroupId, LocalDate startDate, LocalDate endDate,
                                 String platformType, String type, Integer limit) {
        Long tenantId = OaTenantSupport.requireTenantId();
        Long userId = TenantContextHolder.getUserId();
        return tenantId + ":" + userId + ":" + endpoint + ":" + ipGroupId + ":" + startDate + ":" + endDate
                + ":" + platformType + ":" + type + ":" + limit;
    }

    private record DateRange(LocalDate start, LocalDate end) {
    }

    private record QueryContext(Long tenantId, Long ipGroupId, Set<Long> accountIds, DateRange range) {
    }

    private record CacheEntry<T>(T value, long expireAt) {
        boolean isExpired() {
            return System.currentTimeMillis() > expireAt;
        }
    }

}
