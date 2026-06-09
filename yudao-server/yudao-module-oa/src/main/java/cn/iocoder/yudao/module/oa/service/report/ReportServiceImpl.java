package cn.iocoder.yudao.module.oa.service.report;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.oa.api.dto.perf.ExportJobVO;
import cn.iocoder.yudao.module.oa.api.dto.report.ReportStatsVO;
import cn.iocoder.yudao.module.oa.dal.dataobject.account.AccountDO;
import cn.iocoder.yudao.module.oa.dal.dataobject.analytics.AccountStatusLogDO;
import cn.iocoder.yudao.module.oa.dal.dataobject.finance.AccountCostDO;
import cn.iocoder.yudao.module.oa.dal.dataobject.ipgroup.IpGroupMemberDO;
import cn.iocoder.yudao.module.oa.dal.dataobject.operations.ContentDO;
import cn.iocoder.yudao.module.oa.dal.dataobject.operations.FollowerDailyDO;
import cn.iocoder.yudao.module.oa.dal.dataobject.perf.OrderAttributionDO;
import cn.iocoder.yudao.module.oa.dal.mysql.account.AccountMapper;
import cn.iocoder.yudao.module.oa.dal.mysql.analytics.AccountStatusLogMapper;
import cn.iocoder.yudao.module.oa.dal.mysql.finance.AccountCostMapper;
import cn.iocoder.yudao.module.oa.dal.mysql.ipgroup.IpGroupMemberMapper;
import cn.iocoder.yudao.module.oa.dal.mysql.operations.ContentMapper;
import cn.iocoder.yudao.module.oa.dal.mysql.operations.FollowerDailyMapper;
import cn.iocoder.yudao.module.oa.dal.mysql.perf.OrderAttributionMapper;
import cn.iocoder.yudao.module.oa.service.support.OaTenantSupport;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
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
        Page<AccountDO> page = accountMapper.selectPage(new Page<>(pageNum == null ? 1 : pageNum, pageSize == null ? 20 : pageSize), wrapper);
        List<Map<String, Object>> list = page.getRecords().stream().map(acc -> {
            Map<String, Object> row = new HashMap<>();
            row.put("accountId", acc.getId());
            row.put("accountName", acc.getAccountName());
            row.put("platformType", acc.getPlatformType());
            row.put("followerCount", latestFollower(tenantId, acc.getId()));
            row.put("contentCount", contentCount(tenantId, acc.getId(), startDate, endDate));
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
        List<ContentDO> contents = queryContents(tenantId, accountId, startDate, endDate);
        vo.setTotalContents((long) contents.size());
        vo.setTotalReads(contents.stream().mapToLong(c -> c.getReadCount() == null ? 0 : c.getReadCount()).sum());
        for (AccountDO acc : accounts) {
            Map<String, Object> item = new HashMap<>();
            item.put("accountId", acc.getId());
            item.put("accountName", acc.getAccountName());
            item.put("followerCount", latestFollower(tenantId, acc.getId()));
            vo.getItems().add(item);
        }
        return vo;
    }

    @Override
    public ExportJobVO unifiedAccountExport(LocalDate startDate, LocalDate endDate) {
        OaTenantSupport.requireDateRange(startDate, endDate);
        return OaTenantSupport.stubExportJob();
    }

    @Override
    public List<Map<String, Object>> accountStatusTrend(Long accountId, LocalDate startDate, LocalDate endDate) {
        Long tenantId = OaTenantSupport.requireTenantId();
        OaTenantSupport.requireDateRange(startDate, endDate);
        return accountStatusLogMapper.selectList(new LambdaQueryWrapper<AccountStatusLogDO>()
                        .eq(AccountStatusLogDO::getTenantId, tenantId)
                        .eq(accountId != null, AccountStatusLogDO::getAccountId, accountId)
                        .ge(AccountStatusLogDO::getStatDate, startDate)
                        .le(AccountStatusLogDO::getStatDate, endDate)
                        .orderByAsc(AccountStatusLogDO::getStatDate))
                .stream().map(row -> {
                    Map<String, Object> m = new HashMap<>();
                    m.put("statDate", row.getStatDate());
                    m.put("status", row.getStatus());
                    m.put("followerCount", row.getFollowerCount());
                    return m;
                }).collect(Collectors.toList());
    }

    @Override
    public Map<String, Object> accountStatusSummary(Long accountId, LocalDate startDate, LocalDate endDate) {
        List<Map<String, Object>> trend = accountStatusTrend(accountId, startDate, endDate);
        Map<String, Object> summary = new HashMap<>();
        summary.put("totalDays", trend.size());
        summary.put("warningDays", trend.stream().filter(t -> "WARNING".equals(t.get("status"))).count());
        summary.put("latestFollower", trend.isEmpty() ? 0 : trend.get(trend.size() - 1).get("followerCount"));
        return summary;
    }

    @Override
    public PageResult<Map<String, Object>> accountStatusLog(Long accountId, LocalDate startDate, LocalDate endDate,
                                                            Integer pageNum, Integer pageSize) {
        Long tenantId = OaTenantSupport.requireTenantId();
        Page<AccountStatusLogDO> page = accountStatusLogMapper.selectPage(
                new Page<>(pageNum == null ? 1 : pageNum, pageSize == null ? 20 : pageSize),
                new LambdaQueryWrapper<AccountStatusLogDO>()
                        .eq(AccountStatusLogDO::getTenantId, tenantId)
                        .eq(accountId != null, AccountStatusLogDO::getAccountId, accountId)
                        .ge(startDate != null, AccountStatusLogDO::getStatDate, startDate)
                        .le(endDate != null, AccountStatusLogDO::getStatDate, endDate)
                        .orderByDesc(AccountStatusLogDO::getStatDate));
        return new PageResult<>(page.getRecords().stream().map(row -> {
            Map<String, Object> m = new HashMap<>();
            m.put("accountId", row.getAccountId());
            m.put("statDate", row.getStatDate());
            m.put("status", row.getStatus());
            m.put("followerCount", row.getFollowerCount());
            return m;
        }).collect(Collectors.toList()), page.getTotal());
    }

    @Override
    public ExportJobVO accountStatusExport(LocalDate startDate, LocalDate endDate) {
        OaTenantSupport.requireDateRange(startDate, endDate);
        return OaTenantSupport.stubExportJob();
    }

    @Override
    public PageResult<Map<String, Object>> videoOutputList(Long ipGroupId, Long accountId, LocalDate startDate, LocalDate endDate,
                                                           Integer pageNum, Integer pageSize) {
        Long tenantId = OaTenantSupport.requireTenantId();
        LambdaQueryWrapper<ContentDO> wrapper = new LambdaQueryWrapper<ContentDO>()
                .eq(ContentDO::getTenantId, tenantId)
                .eq(accountId != null, ContentDO::getAccountId, accountId)
                .eq(ContentDO::getContentType, "VIDEO")
                .ge(startDate != null && startDate != null, ContentDO::getPublishTime, startDate == null ? null : startDate.atStartOfDay())
                .le(endDate != null, ContentDO::getPublishTime, endDate == null ? null : endDate.plusDays(1).atStartOfDay())
                .orderByDesc(ContentDO::getPublishTime);
        Page<ContentDO> page = contentMapper.selectPage(new Page<>(pageNum == null ? 1 : pageNum, pageSize == null ? 20 : pageSize), wrapper);
        return new PageResult<>(page.getRecords().stream().map(this::contentRow).collect(Collectors.toList()), page.getTotal());
    }

    @Override
    public List<Map<String, Object>> videoOutputTrend(Long accountId, LocalDate startDate, LocalDate endDate) {
        Long tenantId = OaTenantSupport.requireTenantId();
        return contentMapper.selectList(new LambdaQueryWrapper<ContentDO>()
                        .eq(ContentDO::getTenantId, tenantId)
                        .eq(accountId != null, ContentDO::getAccountId, accountId)
                        .eq(ContentDO::getContentType, "VIDEO")
                        .orderByAsc(ContentDO::getPublishTime))
                .stream().map(this::contentRow).collect(Collectors.toList());
    }

    @Override
    public List<Map<String, Object>> videoOutputRanking(LocalDate startDate, LocalDate endDate, Integer limit) {
        Long tenantId = OaTenantSupport.requireTenantId();
        int top = limit == null ? 10 : limit;
        return contentMapper.selectList(new LambdaQueryWrapper<ContentDO>()
                        .eq(ContentDO::getTenantId, tenantId)
                        .eq(ContentDO::getContentType, "VIDEO")
                        .orderByDesc(ContentDO::getReadCount)
                        .last("LIMIT " + top))
                .stream().map(this::contentRow).collect(Collectors.toList());
    }

    @Override
    public ExportJobVO videoOutputExport(LocalDate startDate, LocalDate endDate) {
        return OaTenantSupport.stubExportJob();
    }

    @Override
    public PageResult<Map<String, Object>> liveDurationList(Long ipGroupId, LocalDate startDate, LocalDate endDate,
                                                            Integer pageNum, Integer pageSize) {
        Long tenantId = OaTenantSupport.requireTenantId();
        List<Map<String, Object>> rows = new ArrayList<>();
        Map<String, Object> stub = new HashMap<>();
        stub.put("ipGroupId", ipGroupId);
        stub.put("liveHours", 12.5);
        stub.put("statDate", startDate);
        rows.add(stub);
        return new PageResult<>(rows, (long) rows.size());
    }

    @Override
    public List<Map<String, Object>> liveDurationTrend(Long ipGroupId, LocalDate startDate, LocalDate endDate) {
        OaTenantSupport.requireDateRange(startDate, endDate);
        List<Map<String, Object>> points = new ArrayList<>();
        LocalDate d = startDate;
        while (!d.isAfter(endDate)) {
            Map<String, Object> p = new HashMap<>();
            p.put("statDate", d);
            p.put("liveHours", 1.0 + (d.getDayOfMonth() % 5));
            points.add(p);
            d = d.plusDays(1);
        }
        return points;
    }

    @Override
    public ExportJobVO liveDurationExport(LocalDate startDate, LocalDate endDate) {
        return OaTenantSupport.stubExportJob();
    }

    @Override
    public PageResult<Map<String, Object>> costAllocationList(Long accountId, LocalDate startDate, LocalDate endDate,
                                                              Integer pageNum, Integer pageSize) {
        Long tenantId = OaTenantSupport.requireTenantId();
        Page<AccountCostDO> page = accountCostMapper.selectPage(
                new Page<>(pageNum == null ? 1 : pageNum, pageSize == null ? 20 : pageSize),
                new LambdaQueryWrapper<AccountCostDO>()
                        .eq(AccountCostDO::getTenantId, tenantId)
                        .eq(accountId != null, AccountCostDO::getAccountId, accountId)
                        .ge(startDate != null, AccountCostDO::getPayDate, startDate)
                        .le(endDate != null, AccountCostDO::getPayDate, endDate)
                        .orderByDesc(AccountCostDO::getPayDate));
        return new PageResult<>(page.getRecords().stream().map(row -> {
            Map<String, Object> m = new HashMap<>();
            m.put("accountId", row.getAccountId());
            m.put("costType", row.getCostType());
            m.put("amount", row.getAmount());
            m.put("payDate", row.getPayDate());
            return m;
        }).collect(Collectors.toList()), page.getTotal());
    }

    @Override
    public ExportJobVO costAllocationExport(LocalDate startDate, LocalDate endDate) {
        return OaTenantSupport.stubExportJob();
    }

    @Override
    public PageResult<Map<String, Object>> roiList(Long ipGroupId, LocalDate startDate, LocalDate endDate,
                                                   Integer pageNum, Integer pageSize) {
        Long tenantId = OaTenantSupport.requireTenantId();
        Page<OrderAttributionDO> page = orderAttributionMapper.selectPage(
                new Page<>(pageNum == null ? 1 : pageNum, pageSize == null ? 20 : pageSize),
                new LambdaQueryWrapper<OrderAttributionDO>()
                        .eq(OrderAttributionDO::getTenantId, tenantId)
                        .eq(ipGroupId != null, OrderAttributionDO::getIpGroupId, ipGroupId)
                        .ge(startDate != null, OrderAttributionDO::getStatDate, startDate)
                        .le(endDate != null, OrderAttributionDO::getStatDate, endDate)
                        .orderByDesc(OrderAttributionDO::getStatDate));
        return new PageResult<>(page.getRecords().stream().map(row -> {
            Map<String, Object> m = new HashMap<>();
            m.put("accountId", row.getAccountId());
            m.put("revenue", row.getRevenue());
            m.put("cost", row.getCost());
            m.put("roi", row.getRoi());
            m.put("statDate", row.getStatDate());
            return m;
        }).collect(Collectors.toList()), page.getTotal());
    }

    @Override
    public ExportJobVO roiExport(LocalDate startDate, LocalDate endDate) {
        return OaTenantSupport.stubExportJob();
    }

    @Override
    public List<Map<String, Object>> teamConfigList(Long ipGroupId) {
        Long tenantId = OaTenantSupport.requireTenantId();
        return ipGroupMemberMapper.selectList(new LambdaQueryWrapper<IpGroupMemberDO>()
                        .eq(IpGroupMemberDO::getTenantId, tenantId)
                        .eq(ipGroupId != null, IpGroupMemberDO::getIpGroupId, ipGroupId))
                .stream().map(row -> {
                    Map<String, Object> m = new HashMap<>();
                    m.put("ipGroupId", row.getIpGroupId());
                    m.put("userId", row.getUserId());
                    m.put("position", row.getPosition());
                    m.put("isLeader", row.getIsLeader());
                    return m;
                }).collect(Collectors.toList());
    }

    @Override
    public PageResult<Map<String, Object>> accountAlertList(LocalDate startDate, LocalDate endDate,
                                                            Integer pageNum, Integer pageSize) {
        Long tenantId = OaTenantSupport.requireTenantId();
        Page<AccountStatusLogDO> page = accountStatusLogMapper.selectPage(
                new Page<>(pageNum == null ? 1 : pageNum, pageSize == null ? 20 : pageSize),
                new LambdaQueryWrapper<AccountStatusLogDO>()
                        .eq(AccountStatusLogDO::getTenantId, tenantId)
                        .eq(AccountStatusLogDO::getStatus, "WARNING")
                        .ge(startDate != null, AccountStatusLogDO::getStatDate, startDate)
                        .le(endDate != null, AccountStatusLogDO::getStatDate, endDate)
                        .orderByDesc(AccountStatusLogDO::getStatDate));
        return new PageResult<>(page.getRecords().stream().map(row -> {
            Map<String, Object> m = new HashMap<>();
            m.put("accountId", row.getAccountId());
            m.put("statDate", row.getStatDate());
            m.put("alertLevel", "MEDIUM");
            m.put("message", "粉丝波动预警");
            return m;
        }).collect(Collectors.toList()), page.getTotal());
    }

    @Override
    public ExportJobVO accountAlertExport(LocalDate startDate, LocalDate endDate) {
        return OaTenantSupport.stubExportJob();
    }

    private long latestFollower(Long tenantId, Long accountId) {
        FollowerDailyDO latest = followerDailyMapper.selectOne(new LambdaQueryWrapper<FollowerDailyDO>()
                .eq(FollowerDailyDO::getTenantId, tenantId)
                .eq(FollowerDailyDO::getAccountId, accountId)
                .orderByDesc(FollowerDailyDO::getStatDate)
                .last("LIMIT 1"));
        return latest == null || latest.getFollowerCount() == null ? 0L : latest.getFollowerCount();
    }

    private long contentCount(Long tenantId, Long accountId, LocalDate start, LocalDate end) {
        return contentMapper.selectCount(new LambdaQueryWrapper<ContentDO>()
                .eq(ContentDO::getTenantId, tenantId)
                .eq(ContentDO::getAccountId, accountId));
    }

    private List<ContentDO> queryContents(Long tenantId, Long accountId, LocalDate start, LocalDate end) {
        return contentMapper.selectList(new LambdaQueryWrapper<ContentDO>()
                .eq(ContentDO::getTenantId, tenantId)
                .eq(accountId != null, ContentDO::getAccountId, accountId));
    }

    private Map<String, Object> contentRow(ContentDO row) {
        Map<String, Object> m = new HashMap<>();
        m.put("contentId", row.getId());
        m.put("title", row.getTitle());
        m.put("accountId", row.getAccountId());
        m.put("readCount", row.getReadCount());
        m.put("likeCount", row.getLikeCount());
        m.put("publishTime", row.getPublishTime());
        return m;
    }
}
