package cn.iocoder.yudao.module.oa.service.operations;

import cn.iocoder.yudao.framework.common.exception.OaErrorCodes;
import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.oa.api.dto.operations.ProductivityReviewDetailVO;
import cn.iocoder.yudao.module.oa.api.dto.operations.ProductivityReviewVO;
import cn.iocoder.yudao.module.oa.dal.dataobject.auth.SysUserDO;
import cn.iocoder.yudao.module.oa.dal.dataobject.ipgroup.IpGroupDO;
import cn.iocoder.yudao.module.oa.dal.dataobject.perf.OrderAttributionDO;
import cn.iocoder.yudao.module.oa.dal.dataobject.sop.TaskDO;
import cn.iocoder.yudao.module.oa.dal.mysql.auth.SysUserMapper;
import cn.iocoder.yudao.module.oa.dal.mysql.ipgroup.IpGroupMapper;
import cn.iocoder.yudao.module.oa.dal.mysql.operations.ContentMapper;
import cn.iocoder.yudao.module.oa.dal.mysql.perf.OrderAttributionMapper;
import cn.iocoder.yudao.module.oa.dal.mysql.sop.TaskMapper;
import cn.iocoder.yudao.module.oa.framework.audit.AuditLog;
import cn.iocoder.yudao.module.oa.framework.auth.DataScopeSupport;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProductivityReviewServiceImpl implements ProductivityReviewService {

    private final SysUserMapper sysUserMapper;
    private final IpGroupMapper ipGroupMapper;
    private final TaskMapper taskMapper;
    private final OrderAttributionMapper orderAttributionMapper;
    private final ContentMapper contentMapper;

    @Override
    public PageResult<ProductivityReviewVO> list(LocalDate startDate, LocalDate endDate,
                                                 String timeDimension,
                                                 Long ipGroupId, Long userId, String position,
                                                 String keyword,
                                                 Integer page, Integer size) {
        Long tenantId = requireTenantId();
        // S-R9 B1: 查 sys_user 列表（支持 ipGroupId / userId / position / keyword 过滤）
        LambdaQueryWrapper<SysUserDO> userWrapper = new LambdaQueryWrapper<SysUserDO>()
                .eq(SysUserDO::getTenantId, tenantId)
                .eq(ipGroupId != null, SysUserDO::getIpGroupId, ipGroupId)
                .eq(userId != null, SysUserDO::getId, userId)
                .eq(position != null && !position.isEmpty(), SysUserDO::getPosition, position)
                .like(keyword != null && !keyword.isEmpty(), SysUserDO::getNickname, keyword)
                .orderByAsc(SysUserDO::getId);
        DataScopeSupport.applyIpGroupScope(userWrapper, SysUserDO::getIpGroupId);
        Page<SysUserDO> userPage = sysUserMapper.selectPage(
                new Page<>(page == null ? 1 : page, size == null ? 20 : size), userWrapper);
        if (userPage.getRecords().isEmpty()) {
            return PageResult.empty();
        }
        List<Long> userIds = userPage.getRecords().stream().map(SysUserDO::getId).collect(Collectors.toList());

        // S-R9 B2: 一次 SQL 聚合 task + order_attribution KPI（按 user）
        Map<Long, Map<String, Object>> taskAgg = sumTask(tenantId, userIds, startDate, endDate);
        Map<Long, Map<String, Object>> orderAgg = sumOrder(tenantId, userIds, startDate, endDate);
        Map<Long, Map<String, Object>> contentAgg = sumContent(tenantId, userIds, startDate, endDate);

        List<ProductivityReviewVO> voList = userPage.getRecords().stream()
                .map(u -> toVO(u, taskAgg.get(u.getId()), orderAgg.get(u.getId()), contentAgg.get(u.getId())))
                .collect(Collectors.toList());
        return new PageResult<>(voList, userPage.getTotal());
    }

    @Override
    public ProductivityReviewDetailVO detail(Long userId, LocalDate startDate, LocalDate endDate) {
        Long tenantId = requireTenantId();
        SysUserDO user = sysUserMapper.selectById(userId);
        if (user == null || !user.getTenantId().equals(tenantId)) {
            throw new ServiceException(OaErrorCodes.ENTITY_NOT_EXISTS.getCode(), "经办人不存在");
        }
        ProductivityReviewDetailVO vo = new ProductivityReviewDetailVO();
        // 摘要
        Map<String, Object> taskRow = sumTask(tenantId, Collections.singletonList(userId), startDate, endDate)
                .get(userId);
        Map<String, Object> orderRow = sumOrder(tenantId, Collections.singletonList(userId), startDate, endDate)
                .get(userId);
        Map<String, Object> contentRow = sumContent(tenantId, Collections.singletonList(userId), startDate, endDate)
                .get(userId);
        vo.setSummary(toVO(user, taskRow, orderRow, contentRow));

        // 任务列表（按 status 拆分）
        LambdaQueryWrapper<TaskDO> taskWrapper = new LambdaQueryWrapper<TaskDO>()
                .eq(TaskDO::getTenantId, tenantId)
                .eq(TaskDO::getAssigneeId, userId)
                .orderByDesc(TaskDO::getCreateTime)
                .last("LIMIT 50");
        vo.setTaskList(taskMapper.selectList(taskWrapper).stream().map(t -> {
            java.util.Map<String, Object> m = new java.util.HashMap<>();
            m.put("id", t.getId());
            m.put("planName", t.getPlanName());
            m.put("status", t.getStatus());
            m.put("startTime", t.getStartTime());
            m.put("completeTime", t.getCompleteTime());
            m.put("slaDeadline", t.getSlaDeadline());
            return m;
        }).collect(Collectors.toList()));

        // 财务对比（按 IP 组聚合）
        LambdaQueryWrapper<OrderAttributionDO> orderWrapper = new LambdaQueryWrapper<OrderAttributionDO>()
                .eq(OrderAttributionDO::getTenantId, tenantId)
                .eq(OrderAttributionDO::getOpsUserId, userId)
                .ge(startDate != null, OrderAttributionDO::getStatDate, startDate)
                .le(endDate != null, OrderAttributionDO::getStatDate, endDate);
        List<OrderAttributionDO> orders = orderAttributionMapper.selectList(orderWrapper);
        Map<Long, List<OrderAttributionDO>> byIpGroup = orders.stream()
                .filter(o -> o.getIpGroupId() != null)
                .collect(Collectors.groupingBy(OrderAttributionDO::getIpGroupId));
        vo.setFinanceByGroup(byIpGroup.entrySet().stream().map(e -> {
            java.util.Map<String, Object> m = new java.util.HashMap<>();
            IpGroupDO group = ipGroupMapper.selectById(e.getKey());
            m.put("ipGroupId", e.getKey());
            m.put("ipGroupName", group != null ? group.getGroupName() : null);
            BigDecimal rev = e.getValue().stream().map(OrderAttributionDO::getRevenue)
                    .filter(java.util.Objects::nonNull).reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal cost = e.getValue().stream().map(OrderAttributionDO::getCost)
                    .filter(java.util.Objects::nonNull).reduce(BigDecimal.ZERO, BigDecimal::add);
            m.put("revenue", rev);
            m.put("cost", cost);
            m.put("roi", cost.compareTo(BigDecimal.ZERO) == 0 ? BigDecimal.ZERO
                    : rev.divide(cost, 2, RoundingMode.HALF_UP));
            return m;
        }).collect(Collectors.toList()));

        // S-R21-Mike：内容指标按 oa_content.author_id 聚合
        vo.setContentMetrics(java.util.Map.of(
                "publishCount", contentRow != null ? toInt(contentRow.get("contentOutput")) : 0,
                "avgReading", contentRow != null ? toLong(contentRow.get("avgRead")) : 0L,
                "hotCount", contentRow != null ? toInt(contentRow.get("hitCount")) : 0
        ));

        // 趋势（按日营收/任务完成数）
        vo.setTrend(buildTrend(tenantId, userId, startDate, endDate));
        return vo;
    }

    @Override
    public List<ProductivityReviewVO> anchors(Long ipGroupId, LocalDate startDate, LocalDate endDate) {
        Long tenantId = requireTenantId();
        if (ipGroupId == null) {
            return Collections.emptyList();
        }
        LambdaQueryWrapper<SysUserDO> wrapper = new LambdaQueryWrapper<SysUserDO>()
                .eq(SysUserDO::getTenantId, tenantId)
                .eq(SysUserDO::getIpGroupId, ipGroupId);
        List<SysUserDO> users = sysUserMapper.selectList(wrapper);
        if (users.isEmpty()) {
            return Collections.emptyList();
        }
        List<Long> userIds = users.stream().map(SysUserDO::getId).collect(Collectors.toList());
        Map<Long, Map<String, Object>> taskAgg = sumTask(tenantId, userIds, startDate, endDate);
        Map<Long, Map<String, Object>> orderAgg = sumOrder(tenantId, userIds, startDate, endDate);
        Map<Long, Map<String, Object>> contentAgg = sumContent(tenantId, userIds, startDate, endDate);
        return users.stream()
                .map(u -> toVO(u, taskAgg.get(u.getId()), orderAgg.get(u.getId()), contentAgg.get(u.getId())))
                .collect(Collectors.toList());
    }

    @Override
    @AuditLog(module = "M1-productivity-review", action = "export")
    public String exportCsv(LocalDate startDate, LocalDate endDate, String timeDimension,
                            Long ipGroupId, Long userId, String position, String keyword) {
        // 简化：直接复用 list 拿全量（不传分页）
        PageResult<ProductivityReviewVO> page = list(startDate, endDate, timeDimension, ipGroupId, userId, position, keyword, 1, 10000);
        StringBuilder sb = new StringBuilder();
        sb.append("\uFEFF"); // UTF-8 BOM
        sb.append("用户ID,用户,岗位,IP组,任务总数,已完成,进行中,超时,完成率,营收,成本,ROI,订单数\n");
        for (ProductivityReviewVO v : page.getList()) {
            sb.append(v.getUserId()).append(',')
              .append(safeCsv(v.getUserName())).append(',')
              .append(safeCsv(v.getPosition())).append(',')
              .append(safeCsv(v.getIpGroupName())).append(',')
              .append(v.getTaskTotal()).append(',')
              .append(v.getTaskCompleted()).append(',')
              .append(v.getTaskInProgress()).append(',')
              .append(v.getTaskOverdue()).append(',')
              .append(v.getCompletionRate()).append(',')
              .append(v.getRevenue()).append(',')
              .append(v.getCostAmount()).append(',')
              .append(v.getRoi()).append(',')
              .append(v.getOrderCount()).append('\n');
        }
        return sb.toString();
    }

    private String safeCsv(String s) {
        if (s == null) return "";
        if (s.contains(",") || s.contains("\n") || s.contains("\"")) {
            return "\"" + s.replace("\"", "\"\"") + "\"";
        }
        return s;
    }

    // ==================== 私有助手 ====================

    private Map<Long, Map<String, Object>> sumTask(Long tenantId, List<Long> userIds,
                                                    LocalDate startDate, LocalDate endDate) {
        if (userIds.isEmpty()) return Collections.emptyMap();
        LocalDateTime startTime = startDate != null ? startDate.atStartOfDay() : null;
        LocalDateTime endTime = endDate != null ? endDate.atTime(LocalTime.MAX) : null;
        List<Map<String, Object>> rows = taskMapper.sumByUser(tenantId, userIds, startTime, endTime, LocalDateTime.now());
        return rows.stream().collect(Collectors.toMap(
                r -> toLong(r.get("userId")),
                r -> r,
                (a, b) -> a));
    }

    private Map<Long, Map<String, Object>> sumOrder(Long tenantId, List<Long> userIds,
                                                    LocalDate startDate, LocalDate endDate) {
        if (userIds.isEmpty()) return Collections.emptyMap();
        List<Map<String, Object>> rows = orderAttributionMapper.sumByUser(tenantId, userIds, startDate, endDate);
        return rows.stream().collect(Collectors.toMap(
                r -> toLong(r.get("userId")),
                r -> r,
                (a, b) -> a));
    }

    private Map<Long, Map<String, Object>> sumContent(Long tenantId, List<Long> userIds,
                                                      LocalDate startDate, LocalDate endDate) {
        if (userIds.isEmpty()) {
            return Collections.emptyMap();
        }
        LocalDateTime startTime = startDate != null ? startDate.atStartOfDay() : null;
        LocalDateTime endTime = endDate != null ? endDate.atTime(LocalTime.MAX) : null;
        List<Map<String, Object>> rows = contentMapper.sumByUser(tenantId, userIds, startTime, endTime);
        return rows.stream().collect(Collectors.toMap(
                r -> toLong(r.get("userId")),
                r -> r,
                (a, b) -> a));
    }

    private List<Map<String, Object>> buildTrend(Long tenantId, Long userId,
                                                 LocalDate startDate, LocalDate endDate) {
        LocalDate from = startDate != null ? startDate : LocalDate.now().minusDays(29);
        LocalDate to = endDate != null ? endDate : LocalDate.now();
        LambdaQueryWrapper<OrderAttributionDO> wrapper = new LambdaQueryWrapper<OrderAttributionDO>()
                .eq(OrderAttributionDO::getTenantId, tenantId)
                .eq(OrderAttributionDO::getOpsUserId, userId)
                .between(OrderAttributionDO::getStatDate, from, to);
        return orderAttributionMapper.selectList(wrapper).stream()
                .collect(Collectors.groupingBy(OrderAttributionDO::getStatDate))
                .entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(e -> {
                    java.util.Map<String, Object> m = new java.util.HashMap<>();
                    m.put("date", e.getKey().toString());
                    BigDecimal rev = e.getValue().stream().map(OrderAttributionDO::getRevenue)
                            .filter(java.util.Objects::nonNull).reduce(BigDecimal.ZERO, BigDecimal::add);
                    m.put("revenue", rev);
                    m.put("orderCount", e.getValue().size());
                    return m;
                }).collect(Collectors.toList());
    }

    private ProductivityReviewVO toVO(SysUserDO user, Map<String, Object> taskRow,
                                      Map<String, Object> orderRow, Map<String, Object> contentRow) {
        ProductivityReviewVO vo = new ProductivityReviewVO();
        vo.setUserId(user.getId());
        vo.setUserName(user.getNickname() != null ? user.getNickname() : user.getUsername());
        vo.setPosition(user.getPosition());
        vo.setIpGroupId(user.getIpGroupId());
        if (user.getIpGroupId() != null) {
            IpGroupDO group = ipGroupMapper.selectById(user.getIpGroupId());
            if (group != null) {
                vo.setIpGroupName(group.getGroupName());
            }
        }
        if (taskRow != null) {
            int total = toInt(taskRow.get("total"));
            int completed = toInt(taskRow.get("completed"));
            vo.setTaskTotal(total);
            vo.setTaskCompleted(completed);
            vo.setTaskInProgress(toInt(taskRow.get("inProgress")));
            vo.setTaskOverdue(toInt(taskRow.get("overdue")));
            vo.setCompletionRate(total == 0 ? 0.0 : Math.round(completed * 10000.0 / total) / 100.0);
        }
        if (orderRow != null) {
            double rev = toDouble(orderRow.get("revenue"));
            double cost = toDouble(orderRow.get("cost"));
            vo.setRevenue(rev);
            vo.setCostAmount(cost);
            vo.setRoi(cost == 0 ? 0.0 : Math.round(rev / cost * 100.0) / 100.0);
            vo.setOrderCount(toInt(orderRow.get("orderCount")));
        }
        if (contentRow != null) {
            vo.setContentOutput(toInt(contentRow.get("contentOutput")));
            vo.setAvgRead(toLong(contentRow.get("avgRead")));
            vo.setAvgPlay(toLong(contentRow.get("avgPlay")));
            vo.setHitCount(toInt(contentRow.get("hitCount")));
        }
        return vo;
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

    private static Double toDouble(Object v) {
        if (v == null) return 0.0;
        if (v instanceof Number n) return n.doubleValue();
        try { return Double.parseDouble(v.toString()); } catch (Exception e) { return 0.0; }
    }

    private Long requireTenantId() {
        Long tenantId = TenantContextHolder.getTenantId();
        if (tenantId == null) {
            throw new ServiceException(OaErrorCodes.UNAUTHORIZED.getCode(), "缺少租户上下文");
        }
        return tenantId;
    }
}
