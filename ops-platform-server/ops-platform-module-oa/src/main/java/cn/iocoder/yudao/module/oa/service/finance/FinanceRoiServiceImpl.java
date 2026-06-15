package cn.iocoder.yudao.module.oa.service.finance;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.oa.api.dto.finance.FinanceRoiAnalysisVO;
import cn.iocoder.yudao.module.oa.api.dto.finance.FinanceRoiBreakdownVO;
import cn.iocoder.yudao.module.oa.api.dto.finance.FinanceRoiTrendVO;
import cn.iocoder.yudao.module.oa.api.dto.perf.ExportJobVO;
import cn.iocoder.yudao.module.oa.dal.dataobject.account.AccountDO;
import cn.iocoder.yudao.module.oa.dal.dataobject.finance.AccountCostDO;
import cn.iocoder.yudao.module.oa.dal.dataobject.ipgroup.IpGroupDO;
import cn.iocoder.yudao.module.oa.dal.dataobject.perf.OrderAttributionDO;
import cn.iocoder.yudao.module.oa.dal.mysql.account.AccountMapper;
import cn.iocoder.yudao.module.oa.dal.mysql.finance.AccountCostMapper;
import cn.iocoder.yudao.module.oa.dal.mysql.ipgroup.IpGroupMapper;
import cn.iocoder.yudao.module.oa.dal.mysql.perf.OrderAttributionMapper;
import cn.iocoder.yudao.module.oa.framework.audit.AuditLog;
import cn.iocoder.yudao.module.oa.service.support.OaTenantSupport;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FinanceRoiServiceImpl implements FinanceRoiService {

    private final OrderAttributionMapper orderAttributionMapper;
    private final AccountCostMapper accountCostMapper;
    private final AccountMapper accountMapper;
    private final IpGroupMapper ipGroupMapper;

    @Override
    public FinanceRoiAnalysisVO analysis(LocalDate startDate, LocalDate endDate, Long ipGroupId, Long accountId, String dimension) {
        Long tenantId = OaTenantSupport.requireTenantId();
        OaTenantSupport.requireDateRange(startDate, endDate);
        List<OrderAttributionDO> attrs = queryAttributions(tenantId, startDate, endDate, ipGroupId, accountId);
        List<AccountCostDO> costs = queryCosts(tenantId, startDate, endDate, accountId);

        BigDecimal totalRevenue = sumRevenue(attrs);
        BigDecimal attrCost = attrs.stream().map(OrderAttributionDO::getCost).filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal accountCost = costs.stream().map(AccountCostDO::getAmount).filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalCost = attrCost.add(accountCost);

        FinanceRoiAnalysisVO vo = new FinanceRoiAnalysisVO();
        vo.setTotalRevenue(totalRevenue);
        vo.setTotalCost(totalCost);
        vo.setRoi(calcRoi(totalRevenue, totalCost));

        String dim = StrUtil.blankToDefault(dimension, "IP_GROUP");
        if ("ACCOUNT".equalsIgnoreCase(dim)) {
            vo.setDetails(buildAccountDetails(attrs, costs));
        } else if ("PERSON".equalsIgnoreCase(dim)) {
            vo.setDetails(buildPersonDetails(attrs));
        } else {
            vo.setDetails(buildIpGroupDetails(attrs, costs));
        }
        return vo;
    }

    @Override
    public FinanceRoiTrendVO trend(LocalDate startDate, LocalDate endDate, Long ipGroupId, Long accountId) {
        Long tenantId = OaTenantSupport.requireTenantId();
        OaTenantSupport.requireDateRange(startDate, endDate);
        List<OrderAttributionDO> attrs = queryAttributions(tenantId, startDate, endDate, ipGroupId, accountId);
        Map<LocalDate, BigDecimal> revenueByDate = attrs.stream()
                .collect(Collectors.groupingBy(OrderAttributionDO::getStatDate,
                        Collectors.mapping(OrderAttributionDO::getRevenue,
                                Collectors.reducing(BigDecimal.ZERO, v -> v == null ? BigDecimal.ZERO : v, BigDecimal::add))));
        Map<LocalDate, BigDecimal> costByDate = attrs.stream()
                .collect(Collectors.groupingBy(OrderAttributionDO::getStatDate,
                        Collectors.mapping(OrderAttributionDO::getCost,
                                Collectors.reducing(BigDecimal.ZERO, v -> v == null ? BigDecimal.ZERO : v, BigDecimal::add))));
        List<AccountCostDO> accountCosts = queryCosts(tenantId, startDate, endDate, accountId);
        Map<LocalDate, BigDecimal> accountCostByDate = accountCosts.stream()
                .filter(c -> c.getPayDate() != null)
                .collect(Collectors.groupingBy(AccountCostDO::getPayDate,
                        Collectors.mapping(AccountCostDO::getAmount,
                                Collectors.reducing(BigDecimal.ZERO, v -> v == null ? BigDecimal.ZERO : v, BigDecimal::add))));

        FinanceRoiTrendVO vo = new FinanceRoiTrendVO();
        LocalDate d = startDate;
        while (!d.isAfter(endDate)) {
            FinanceRoiTrendVO.TrendPoint point = new FinanceRoiTrendVO.TrendPoint();
            point.setStatDate(d);
            point.setRevenue(revenueByDate.getOrDefault(d, BigDecimal.ZERO));
            BigDecimal attrCost = costByDate.getOrDefault(d, BigDecimal.ZERO);
            BigDecimal ledgerCost = accountCostByDate.getOrDefault(d, BigDecimal.ZERO);
            point.setCost(attrCost.add(ledgerCost));
            point.setRoi(calcRoi(point.getRevenue(), point.getCost()));
            vo.getPoints().add(point);
            d = d.plusDays(1);
        }
        return vo;
    }

    @Override
    public FinanceRoiBreakdownVO breakdown(LocalDate startDate, LocalDate endDate, Long ipGroupId, Long accountId) {
        Long tenantId = OaTenantSupport.requireTenantId();
        OaTenantSupport.requireDateRange(startDate, endDate);
        List<AccountCostDO> costs = queryCosts(tenantId, startDate, endDate, accountId);
        Map<String, BigDecimal> byType = costs.stream()
                .collect(Collectors.groupingBy(AccountCostDO::getCostType,
                        Collectors.mapping(AccountCostDO::getAmount,
                                Collectors.reducing(BigDecimal.ZERO, v -> v == null ? BigDecimal.ZERO : v, BigDecimal::add))));
        BigDecimal total = byType.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add);

        FinanceRoiBreakdownVO vo = new FinanceRoiBreakdownVO();
        vo.setPurchase(byType.getOrDefault("PURCHASE", BigDecimal.ZERO));
        vo.setProcess(byType.getOrDefault("PROCESS_HUMAN", BigDecimal.ZERO).add(byType.getOrDefault("AD_SPEND", BigDecimal.ZERO)));
        byType.forEach((type, amount) -> {
            FinanceRoiBreakdownVO.CostTypeItem item = new FinanceRoiBreakdownVO.CostTypeItem();
            item.setType(type);
            item.setTypeLabel(type);
            item.setAmount(amount);
            item.setPercentage(total.compareTo(BigDecimal.ZERO) == 0 ? BigDecimal.ZERO
                    : amount.multiply(BigDecimal.valueOf(100)).divide(total, 2, RoundingMode.HALF_UP));
            vo.getByType().add(item);
        });
        return vo;
    }

    @Override
    @AuditLog(module = "M5-finance-roi", action = "export")
    public ExportJobVO export(LocalDate startDate, LocalDate endDate) {
        OaTenantSupport.requireDateRange(startDate, endDate);
        return OaTenantSupport.stubExportJob();
    }

    private List<OrderAttributionDO> queryAttributions(Long tenantId, LocalDate start, LocalDate end,
                                                       Long ipGroupId, Long accountId) {
        return orderAttributionMapper.selectList(new LambdaQueryWrapper<OrderAttributionDO>()
                .eq(OrderAttributionDO::getTenantId, tenantId)
                .eq(ipGroupId != null, OrderAttributionDO::getIpGroupId, ipGroupId)
                .eq(accountId != null, OrderAttributionDO::getAccountId, accountId)
                .ge(OrderAttributionDO::getStatDate, start)
                .le(OrderAttributionDO::getStatDate, end));
    }

    private List<AccountCostDO> queryCosts(Long tenantId, LocalDate start, LocalDate end, Long accountId) {
        return accountCostMapper.selectList(new LambdaQueryWrapper<AccountCostDO>()
                .eq(AccountCostDO::getTenantId, tenantId)
                .eq(accountId != null, AccountCostDO::getAccountId, accountId)
                .ge(AccountCostDO::getPayDate, start)
                .le(AccountCostDO::getPayDate, end));
    }

    private BigDecimal sumRevenue(List<OrderAttributionDO> attrs) {
        return attrs.stream().map(OrderAttributionDO::getRevenue).filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private List<FinanceRoiAnalysisVO.RoiDetailItem> buildIpGroupDetails(List<OrderAttributionDO> attrs, List<AccountCostDO> costs) {
        Map<Long, List<OrderAttributionDO>> grouped = attrs.stream()
                .filter(a -> a.getIpGroupId() != null)
                .collect(Collectors.groupingBy(OrderAttributionDO::getIpGroupId));
        if (grouped.isEmpty()) {
            return new ArrayList<>();
        }
        Map<Long, String> names = ipGroupMapper.selectBatchIds(grouped.keySet()).stream()
                .collect(Collectors.toMap(IpGroupDO::getId, IpGroupDO::getGroupName, (a, b) -> a));
        List<FinanceRoiAnalysisVO.RoiDetailItem> details = new ArrayList<>();
        grouped.forEach((gid, rows) -> {
            FinanceRoiAnalysisVO.RoiDetailItem item = new FinanceRoiAnalysisVO.RoiDetailItem();
            item.setId(gid);
            item.setName(names.get(gid));
            item.setRevenue(sumRevenue(rows));
            item.setCost(rows.stream().map(OrderAttributionDO::getCost).filter(Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add));
            item.setRoi(calcRoi(item.getRevenue(), item.getCost()));
            details.add(item);
        });
        return details;
    }

    private List<FinanceRoiAnalysisVO.RoiDetailItem> buildAccountDetails(List<OrderAttributionDO> attrs, List<AccountCostDO> costs) {
        Map<Long, BigDecimal> revenue = new HashMap<>();
        Map<Long, BigDecimal> cost = new HashMap<>();
        attrs.forEach(a -> {
            if (a.getAccountId() != null) {
                revenue.merge(a.getAccountId(), a.getRevenue() == null ? BigDecimal.ZERO : a.getRevenue(), BigDecimal::add);
                cost.merge(a.getAccountId(), a.getCost() == null ? BigDecimal.ZERO : a.getCost(), BigDecimal::add);
            }
        });
        costs.forEach(c -> {
            if (c.getAccountId() != null) {
                cost.merge(c.getAccountId(), c.getAmount() == null ? BigDecimal.ZERO : c.getAmount(), BigDecimal::add);
                revenue.putIfAbsent(c.getAccountId(), BigDecimal.ZERO);
            }
        });
        Set<Long> accountIds = new HashSet<>(revenue.keySet());
        accountIds.addAll(cost.keySet());
        if (accountIds.isEmpty()) {
            return new ArrayList<>();
        }
        Map<Long, String> names = accountMapper.selectBatchIds(accountIds).stream()
                .collect(Collectors.toMap(AccountDO::getId, AccountDO::getAccountName, (a, b) -> a));
        List<FinanceRoiAnalysisVO.RoiDetailItem> details = new ArrayList<>();
        accountIds.forEach(aid -> {
            FinanceRoiAnalysisVO.RoiDetailItem item = new FinanceRoiAnalysisVO.RoiDetailItem();
            item.setId(aid);
            item.setName(names.get(aid));
            item.setRevenue(revenue.getOrDefault(aid, BigDecimal.ZERO));
            item.setCost(cost.getOrDefault(aid, BigDecimal.ZERO));
            item.setRoi(calcRoi(item.getRevenue(), item.getCost()));
            details.add(item);
        });
        return details;
    }

    private List<FinanceRoiAnalysisVO.RoiDetailItem> buildPersonDetails(List<OrderAttributionDO> attrs) {
        Map<Long, List<OrderAttributionDO>> grouped = attrs.stream()
                .filter(a -> a.getOpsUserId() != null)
                .collect(Collectors.groupingBy(OrderAttributionDO::getOpsUserId));
        List<FinanceRoiAnalysisVO.RoiDetailItem> details = new ArrayList<>();
        grouped.forEach((uid, rows) -> {
            FinanceRoiAnalysisVO.RoiDetailItem item = new FinanceRoiAnalysisVO.RoiDetailItem();
            item.setId(uid);
            item.setName("用户-" + uid);
            item.setRevenue(sumRevenue(rows));
            item.setCost(rows.stream().map(OrderAttributionDO::getCost).filter(Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add));
            item.setRoi(calcRoi(item.getRevenue(), item.getCost()));
            details.add(item);
        });
        return details;
    }

    private BigDecimal calcRoi(BigDecimal revenue, BigDecimal cost) {
        if (cost == null || cost.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        return revenue.divide(cost, 2, RoundingMode.HALF_UP);
    }
}
