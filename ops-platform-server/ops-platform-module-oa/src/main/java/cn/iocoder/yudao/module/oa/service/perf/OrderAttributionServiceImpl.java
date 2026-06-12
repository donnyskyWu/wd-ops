package cn.iocoder.yudao.module.oa.service.perf;

import cn.iocoder.yudao.framework.common.exception.OaErrorCodes;
import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.oa.api.dto.perf.ExportJobVO;
import cn.iocoder.yudao.module.oa.api.dto.perf.OrderAttributionRoiVO;
import cn.iocoder.yudao.module.oa.api.dto.perf.OrderAttributionVO;
import cn.iocoder.yudao.module.oa.dal.dataobject.account.AccountDO;
import cn.iocoder.yudao.module.oa.dal.dataobject.auth.SysUserDO;
import cn.iocoder.yudao.module.oa.dal.dataobject.author.AuthorDO;
import cn.iocoder.yudao.module.oa.dal.dataobject.ipgroup.IpGroupDO;
import cn.iocoder.yudao.module.oa.dal.dataobject.perf.OrderAttributionDO;
import cn.iocoder.yudao.module.oa.dal.dataobject.perf.OrderDO;
import cn.iocoder.yudao.module.oa.dal.mysql.account.AccountMapper;
import cn.iocoder.yudao.module.oa.dal.mysql.auth.SysUserMapper;
import cn.iocoder.yudao.module.oa.dal.mysql.author.AuthorMapper;
import cn.iocoder.yudao.module.oa.dal.mysql.ipgroup.IpGroupMapper;
import cn.iocoder.yudao.module.oa.dal.mysql.perf.OrderAttributionMapper;
import cn.iocoder.yudao.module.oa.dal.mysql.perf.OrderMapper;
import cn.iocoder.yudao.module.oa.framework.audit.AuditLog;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OrderAttributionServiceImpl implements OrderAttributionService {

    private final OrderAttributionMapper orderAttributionMapper;
    private final OrderMapper orderMapper;
    private final AccountMapper accountMapper;
    private final IpGroupMapper ipGroupMapper;
    private final AuthorMapper authorMapper;
    private final SysUserMapper sysUserMapper;

    @Override
    public PageResult<OrderAttributionVO> list(Long ipGroupId, Long accountId, LocalDate startDate, LocalDate endDate,
                                             Integer pageNum, Integer pageSize) {
        Long tenantId = requireTenantId();
        requireDateRange(startDate, endDate);
        LambdaQueryWrapper<OrderAttributionDO> wrapper = new LambdaQueryWrapper<OrderAttributionDO>()
                .eq(OrderAttributionDO::getTenantId, tenantId)
                .eq(ipGroupId != null, OrderAttributionDO::getIpGroupId, ipGroupId)
                .eq(accountId != null, OrderAttributionDO::getAccountId, accountId)
                .ge(OrderAttributionDO::getStatDate, startDate)
                .le(OrderAttributionDO::getStatDate, endDate)
                .orderByDesc(OrderAttributionDO::getStatDate);
        Page<OrderAttributionDO> page = orderAttributionMapper.selectPage(
                new Page<>(pageNum == null ? 1 : pageNum, pageSize == null ? 20 : pageSize), wrapper);
        return new PageResult<>(page.getRecords().stream()
                .map(this::toVO)
                .collect(Collectors.toList()), page.getTotal());
    }

    @Override
    public OrderAttributionRoiVO roi(Long ipGroupId, Long accountId, LocalDate startDate, LocalDate endDate) {
        Long tenantId = requireTenantId();
        requireDateRange(startDate, endDate);
        LambdaQueryWrapper<OrderAttributionDO> wrapper = new LambdaQueryWrapper<OrderAttributionDO>()
                .eq(OrderAttributionDO::getTenantId, tenantId)
                .eq(ipGroupId != null, OrderAttributionDO::getIpGroupId, ipGroupId)
                .eq(accountId != null, OrderAttributionDO::getAccountId, accountId)
                .ge(OrderAttributionDO::getStatDate, startDate)
                .le(OrderAttributionDO::getStatDate, endDate);
        List<OrderAttributionDO> rows = orderAttributionMapper.selectList(wrapper);

        BigDecimal totalPay = rows.stream().map(OrderAttributionDO::getRevenue)
                .filter(Objects::nonNull).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalCost = rows.stream().map(OrderAttributionDO::getCost)
                .filter(Objects::nonNull).reduce(BigDecimal.ZERO, BigDecimal::add);
        OrderAttributionRoiVO vo = new OrderAttributionRoiVO();
        vo.setTotalPayAmount(totalPay);
        vo.setTotalInCost(totalCost);
        vo.setRoi(calcRoi(totalPay, totalCost));

        Map<Long, List<OrderAttributionDO>> byGroup = rows.stream()
                .filter(row -> row.getIpGroupId() != null)
                .collect(Collectors.groupingBy(OrderAttributionDO::getIpGroupId));
        Map<Long, String> groupNames = ipGroupMapper.selectBatchIds(byGroup.keySet()).stream()
                .collect(Collectors.toMap(IpGroupDO::getId, IpGroupDO::getGroupName, (a, b) -> a));
        vo.setByIpGroup(byGroup.entrySet().stream().map(entry -> {
            BigDecimal pay = entry.getValue().stream().map(OrderAttributionDO::getRevenue)
                    .filter(Objects::nonNull).reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal cost = entry.getValue().stream().map(OrderAttributionDO::getCost)
                    .filter(Objects::nonNull).reduce(BigDecimal.ZERO, BigDecimal::add);
            OrderAttributionRoiVO.IpGroupRoi groupRoi = new OrderAttributionRoiVO.IpGroupRoi();
            groupRoi.setIpGroupId(entry.getKey());
            groupRoi.setIpGroupName(groupNames.get(entry.getKey()));
            groupRoi.setPayAmount(pay);
            groupRoi.setInCost(cost);
            groupRoi.setRoi(calcRoi(pay, cost));
            return groupRoi;
        }).collect(Collectors.toList()));
        return vo;
    }

    @Override
    @AuditLog(module = "M3-order-attribution", action = "export")
    public ExportJobVO export(LocalDate startDate, LocalDate endDate) {
        requireDateRange(startDate, endDate);
        ExportJobVO vo = new ExportJobVO();
        vo.setJobId("order-attribution-export-" + UUID.randomUUID());
        return vo;
    }

    private OrderAttributionVO toVO(OrderAttributionDO row) {
        OrderAttributionVO vo = new OrderAttributionVO();
        vo.setId(row.getId());
        vo.setOrderId(row.getOrderId());
        OrderDO order = orderMapper.selectById(row.getOrderId());
        vo.setOrderNo(order != null ? order.getOrderNo() : null);
        vo.setAccountId(row.getAccountId());
        if (row.getAccountId() != null) {
            AccountDO account = accountMapper.selectById(row.getAccountId());
            vo.setAccountName(account != null ? account.getAccountName() : null);
        }
        vo.setIpGroupId(row.getIpGroupId());
        if (row.getIpGroupId() != null) {
            IpGroupDO group = ipGroupMapper.selectById(row.getIpGroupId());
            vo.setIpGroupName(group != null ? group.getGroupName() : null);
        }
        vo.setAuthorId(row.getAuthorId());
        if (row.getAuthorId() != null) {
            AuthorDO author = authorMapper.selectById(row.getAuthorId());
            vo.setAuthorName(author != null ? author.getAuthorName() : null);
        }
        vo.setOpsUserId(row.getOpsUserId());
        if (row.getOpsUserId() != null) {
            SysUserDO user = sysUserMapper.selectById(row.getOpsUserId());
            vo.setOpsUserName(user != null ? user.getNickname() : null);
        }
        vo.setRevenue(row.getRevenue());
        vo.setCost(row.getCost());
        vo.setRoi(row.getRoi());
        vo.setStatDate(row.getStatDate());
        return vo;
    }

    private BigDecimal calcRoi(BigDecimal pay, BigDecimal cost) {
        if (cost == null || cost.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        return pay.divide(cost, 2, RoundingMode.HALF_UP);
    }

    private void requireDateRange(LocalDate startDate, LocalDate endDate) {
        if (startDate == null || endDate == null) {
            throw new ServiceException(OaErrorCodes.DICT_VALUE_INVALID.getCode(), "startDate 与 endDate 必填");
        }
    }

    private Long requireTenantId() {
        Long tenantId = TenantContextHolder.getTenantId();
        if (tenantId == null) {
            throw new ServiceException(OaErrorCodes.UNAUTHORIZED.getCode(), "缺少租户上下文");
        }
        return tenantId;
    }
}
