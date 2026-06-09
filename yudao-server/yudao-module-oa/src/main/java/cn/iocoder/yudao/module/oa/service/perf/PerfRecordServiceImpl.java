package cn.iocoder.yudao.module.oa.service.perf;

import cn.iocoder.yudao.framework.common.exception.OaErrorCodes;
import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.oa.api.dto.perf.PerfRecordAdjustReq;
import cn.iocoder.yudao.module.oa.api.dto.perf.PerfRecordCalculateReq;
import cn.iocoder.yudao.module.oa.api.dto.perf.PerfRecordConfirmReq;
import cn.iocoder.yudao.module.oa.api.dto.perf.PerfRecordCreateReq;
import cn.iocoder.yudao.module.oa.api.dto.perf.PerfRecordDetailVO;
import cn.iocoder.yudao.module.oa.api.dto.perf.PerfRecordItemDetailVO;
import cn.iocoder.yudao.module.oa.api.dto.perf.PerfRecordVO;
import cn.iocoder.yudao.module.oa.api.dto.perf.ScoreStandardDTO;
import cn.iocoder.yudao.module.oa.dal.dataobject.auth.SysUserDO;
import cn.iocoder.yudao.module.oa.dal.dataobject.operations.ContentDO;
import cn.iocoder.yudao.module.oa.dal.dataobject.perf.MetricDO;
import cn.iocoder.yudao.module.oa.dal.dataobject.perf.OrderAttributionDO;
import cn.iocoder.yudao.module.oa.dal.dataobject.perf.PerfItemRecordDO;
import cn.iocoder.yudao.module.oa.dal.dataobject.perf.PerfRecordDO;
import cn.iocoder.yudao.module.oa.dal.dataobject.perf.PerfTemplateDO;
import cn.iocoder.yudao.module.oa.dal.dataobject.perf.PerfTemplateItemDO;
import cn.iocoder.yudao.module.oa.dal.mysql.auth.SysUserMapper;
import cn.iocoder.yudao.module.oa.dal.mysql.operations.ContentMapper;
import cn.iocoder.yudao.module.oa.dal.mysql.perf.MetricMapper;
import cn.iocoder.yudao.module.oa.dal.mysql.perf.OrderAttributionMapper;
import cn.iocoder.yudao.module.oa.dal.mysql.perf.PerfItemRecordMapper;
import cn.iocoder.yudao.module.oa.dal.mysql.perf.PerfRecordMapper;
import cn.iocoder.yudao.module.oa.dal.mysql.perf.PerfTemplateItemMapper;
import cn.iocoder.yudao.module.oa.framework.audit.AuditLog;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PerfRecordServiceImpl implements PerfRecordService {

    private final PerfRecordMapper perfRecordMapper;
    private final PerfItemRecordMapper perfItemRecordMapper;
    private final PerfTemplateService perfTemplateService;
    private final PerfTemplateItemMapper perfTemplateItemMapper;
    private final MetricMapper metricMapper;
    private final SysUserMapper sysUserMapper;
    private final OrderAttributionMapper orderAttributionMapper;
    private final ContentMapper contentMapper;

    @Override
    public PageResult<PerfRecordVO> list(Long ipGroupId, Long targetUserId, String periodType, String status,
                                         Integer pageNum, Integer pageSize) {
        Long tenantId = requireTenantId();
        LambdaQueryWrapper<PerfRecordDO> wrapper = new LambdaQueryWrapper<PerfRecordDO>()
                .eq(PerfRecordDO::getTenantId, tenantId)
                .eq(ipGroupId != null, PerfRecordDO::getIpGroupId, ipGroupId)
                .eq(targetUserId != null, PerfRecordDO::getTargetUserId, targetUserId)
                .eq(periodType != null && !periodType.isBlank(), PerfRecordDO::getPeriodType, periodType)
                .eq(status != null && !status.isBlank(), PerfRecordDO::getStatus, status)
                .orderByDesc(PerfRecordDO::getId);
        Page<PerfRecordDO> page = perfRecordMapper.selectPage(
                new Page<>(pageNum == null ? 1 : pageNum, pageSize == null ? 20 : pageSize), wrapper);
        Map<Long, String> userNames = loadUserNames(page.getRecords());
        return new PageResult<>(page.getRecords().stream()
                .map(record -> toVO(record, userNames))
                .collect(Collectors.toList()), page.getTotal());
    }

    @Override
    @Transactional
    @AuditLog(module = "M3-perf", action = "create-record")
    public Long create(PerfRecordCreateReq req) {
        Long tenantId = requireTenantId();
        SysUserDO user = requireEnabledUser(req.getTargetUserId(), tenantId);
        long duplicate = perfRecordMapper.selectCount(new LambdaQueryWrapper<PerfRecordDO>()
                .eq(PerfRecordDO::getTenantId, tenantId)
                .eq(PerfRecordDO::getTargetUserId, req.getTargetUserId())
                .eq(PerfRecordDO::getPeriodType, req.getPeriodType())
                .eq(PerfRecordDO::getPeriodStart, req.getPeriodStart())
                .eq(PerfRecordDO::getPeriodEnd, req.getPeriodEnd()));
        if (duplicate > 0) {
            throw new ServiceException(OaErrorCodes.PERF_DUPLICATE_PERIOD);
        }
        PerfTemplateDO template = perfTemplateService.findActiveByPosition(user.getPosition());
        if (template == null) {
            throw new ServiceException(OaErrorCodes.PERF_NO_TEMPLATE);
        }
        PerfRecordDO entity = new PerfRecordDO();
        entity.setTenantId(tenantId);
        entity.setTemplateId(template.getId());
        entity.setTargetUserId(req.getTargetUserId());
        entity.setIpGroupId(user.getIpGroupId());
        entity.setPeriodType(req.getPeriodType());
        entity.setPeriodStart(req.getPeriodStart());
        entity.setPeriodEnd(req.getPeriodEnd());
        entity.setStatus("DRAFT");
        entity.setCreator(TenantContextHolder.getUsername());
        entity.setUpdater(TenantContextHolder.getUsername());
        entity.setCreateTime(LocalDateTime.now());
        entity.setUpdateTime(LocalDateTime.now());
        perfRecordMapper.insert(entity);
        return entity.getId();
    }

    @Override
    @Transactional
    @AuditLog(module = "M3-perf", action = "calculate-record")
    public void calculate(PerfRecordCalculateReq req) {
        PerfRecordDO record = requireRecord(req.getRecordId());
        requireDraft(record);
        List<PerfTemplateItemDO> templateItems = perfTemplateItemMapper.selectList(
                new LambdaQueryWrapper<PerfTemplateItemDO>()
                        .eq(PerfTemplateItemDO::getTemplateId, record.getTemplateId()));
        perfItemRecordMapper.delete(new LambdaQueryWrapper<PerfItemRecordDO>()
                .eq(PerfItemRecordDO::getRecordId, record.getId()));

        BigDecimal totalScore = BigDecimal.ZERO;
        for (PerfTemplateItemDO templateItem : templateItems) {
            MetricDO metric = metricMapper.selectById(templateItem.getMetricId());
            BigDecimal metricValue = resolveMetricValue(metric, record);
            ScoreStandardDTO standard = PerfScoreSupport.parseStandard(templateItem.getScoreStandardJson());
            PerfScoreSupport.ScoreResult scoreResult = PerfScoreSupport.resolveScore(metricValue, standard);

            PerfItemRecordDO itemRecord = new PerfItemRecordDO();
            itemRecord.setRecordId(record.getId());
            itemRecord.setMetricId(templateItem.getMetricId());
            itemRecord.setMetricValue(metricValue);
            itemRecord.setScore(scoreResult.score());
            itemRecord.setManualAdjustment(BigDecimal.ZERO);
            itemRecord.setFinalScore(scoreResult.score());
            itemRecord.setCreator(TenantContextHolder.getUsername());
            itemRecord.setUpdater(TenantContextHolder.getUsername());
            itemRecord.setCreateTime(LocalDateTime.now());
            itemRecord.setUpdateTime(LocalDateTime.now());
            perfItemRecordMapper.insert(itemRecord);

            BigDecimal weighted = scoreResult.score()
                    .multiply(templateItem.getWeight())
                    .divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
            totalScore = totalScore.add(weighted);
        }

        record.setTotalScore(totalScore);
        record.setGrade(PerfScoreSupport.resolveGrade(totalScore));
        record.setUpdater(TenantContextHolder.getUsername());
        record.setUpdateTime(LocalDateTime.now());
        perfRecordMapper.updateById(record);
    }

    @Override
    @Transactional
    @AuditLog(module = "M3-perf", action = "adjust-record")
    public void adjust(PerfRecordAdjustReq req) {
        PerfItemRecordDO itemRecord = perfItemRecordMapper.selectById(req.getItemRecordId());
        if (itemRecord == null) {
            throw new ServiceException(OaErrorCodes.ENTITY_NOT_EXISTS);
        }
        PerfRecordDO record = requireRecord(itemRecord.getRecordId());
        requireDraft(record);
        if (itemRecord.getScore() == null || itemRecord.getScore().compareTo(BigDecimal.ZERO) == 0) {
            throw new ServiceException(OaErrorCodes.PERF_ADJUST_LIMIT);
        }
        BigDecimal ratio = req.getManualAdjustment().abs()
                .divide(itemRecord.getScore().abs(), 4, RoundingMode.HALF_UP);
        if (ratio.compareTo(new BigDecimal("0.20")) > 0) {
            throw new ServiceException(OaErrorCodes.PERF_ADJUST_LIMIT);
        }
        itemRecord.setManualAdjustment(req.getManualAdjustment());
        itemRecord.setFinalScore(itemRecord.getScore().add(req.getManualAdjustment()));
        itemRecord.setRemark(req.getRemark());
        itemRecord.setUpdater(TenantContextHolder.getUsername());
        itemRecord.setUpdateTime(LocalDateTime.now());
        perfItemRecordMapper.updateById(itemRecord);
        recalculateTotal(record);
    }

    @Override
    public PerfRecordDetailVO detail(Long id) {
        PerfRecordDO record = requireRecord(id);
        SysUserDO user = sysUserMapper.selectById(record.getTargetUserId());
        List<PerfItemRecordDO> itemRecords = perfItemRecordMapper.selectList(
                new LambdaQueryWrapper<PerfItemRecordDO>()
                        .eq(PerfItemRecordDO::getRecordId, record.getId()));
        Map<Long, String> metricNames = metricMapper.selectBatchIds(
                        itemRecords.stream().map(PerfItemRecordDO::getMetricId).distinct().toList())
                .stream()
                .collect(Collectors.toMap(MetricDO::getId, MetricDO::getMetricName, (a, b) -> a));
        Map<Long, BigDecimal> weights = perfTemplateItemMapper.selectList(
                        new LambdaQueryWrapper<PerfTemplateItemDO>()
                                .eq(PerfTemplateItemDO::getTemplateId, record.getTemplateId()))
                .stream()
                .collect(Collectors.toMap(PerfTemplateItemDO::getMetricId, PerfTemplateItemDO::getWeight, (a, b) -> a));

        PerfRecordDetailVO vo = new PerfRecordDetailVO();
        vo.setId(record.getId());
        vo.setTargetUserName(user != null ? user.getNickname() : null);
        vo.setPeriodType(record.getPeriodType());
        vo.setPeriodStart(record.getPeriodStart());
        vo.setPeriodEnd(record.getPeriodEnd());
        vo.setTotalScore(record.getTotalScore());
        vo.setGrade(record.getGrade());
        vo.setStatus(record.getStatus());
        vo.setItems(itemRecords.stream().map(item -> {
            PerfRecordItemDetailVO itemVO = new PerfRecordItemDetailVO();
            itemVO.setId(item.getId());
            itemVO.setMetricName(metricNames.get(item.getMetricId()));
            itemVO.setWeight(weights.get(item.getMetricId()));
            itemVO.setMetricValue(item.getMetricValue());
            itemVO.setScore(item.getScore());
            itemVO.setManualAdjustment(item.getManualAdjustment());
            itemVO.setFinalScore(item.getFinalScore());
            return itemVO;
        }).collect(Collectors.toList()));
        return vo;
    }

    @Override
    @Transactional
    @AuditLog(module = "M3-perf", action = "confirm-record")
    public void confirm(PerfRecordConfirmReq req) {
        PerfRecordDO record = requireRecord(req.getId());
        requireDraft(record);
        record.setStatus("CONFIRMED");
        record.setUpdater(TenantContextHolder.getUsername());
        record.setUpdateTime(LocalDateTime.now());
        perfRecordMapper.updateById(record);
    }

    private void recalculateTotal(PerfRecordDO record) {
        List<PerfItemRecordDO> itemRecords = perfItemRecordMapper.selectList(
                new LambdaQueryWrapper<PerfItemRecordDO>()
                        .eq(PerfItemRecordDO::getRecordId, record.getId()));
        Map<Long, BigDecimal> weights = perfTemplateItemMapper.selectList(
                        new LambdaQueryWrapper<PerfTemplateItemDO>()
                                .eq(PerfTemplateItemDO::getTemplateId, record.getTemplateId()))
                .stream()
                .collect(Collectors.toMap(PerfTemplateItemDO::getMetricId, PerfTemplateItemDO::getWeight, (a, b) -> a));
        BigDecimal total = BigDecimal.ZERO;
        for (PerfItemRecordDO item : itemRecords) {
            BigDecimal weight = weights.getOrDefault(item.getMetricId(), BigDecimal.ZERO);
            BigDecimal finalScore = item.getFinalScore() != null ? item.getFinalScore() : BigDecimal.ZERO;
            total = total.add(finalScore.multiply(weight).divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP));
        }
        record.setTotalScore(total);
        record.setGrade(PerfScoreSupport.resolveGrade(total));
        record.setUpdater(TenantContextHolder.getUsername());
        record.setUpdateTime(LocalDateTime.now());
        perfRecordMapper.updateById(record);
    }

    private BigDecimal resolveMetricValue(MetricDO metric, PerfRecordDO record) {
        if (metric == null) {
            return BigDecimal.ZERO;
        }
        Long tenantId = record.getTenantId();
        LocalDate start = record.getPeriodStart();
        LocalDate end = record.getPeriodEnd();
        return switch (metric.getMetricCode()) {
            case "REVENUE" -> sumAttribution(tenantId, record.getTargetUserId(), start, end, true);
            case "ROI" -> {
                BigDecimal revenue = sumAttribution(tenantId, record.getTargetUserId(), start, end, true);
                BigDecimal cost = sumAttribution(tenantId, record.getTargetUserId(), start, end, false);
                yield cost.compareTo(BigDecimal.ZERO) == 0 ? BigDecimal.ZERO
                        : revenue.divide(cost, 4, RoundingMode.HALF_UP);
            }
            case "POST_COUNT" -> BigDecimal.valueOf(countContent(tenantId, record.getIpGroupId(), start, end));
            default -> new BigDecimal("50");
        };
    }

    private BigDecimal sumAttribution(Long tenantId, Long opsUserId, LocalDate start, LocalDate end, boolean revenue) {
        List<OrderAttributionDO> rows = orderAttributionMapper.selectList(new LambdaQueryWrapper<OrderAttributionDO>()
                .eq(OrderAttributionDO::getTenantId, tenantId)
                .eq(OrderAttributionDO::getOpsUserId, opsUserId)
                .ge(OrderAttributionDO::getStatDate, start)
                .le(OrderAttributionDO::getStatDate, end));
        return rows.stream()
                .map(row -> revenue ? row.getRevenue() : row.getCost())
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private long countContent(Long tenantId, Long ipGroupId, LocalDate start, LocalDate end) {
        LambdaQueryWrapper<ContentDO> wrapper = new LambdaQueryWrapper<ContentDO>()
                .eq(ContentDO::getTenantId, tenantId)
                .ge(ContentDO::getPublishTime, start.atStartOfDay())
                .le(ContentDO::getPublishTime, end.atTime(LocalTime.MAX));
        if (ipGroupId != null) {
            // 通过账号关联 IP 组：简化按 account 在同期有内容的计数
            wrapper.inSql(ContentDO::getAccountId,
                    "SELECT id FROM oa_account WHERE tenant_id = " + tenantId + " AND ip_group_id = " + ipGroupId);
        }
        Long count = contentMapper.selectCount(wrapper);
        return count == null ? 0L : count;
    }

    private PerfRecordVO toVO(PerfRecordDO record, Map<Long, String> userNames) {
        PerfRecordVO vo = new PerfRecordVO();
        vo.setId(record.getId());
        vo.setTargetUserId(record.getTargetUserId());
        vo.setTargetUserName(userNames.get(record.getTargetUserId()));
        vo.setTemplateId(record.getTemplateId());
        vo.setIpGroupId(record.getIpGroupId());
        vo.setPeriodType(record.getPeriodType());
        vo.setPeriodStart(record.getPeriodStart());
        vo.setPeriodEnd(record.getPeriodEnd());
        vo.setTotalScore(record.getTotalScore());
        vo.setGrade(record.getGrade());
        vo.setStatus(record.getStatus());
        vo.setCreateTime(record.getCreateTime());
        return vo;
    }

    private Map<Long, String> loadUserNames(List<PerfRecordDO> records) {
        List<Long> userIds = records.stream().map(PerfRecordDO::getTargetUserId).distinct().toList();
        if (userIds.isEmpty()) {
            return Map.of();
        }
        return sysUserMapper.selectBatchIds(userIds).stream()
                .collect(Collectors.toMap(SysUserDO::getId, SysUserDO::getNickname, (a, b) -> a));
    }

    private SysUserDO requireEnabledUser(Long userId, Long tenantId) {
        SysUserDO user = sysUserMapper.selectById(userId);
        if (user == null || !Objects.equals(user.getTenantId(), tenantId)) {
            throw new ServiceException(OaErrorCodes.ENTITY_NOT_EXISTS);
        }
        if (!"ENABLED".equals(user.getStatus())) {
            throw new ServiceException(OaErrorCodes.ENTITY_DISABLED);
        }
        return user;
    }

    private PerfRecordDO requireRecord(Long id) {
        PerfRecordDO record = perfRecordMapper.selectById(id);
        if (record == null) {
            throw new ServiceException(OaErrorCodes.ENTITY_NOT_EXISTS);
        }
        if (!Objects.equals(record.getTenantId(), requireTenantId())) {
            throw new ServiceException(OaErrorCodes.TENANT_FORBIDDEN);
        }
        return record;
    }

    private void requireDraft(PerfRecordDO record) {
        if (!"DRAFT".equals(record.getStatus())) {
            throw new ServiceException(OaErrorCodes.PERF_RECORD_NOT_DRAFT);
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
