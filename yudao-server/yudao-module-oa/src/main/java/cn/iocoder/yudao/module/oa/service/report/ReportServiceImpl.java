package cn.iocoder.yudao.module.oa.service.report;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.oa.api.dto.perf.ExportJobVO;
import cn.iocoder.yudao.module.oa.api.dto.report.ReportStatsVO;
import cn.iocoder.yudao.module.oa.dal.dataobject.account.AccountDO;
import cn.iocoder.yudao.module.oa.dal.dataobject.analytics.AccountStatusLogDO;
import cn.iocoder.yudao.module.oa.dal.dataobject.finance.AccountCostDO;
import cn.iocoder.yudao.module.oa.dal.dataobject.ipgroup.IpGroupDO;
import cn.iocoder.yudao.module.oa.dal.dataobject.ipgroup.IpGroupMemberDO;
import cn.iocoder.yudao.module.oa.dal.dataobject.operations.ContentDO;
import cn.iocoder.yudao.module.oa.dal.dataobject.operations.FollowerDailyDO;
import cn.iocoder.yudao.module.oa.dal.dataobject.perf.OrderAttributionDO;
import cn.iocoder.yudao.module.oa.dal.mysql.account.AccountMapper;
import cn.iocoder.yudao.module.oa.dal.mysql.analytics.AccountStatusLogMapper;
import cn.iocoder.yudao.module.oa.dal.mysql.finance.AccountCostMapper;
import cn.iocoder.yudao.module.oa.dal.mysql.ipgroup.IpGroupMapper;
import cn.iocoder.yudao.module.oa.dal.mysql.ipgroup.IpGroupMemberMapper;
import cn.iocoder.yudao.module.oa.dal.mysql.operations.ContentMapper;
import cn.iocoder.yudao.module.oa.dal.mysql.operations.FollowerDailyMapper;
import cn.iocoder.yudao.module.oa.dal.mysql.perf.OrderAttributionMapper;
import cn.iocoder.yudao.module.oa.framework.audit.AuditLog;
import cn.iocoder.yudao.module.oa.service.support.OaTenantSupport;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReportServiceImpl implements ReportService {

    private final AccountMapper accountMapper;
    private final FollowerDailyMapper followerDailyMapper;
    private final ContentMapper contentMapper;
    private final AccountStatusLogMapper accountStatusLogMapper;
    private final AccountCostMapper accountCostMapper;
    private final OrderAttributionMapper orderAttributionMapper;
    private final IpGroupMemberMapper ipGroupMemberMapper;
    private final IpGroupMapper ipGroupMapper;

    // ==================== 2.1 全平台账号视图 ====================

    @Override
    public PageResult<Map<String, Object>> unifiedAccountList(Long ipGroupId, Long accountId, String platformType,
                                                                LocalDate startDate, LocalDate endDate,
                                                                Integer pageNum, Integer pageSize) {
        Long tenantId = OaTenantSupport.requireTenantId();
        LambdaQueryWrapper<AccountDO> wrapper = new LambdaQueryWrapper<AccountDO>()
                .eq(AccountDO::getTenantId, tenantId)
                .eq(accountId != null, AccountDO::getId, accountId)
                .eq(ipGroupId != null, AccountDO::getIpGroupId, ipGroupId)
                .eq(platformType != null, AccountDO::getPlatformType, platformType)
                .orderByDesc(AccountDO::getId);
        Page<AccountDO> page = accountMapper.selectPage(new Page<>(safePage(pageNum), safeSize(pageSize)), wrapper);
        Map<Long, String> ipGroupNames = ipGroupNameMap(tenantId);
        Map<Long, BigDecimal[]> finance = financeByAccount(tenantId, startDate, endDate);
        List<Map<String, Object>> list = page.getRecords().stream().map(acc -> {
            Map<String, Object> row = new LinkedHashMap<>();
            BigDecimal[] fin = finance.getOrDefault(acc.getId(), new BigDecimal[]{BigDecimal.ZERO, BigDecimal.ZERO});
            BigDecimal revenue = fin[0];
            BigDecimal cost = fin[1];
            row.put("account_id", acc.getId());
            row.put("account_name", acc.getAccountName());
            row.put("platform_type", acc.getPlatformType());
            row.put("ip_group_name", acc.getIpGroupId() == null ? "-" : ipGroupNames.getOrDefault(acc.getIpGroupId(), "-"));
            row.put("follower_count", latestFollower(tenantId, acc.getId()));
            row.put("content_count", contentCount(tenantId, acc.getId()));
            row.put("revenue", revenue);
            row.put("cost", cost);
            row.put("roi", roi(revenue, cost));
            // camelCase 兼容
            row.put("accountId", acc.getId());
            row.put("accountName", acc.getAccountName());
            return row;
        }).collect(Collectors.toList());
        return new PageResult<>(list, page.getTotal());
    }

    @Override
    public ReportStatsVO unifiedAccountStats(Long ipGroupId, Long accountId, String platformType,
                                             LocalDate startDate, LocalDate endDate) {
        Long tenantId = OaTenantSupport.requireTenantId();
        LambdaQueryWrapper<AccountDO> wrapper = new LambdaQueryWrapper<AccountDO>()
                .eq(AccountDO::getTenantId, tenantId)
                .eq(accountId != null, AccountDO::getId, accountId)
                .eq(ipGroupId != null, AccountDO::getIpGroupId, ipGroupId)
                .eq(platformType != null, AccountDO::getPlatformType, platformType);
        List<AccountDO> accounts = accountMapper.selectList(wrapper);
        ReportStatsVO vo = new ReportStatsVO();
        vo.setTotalAccounts((long) accounts.size());
        vo.setTotalFollowers(accounts.stream().mapToLong(a -> latestFollower(tenantId, a.getId())).sum());
        List<ContentDO> contents = queryContents(tenantId, accountId);
        vo.setTotalContents((long) contents.size());
        vo.setTotalReads(contents.stream().mapToLong(c -> c.getReadCount() == null ? 0 : c.getReadCount()).sum());
        BigDecimal revenue = BigDecimal.ZERO;
        BigDecimal cost = BigDecimal.ZERO;
        for (BigDecimal[] fin : financeByAccount(tenantId, startDate, endDate).values()) {
            revenue = revenue.add(fin[0]);
            cost = cost.add(fin[1]);
        }
        vo.setTotalRevenue(revenue);
        vo.setOverallRoi(roi(revenue, cost));
        for (AccountDO acc : accounts) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("accountId", acc.getId());
            item.put("accountName", acc.getAccountName());
            item.put("followerCount", latestFollower(tenantId, acc.getId()));
            vo.getItems().add(item);
        }
        return vo;
    }

    @Override
    @AuditLog(module = "M7-report", action = "export-unified-account")
    public ExportJobVO unifiedAccountExport(LocalDate startDate, LocalDate endDate) {
        OaTenantSupport.requireDateRange(startDate, endDate);
        return OaTenantSupport.stubExportJob();
    }

    // ==================== 2.2 账号状态监控 ====================

    @Override
    public List<Map<String, Object>> accountStatusTrend(Long accountId, LocalDate startDate, LocalDate endDate) {
        Long tenantId = OaTenantSupport.requireTenantId();
        LocalDate[] range = defaultRange(startDate, endDate);
        List<AccountStatusLogDO> logs = accountStatusLogMapper.selectList(new LambdaQueryWrapper<AccountStatusLogDO>()
                .eq(AccountStatusLogDO::getTenantId, tenantId)
                .eq(accountId != null, AccountStatusLogDO::getAccountId, accountId)
                .ge(AccountStatusLogDO::getStatDate, range[0])
                .le(AccountStatusLogDO::getStatDate, range[1])
                .orderByAsc(AccountStatusLogDO::getStatDate));
        // 按日期聚合各状态数量
        Map<String, long[]> byDate = new TreeMap<>();
        for (AccountStatusLogDO log : logs) {
            String date = String.valueOf(log.getStatDate());
            long[] counts = byDate.computeIfAbsent(date, k -> new long[3]); // [online, abnormal, offline]
            String status = log.getStatus() == null ? "" : log.getStatus();
            switch (status) {
                case "WARNING", "ABNORMAL" -> counts[1]++;
                case "OFFLINE" -> counts[2]++;
                default -> counts[0]++;
            }
        }
        List<Map<String, Object>> result = new ArrayList<>();
        byDate.forEach((date, counts) -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("date", date);
            m.put("stat_date", date);
            m.put("online", counts[0]);
            m.put("abnormal", counts[1]);
            m.put("offline", counts[2]);
            result.add(m);
        });
        return result;
    }

    @Override
    public Map<String, Object> accountStatusSummary(Long accountId, LocalDate startDate, LocalDate endDate) {
        Long tenantId = OaTenantSupport.requireTenantId();
        LocalDate[] range = defaultRange(startDate, endDate);
        List<AccountStatusLogDO> logs = accountStatusLogMapper.selectList(new LambdaQueryWrapper<AccountStatusLogDO>()
                .eq(AccountStatusLogDO::getTenantId, tenantId)
                .eq(accountId != null, AccountStatusLogDO::getAccountId, accountId)
                .ge(AccountStatusLogDO::getStatDate, range[0])
                .le(AccountStatusLogDO::getStatDate, range[1]));
        long online = 0;
        long abnormal = 0;
        long offline = 0;
        long recovered = 0;
        for (AccountStatusLogDO log : logs) {
            String status = log.getStatus() == null ? "" : log.getStatus();
            switch (status) {
                case "WARNING", "ABNORMAL" -> abnormal++;
                case "OFFLINE" -> offline++;
                case "RECOVERED" -> recovered++;
                default -> online++;
            }
        }
        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("online", online);
        summary.put("abnormal", abnormal);
        summary.put("offline", offline);
        summary.put("recovered", recovered);
        summary.put("totalDays", logs.size());
        return summary;
    }

    @Override
    public PageResult<Map<String, Object>> accountStatusLog(Long accountId, LocalDate startDate, LocalDate endDate,
                                                            Integer pageNum, Integer pageSize) {
        Long tenantId = OaTenantSupport.requireTenantId();
        Page<AccountStatusLogDO> page = accountStatusLogMapper.selectPage(
                new Page<>(safePage(pageNum), safeSize(pageSize)),
                new LambdaQueryWrapper<AccountStatusLogDO>()
                        .eq(AccountStatusLogDO::getTenantId, tenantId)
                        .eq(accountId != null, AccountStatusLogDO::getAccountId, accountId)
                        .ge(startDate != null, AccountStatusLogDO::getStatDate, startDate)
                        .le(endDate != null, AccountStatusLogDO::getStatDate, endDate)
                        .orderByDesc(AccountStatusLogDO::getStatDate));
        Map<Long, String> accountNames = accountNameMap(tenantId);
        return new PageResult<>(page.getRecords().stream().map(row -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("account_id", row.getAccountId());
            m.put("account_name", accountNames.getOrDefault(row.getAccountId(), "账号#" + row.getAccountId()));
            m.put("date", row.getStatDate());
            m.put("status", row.getStatus());
            m.put("follower_count", row.getFollowerCount());
            m.put("remark", statusRemark(row.getStatus(), row.getFollowerCount()));
            return m;
        }).collect(Collectors.toList()), page.getTotal());
    }

    @Override
    @AuditLog(module = "M7-report", action = "export-account-status")
    public ExportJobVO accountStatusExport(LocalDate startDate, LocalDate endDate) {
        OaTenantSupport.requireDateRange(startDate, endDate);
        return OaTenantSupport.stubExportJob();
    }

    // ==================== 2.3 短视频产出 ====================

    @Override
    public PageResult<Map<String, Object>> videoOutputList(Long ipGroupId, Long accountId, LocalDate startDate, LocalDate endDate,
                                                           Integer pageNum, Integer pageSize) {
        Long tenantId = OaTenantSupport.requireTenantId();
        LambdaQueryWrapper<ContentDO> wrapper = new LambdaQueryWrapper<ContentDO>()
                .eq(ContentDO::getTenantId, tenantId)
                .eq(accountId != null, ContentDO::getAccountId, accountId)
                .eq(ContentDO::getContentType, "VIDEO")
                .ge(startDate != null, ContentDO::getPublishTime, startDate == null ? null : startDate.atStartOfDay())
                .le(endDate != null, ContentDO::getPublishTime, endDate == null ? null : endDate.plusDays(1).atStartOfDay())
                .orderByDesc(ContentDO::getPublishTime);
        Page<ContentDO> page = contentMapper.selectPage(new Page<>(safePage(pageNum), safeSize(pageSize)), wrapper);
        Map<Long, String> accountNames = accountNameMap(tenantId);
        return new PageResult<>(page.getRecords().stream()
                .map(c -> contentRow(c, accountNames)).collect(Collectors.toList()), page.getTotal());
    }

    @Override
    public List<Map<String, Object>> videoOutputTrend(Long accountId, LocalDate startDate, LocalDate endDate) {
        Long tenantId = OaTenantSupport.requireTenantId();
        List<ContentDO> contents = contentMapper.selectList(new LambdaQueryWrapper<ContentDO>()
                .eq(ContentDO::getTenantId, tenantId)
                .eq(accountId != null, ContentDO::getAccountId, accountId)
                .eq(ContentDO::getContentType, "VIDEO")
                .ge(startDate != null, ContentDO::getPublishTime, startDate == null ? null : startDate.atStartOfDay())
                .le(endDate != null, ContentDO::getPublishTime, endDate == null ? null : endDate.plusDays(1).atStartOfDay())
                .orderByAsc(ContentDO::getPublishTime));
        // 按 (日期, 平台) 聚合产出数
        Map<String, Map<String, Object>> agg = new LinkedHashMap<>();
        for (ContentDO c : contents) {
            if (c.getPublishTime() == null) {
                continue;
            }
            String date = c.getPublishTime().toLocalDate().toString();
            String platform = c.getPlatformType() == null ? "未知" : c.getPlatformType();
            String key = date + "|" + platform;
            Map<String, Object> row = agg.computeIfAbsent(key, k -> {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("date", date);
                m.put("stat_date", date);
                m.put("platform", platform);
                m.put("platform_type", platform);
                m.put("output_count", 0L);
                return m;
            });
            row.put("output_count", ((Number) row.get("output_count")).longValue() + 1);
        }
        return new ArrayList<>(agg.values());
    }

    @Override
    public List<Map<String, Object>> videoOutputRanking(LocalDate startDate, LocalDate endDate, Integer limit) {
        Long tenantId = OaTenantSupport.requireTenantId();
        List<ContentDO> contents = contentMapper.selectList(new LambdaQueryWrapper<ContentDO>()
                .eq(ContentDO::getTenantId, tenantId)
                .eq(ContentDO::getContentType, "VIDEO")
                .ge(startDate != null, ContentDO::getPublishTime, startDate == null ? null : startDate.atStartOfDay())
                .le(endDate != null, ContentDO::getPublishTime, endDate == null ? null : endDate.plusDays(1).atStartOfDay()));
        Map<Long, String> accountNames = accountNameMap(tenantId);
        Map<Long, String> accountPlatforms = accountPlatformMap(tenantId);
        // 按账号聚合
        Map<Long, long[]> agg = new LinkedHashMap<>(); // [outputCount, readSum, topCount]
        for (ContentDO c : contents) {
            long[] a = agg.computeIfAbsent(c.getAccountId(), k -> new long[3]);
            a[0]++;
            a[1] += c.getReadCount() == null ? 0 : c.getReadCount();
            a[2] += (c.getIsHit() != null && c.getIsHit() == 1) ? 1 : 0;
        }
        List<Map<String, Object>> result = agg.entrySet().stream().map(e -> {
            long[] a = e.getValue();
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("account_id", e.getKey());
            m.put("account_name", accountNames.getOrDefault(e.getKey(), "账号#" + e.getKey()));
            m.put("platform", accountPlatforms.getOrDefault(e.getKey(), "-"));
            m.put("platform_type", accountPlatforms.getOrDefault(e.getKey(), "-"));
            m.put("output_count", a[0]);
            m.put("avg_view", a[0] == 0 ? 0 : a[1] / a[0]);
            m.put("top_count", a[2]);
            return m;
        }).sorted((x, y) -> Long.compare(((Number) y.get("output_count")).longValue(),
                ((Number) x.get("output_count")).longValue())).collect(Collectors.toList());
        int top = limit == null ? 10 : limit;
        return result.size() > top ? result.subList(0, top) : result;
    }

    @Override
    @AuditLog(module = "M7-report", action = "export-video-output")
    public ExportJobVO videoOutputExport(LocalDate startDate, LocalDate endDate) {
        return OaTenantSupport.stubExportJob();
    }

    // ==================== 2.4 直播时长（暂无真实直播表，生成确定性占位明细） ====================

    @Override
    public PageResult<Map<String, Object>> liveDurationList(Long ipGroupId, LocalDate startDate, LocalDate endDate,
                                                            Integer pageNum, Integer pageSize) {
        Long tenantId = OaTenantSupport.requireTenantId();
        LocalDate[] range = defaultRange(startDate, endDate);
        List<AccountDO> accounts = accountMapper.selectList(new LambdaQueryWrapper<AccountDO>()
                .eq(AccountDO::getTenantId, tenantId)
                .eq(ipGroupId != null, AccountDO::getIpGroupId, ipGroupId)
                .orderByDesc(AccountDO::getId)
                .last("LIMIT 30"));
        List<Map<String, Object>> rows = new ArrayList<>();
        for (AccountDO acc : accounts) {
            long seed = acc.getId();
            long sessions = 1 + (seed % 4);
            long totalDuration = sessions * (60 + (seed % 5) * 15); // 分钟
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("date", range[1]);
            m.put("account_id", acc.getId());
            m.put("account_name", acc.getAccountName());
            m.put("session_count", sessions);
            m.put("total_duration", totalDuration);
            m.put("avg_duration", sessions == 0 ? 0 : totalDuration / sessions);
            m.put("peak_viewers", 100 + (seed % 9) * 50);
            rows.add(m);
        }
        return new PageResult<>(rows, (long) rows.size());
    }

    @Override
    public List<Map<String, Object>> liveDurationTrend(Long ipGroupId, LocalDate startDate, LocalDate endDate) {
        LocalDate[] range = defaultRange(startDate, endDate);
        List<Map<String, Object>> points = new ArrayList<>();
        LocalDate d = range[0];
        while (!d.isAfter(range[1])) {
            Map<String, Object> p = new LinkedHashMap<>();
            long sessions = 1 + (d.getDayOfMonth() % 5);
            p.put("date", d.toString());
            p.put("stat_date", d.toString());
            p.put("session_count", sessions);
            p.put("total_duration", sessions * (60 + (d.getDayOfMonth() % 4) * 20));
            points.add(p);
            d = d.plusDays(1);
        }
        return points;
    }

    @Override
    @AuditLog(module = "M7-report", action = "export-live-duration")
    public ExportJobVO liveDurationExport(LocalDate startDate, LocalDate endDate) {
        return OaTenantSupport.stubExportJob();
    }

    // ==================== 2.5 账号成本分摊 ====================

    @Override
    public PageResult<Map<String, Object>> costAllocationList(Long accountId, LocalDate startDate, LocalDate endDate,
                                                              Integer pageNum, Integer pageSize) {
        Long tenantId = OaTenantSupport.requireTenantId();
        Page<AccountCostDO> page = accountCostMapper.selectPage(
                new Page<>(safePage(pageNum), safeSize(pageSize)),
                new LambdaQueryWrapper<AccountCostDO>()
                        .eq(AccountCostDO::getTenantId, tenantId)
                        .eq(accountId != null, AccountCostDO::getAccountId, accountId)
                        .ge(startDate != null, AccountCostDO::getPayDate, startDate)
                        .le(endDate != null, AccountCostDO::getPayDate, endDate)
                        .orderByDesc(AccountCostDO::getPayDate));
        Map<Long, String> accountNames = accountNameMap(tenantId);
        BigDecimal total = page.getRecords().stream()
                .map(r -> r.getAmount() == null ? BigDecimal.ZERO : r.getAmount())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return new PageResult<>(page.getRecords().stream().map(row -> {
            BigDecimal amount = row.getAmount() == null ? BigDecimal.ZERO : row.getAmount();
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("account_id", row.getAccountId());
            m.put("account_name", accountNames.getOrDefault(row.getAccountId(), "账号#" + row.getAccountId()));
            m.put("cost_type", row.getCostType());
            m.put("amount", amount);
            m.put("pay_date", row.getPayDate());
            m.put("share_ratio", total.compareTo(BigDecimal.ZERO) == 0 ? 0
                    : amount.divide(total, 4, RoundingMode.HALF_UP));
            m.put("remark", row.getRemark());
            return m;
        }).collect(Collectors.toList()), page.getTotal());
    }

    @Override
    @AuditLog(module = "M7-report", action = "export-cost-allocation")
    public ExportJobVO costAllocationExport(LocalDate startDate, LocalDate endDate) {
        return OaTenantSupport.stubExportJob();
    }

    // ==================== 2.6 ROI 分析 ====================

    @Override
    public PageResult<Map<String, Object>> roiList(Long ipGroupId, LocalDate startDate, LocalDate endDate,
                                                   Integer pageNum, Integer pageSize) {
        Long tenantId = OaTenantSupport.requireTenantId();
        Page<OrderAttributionDO> page = orderAttributionMapper.selectPage(
                new Page<>(safePage(pageNum), safeSize(pageSize)),
                new LambdaQueryWrapper<OrderAttributionDO>()
                        .eq(OrderAttributionDO::getTenantId, tenantId)
                        .eq(ipGroupId != null, OrderAttributionDO::getIpGroupId, ipGroupId)
                        .ge(startDate != null, OrderAttributionDO::getStatDate, startDate)
                        .le(endDate != null, OrderAttributionDO::getStatDate, endDate)
                        .orderByDesc(OrderAttributionDO::getStatDate));
        Map<Long, String> ipGroupNames = ipGroupNameMap(tenantId);
        Map<Long, String> accountPlatforms = accountPlatformMap(tenantId);
        return new PageResult<>(page.getRecords().stream().map(row -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("date", row.getStatDate());
            m.put("ip_group_name", row.getIpGroupId() == null ? "-" : ipGroupNames.getOrDefault(row.getIpGroupId(), "-"));
            m.put("platform", row.getAccountId() == null ? "-" : accountPlatforms.getOrDefault(row.getAccountId(), "-"));
            m.put("account_id", row.getAccountId());
            m.put("revenue", row.getRevenue());
            m.put("cost", row.getCost());
            m.put("roi", row.getRoi() != null ? row.getRoi() : roi(row.getRevenue(), row.getCost()));
            return m;
        }).collect(Collectors.toList()), page.getTotal());
    }

    @Override
    @AuditLog(module = "M7-report", action = "export-roi")
    public ExportJobVO roiExport(LocalDate startDate, LocalDate endDate) {
        return OaTenantSupport.stubExportJob();
    }

    // ==================== 2.7 IP 团队人员配置（按 IP 组聚合） ====================

    @Override
    public List<Map<String, Object>> teamConfigList(Long ipGroupId) {
        Long tenantId = OaTenantSupport.requireTenantId();
        List<IpGroupMemberDO> members = ipGroupMemberMapper.selectList(new LambdaQueryWrapper<IpGroupMemberDO>()
                .eq(IpGroupMemberDO::getTenantId, tenantId)
                .eq(ipGroupId != null, IpGroupMemberDO::getIpGroupId, ipGroupId));
        List<AccountDO> accounts = accountMapper.selectList(new LambdaQueryWrapper<AccountDO>()
                .eq(AccountDO::getTenantId, tenantId)
                .eq(ipGroupId != null, AccountDO::getIpGroupId, ipGroupId));
        Map<Long, String> ipGroupNames = ipGroupNameMap(tenantId);
        Map<Long, BigDecimal[]> finance = financeByIpGroup(tenantId, null, null);

        // 统计每个 IP 组人数 / 账号数
        Map<Long, Long> userCount = members.stream()
                .filter(m -> m.getIpGroupId() != null)
                .collect(Collectors.groupingBy(IpGroupMemberDO::getIpGroupId, Collectors.counting()));
        Map<Long, Long> accountCount = accounts.stream()
                .filter(a -> a.getIpGroupId() != null)
                .collect(Collectors.groupingBy(AccountDO::getIpGroupId, Collectors.counting()));

        List<Long> groupIds = new ArrayList<>();
        for (Long id : userCount.keySet()) {
            if (!groupIds.contains(id)) groupIds.add(id);
        }
        for (Long id : accountCount.keySet()) {
            if (!groupIds.contains(id)) groupIds.add(id);
        }

        List<Map<String, Object>> result = new ArrayList<>();
        for (Long gid : groupIds) {
            long users = userCount.getOrDefault(gid, 0L);
            long accts = accountCount.getOrDefault(gid, 0L);
            BigDecimal revenue = finance.getOrDefault(gid, new BigDecimal[]{BigDecimal.ZERO, BigDecimal.ZERO})[0];
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("ip_group_id", gid);
            m.put("ip_group_name", ipGroupNames.getOrDefault(gid, "IP组#" + gid));
            m.put("user_count", users);
            m.put("account_count", accts);
            m.put("avg_account_per_user", users == 0 ? 0
                    : BigDecimal.valueOf(accts).divide(BigDecimal.valueOf(users), 2, RoundingMode.HALF_UP));
            m.put("revenue_per_user", users == 0 ? BigDecimal.ZERO
                    : revenue.divide(BigDecimal.valueOf(users), 2, RoundingMode.HALF_UP));
            m.put("efficiency", accts == 0 ? BigDecimal.ZERO
                    : revenue.divide(BigDecimal.valueOf(accts), 2, RoundingMode.HALF_UP));
            result.add(m);
        }
        return result;
    }

    // ==================== 2.8 账号异常预警 ====================

    @Override
    public PageResult<Map<String, Object>> accountAlertList(LocalDate startDate, LocalDate endDate,
                                                            Integer pageNum, Integer pageSize) {
        Long tenantId = OaTenantSupport.requireTenantId();
        Page<AccountStatusLogDO> page = accountStatusLogMapper.selectPage(
                new Page<>(safePage(pageNum), safeSize(pageSize)),
                new LambdaQueryWrapper<AccountStatusLogDO>()
                        .eq(AccountStatusLogDO::getTenantId, tenantId)
                        .in(AccountStatusLogDO::getStatus, List.of("WARNING", "ABNORMAL", "OFFLINE"))
                        .ge(startDate != null, AccountStatusLogDO::getStatDate, startDate)
                        .le(endDate != null, AccountStatusLogDO::getStatDate, endDate)
                        .orderByDesc(AccountStatusLogDO::getStatDate));
        Map<Long, String> accountNames = accountNameMap(tenantId);
        return new PageResult<>(page.getRecords().stream().map(row -> {
            String status = row.getStatus() == null ? "" : row.getStatus();
            String level = switch (status) {
                case "OFFLINE" -> "CRITICAL";
                case "ABNORMAL" -> "HIGH";
                case "WARNING" -> "WARNING";
                default -> "INFO";
            };
            String type = switch (status) {
                case "OFFLINE" -> "ACCOUNT_OFFLINE";
                case "ABNORMAL" -> "DATA_ABNORMAL";
                default -> "FAN_FLUCTUATION";
            };
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("account_id", row.getAccountId());
            m.put("account_name", accountNames.getOrDefault(row.getAccountId(), "账号#" + row.getAccountId()));
            m.put("date", row.getStatDate());
            m.put("level", level);
            m.put("type", type);
            m.put("message", alertMessage(row.getStatus(), row.getFollowerCount()));
            // camelCase 兼容
            m.put("accountId", row.getAccountId());
            m.put("accountName", m.get("account_name"));
            m.put("alertLevel", level);
            m.put("alertType", type);
            return m;
        }).collect(Collectors.toList()), page.getTotal());
    }

    @Override
    @AuditLog(module = "M7-report", action = "export-account-alert")
    public ExportJobVO accountAlertExport(LocalDate startDate, LocalDate endDate) {
        return OaTenantSupport.stubExportJob();
    }

    // ==================== 私有辅助方法 ====================

    private int safePage(Integer pageNum) {
        return pageNum == null ? 1 : pageNum;
    }

    private int safeSize(Integer pageSize) {
        return pageSize == null ? 20 : pageSize;
    }

    private LocalDate[] defaultRange(LocalDate startDate, LocalDate endDate) {
        LocalDate end = endDate != null ? endDate : LocalDate.now();
        LocalDate start = startDate != null ? startDate : end.minusDays(89);
        return new LocalDate[]{start, end};
    }

    private BigDecimal roi(BigDecimal revenue, BigDecimal cost) {
        if (cost == null || cost.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        return (revenue == null ? BigDecimal.ZERO : revenue).divide(cost, 2, RoundingMode.HALF_UP);
    }

    private Map<Long, String> accountNameMap(Long tenantId) {
        return accountMapper.selectList(new LambdaQueryWrapper<AccountDO>().eq(AccountDO::getTenantId, tenantId))
                .stream().collect(Collectors.toMap(AccountDO::getId,
                        a -> a.getAccountName() == null ? ("账号#" + a.getId()) : a.getAccountName(), (x, y) -> x));
    }

    private Map<Long, String> accountPlatformMap(Long tenantId) {
        return accountMapper.selectList(new LambdaQueryWrapper<AccountDO>().eq(AccountDO::getTenantId, tenantId))
                .stream().collect(Collectors.toMap(AccountDO::getId,
                        a -> a.getPlatformType() == null ? "-" : a.getPlatformType(), (x, y) -> x));
    }

    private Map<Long, String> ipGroupNameMap(Long tenantId) {
        return ipGroupMapper.selectList(new LambdaQueryWrapper<IpGroupDO>().eq(IpGroupDO::getTenantId, tenantId))
                .stream().collect(Collectors.toMap(IpGroupDO::getId,
                        g -> g.getGroupName() == null ? ("IP组#" + g.getId()) : g.getGroupName(), (x, y) -> x));
    }

    /** 返回 accountId -> [revenue, cost] */
    private Map<Long, BigDecimal[]> financeByAccount(Long tenantId, LocalDate startDate, LocalDate endDate) {
        List<OrderAttributionDO> rows = orderAttributionMapper.selectList(new LambdaQueryWrapper<OrderAttributionDO>()
                .eq(OrderAttributionDO::getTenantId, tenantId)
                .ge(startDate != null, OrderAttributionDO::getStatDate, startDate)
                .le(endDate != null, OrderAttributionDO::getStatDate, endDate));
        Map<Long, BigDecimal[]> result = new LinkedHashMap<>();
        for (OrderAttributionDO row : rows) {
            if (row.getAccountId() == null) {
                continue;
            }
            BigDecimal[] agg = result.computeIfAbsent(row.getAccountId(), k -> new BigDecimal[]{BigDecimal.ZERO, BigDecimal.ZERO});
            agg[0] = agg[0].add(row.getRevenue() == null ? BigDecimal.ZERO : row.getRevenue());
            agg[1] = agg[1].add(row.getCost() == null ? BigDecimal.ZERO : row.getCost());
        }
        return result;
    }

    /** 返回 ipGroupId -> [revenue, cost] */
    private Map<Long, BigDecimal[]> financeByIpGroup(Long tenantId, LocalDate startDate, LocalDate endDate) {
        List<OrderAttributionDO> rows = orderAttributionMapper.selectList(new LambdaQueryWrapper<OrderAttributionDO>()
                .eq(OrderAttributionDO::getTenantId, tenantId)
                .ge(startDate != null, OrderAttributionDO::getStatDate, startDate)
                .le(endDate != null, OrderAttributionDO::getStatDate, endDate));
        Map<Long, BigDecimal[]> result = new LinkedHashMap<>();
        for (OrderAttributionDO row : rows) {
            if (row.getIpGroupId() == null) {
                continue;
            }
            BigDecimal[] agg = result.computeIfAbsent(row.getIpGroupId(), k -> new BigDecimal[]{BigDecimal.ZERO, BigDecimal.ZERO});
            agg[0] = agg[0].add(row.getRevenue() == null ? BigDecimal.ZERO : row.getRevenue());
            agg[1] = agg[1].add(row.getCost() == null ? BigDecimal.ZERO : row.getCost());
        }
        return result;
    }

    private long latestFollower(Long tenantId, Long accountId) {
        FollowerDailyDO latest = followerDailyMapper.selectOne(new LambdaQueryWrapper<FollowerDailyDO>()
                .eq(FollowerDailyDO::getTenantId, tenantId)
                .eq(FollowerDailyDO::getAccountId, accountId)
                .orderByDesc(FollowerDailyDO::getStatDate)
                .last("LIMIT 1"));
        return latest == null || latest.getFollowerCount() == null ? 0L : latest.getFollowerCount();
    }

    private long contentCount(Long tenantId, Long accountId) {
        return contentMapper.selectCount(new LambdaQueryWrapper<ContentDO>()
                .eq(ContentDO::getTenantId, tenantId)
                .eq(ContentDO::getAccountId, accountId));
    }

    private List<ContentDO> queryContents(Long tenantId, Long accountId) {
        return contentMapper.selectList(new LambdaQueryWrapper<ContentDO>()
                .eq(ContentDO::getTenantId, tenantId)
                .eq(accountId != null, ContentDO::getAccountId, accountId));
    }

    private Map<String, Object> contentRow(ContentDO row, Map<Long, String> accountNames) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("content_id", row.getId());
        m.put("title", row.getTitle());
        m.put("account_id", row.getAccountId());
        m.put("account_name", accountNames.getOrDefault(row.getAccountId(), "账号#" + row.getAccountId()));
        m.put("platform_type", row.getPlatformType());
        m.put("date", row.getPublishTime() == null ? null : row.getPublishTime().toLocalDate());
        m.put("output_count", 1);
        m.put("read_count", row.getReadCount());
        m.put("like_count", row.getLikeCount());
        return m;
    }

    private String statusRemark(String status, Long followerCount) {
        if (status == null) {
            return "";
        }
        return switch (status) {
            case "WARNING", "ABNORMAL" -> "粉丝波动异常，当前粉丝 " + (followerCount == null ? 0 : followerCount);
            case "OFFLINE" -> "账号掉线，请检查登录状态";
            case "RECOVERED" -> "状态已恢复";
            default -> "运行正常";
        };
    }

    private String alertMessage(String status, Long followerCount) {
        return switch (status == null ? "" : status) {
            case "OFFLINE" -> "账号掉线告警，请尽快处理";
            case "ABNORMAL" -> "账号数据异常告警";
            default -> "粉丝波动预警，当前粉丝 " + (followerCount == null ? 0 : followerCount);
        };
    }
}
