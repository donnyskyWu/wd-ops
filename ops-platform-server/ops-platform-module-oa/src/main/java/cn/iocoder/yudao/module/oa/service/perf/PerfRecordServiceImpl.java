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
import cn.iocoder.yudao.module.oa.dal.dataobject.dict.SysDictDataDO;
import cn.iocoder.yudao.module.oa.dal.dataobject.ipgroup.IpGroupDO;
import cn.iocoder.yudao.module.oa.dal.dataobject.ipgroup.IpGroupMemberDO;
import cn.iocoder.yudao.module.oa.dal.dataobject.perf.MetricDO;
import cn.iocoder.yudao.module.oa.dal.dataobject.perf.PerfItemRecordDO;
import cn.iocoder.yudao.module.oa.dal.dataobject.perf.PerfRecordDO;
import cn.iocoder.yudao.module.oa.dal.dataobject.perf.PerfTemplateDO;
import cn.iocoder.yudao.module.oa.dal.dataobject.perf.PerfTemplateItemDO;
import cn.iocoder.yudao.module.oa.dal.mysql.ipgroup.IpGroupMapper;
import cn.iocoder.yudao.module.oa.dal.mysql.ipgroup.IpGroupMemberMapper;
import cn.iocoder.yudao.module.oa.dal.mysql.perf.MetricMapper;
import cn.iocoder.yudao.module.oa.dal.mysql.perf.PerfItemRecordMapper;
import cn.iocoder.yudao.module.oa.dal.mysql.perf.PerfRecordMapper;
import cn.iocoder.yudao.module.oa.dal.mysql.perf.PerfTemplateItemMapper;
import cn.iocoder.yudao.module.oa.dal.mysql.perf.PerfTemplateMapper;
import cn.iocoder.yudao.module.oa.framework.audit.AuditLog;
import cn.iocoder.yudao.module.oa.service.dict.DictService;
import cn.iocoder.yudao.module.oa.service.support.FootballSystemUserValidator;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PerfRecordServiceImpl implements PerfRecordService {

    private static final BigDecimal DEFAULT_BASE_SCORE = new BigDecimal("60");
    private static final BigDecimal DEFAULT_MAX_SCORE = new BigDecimal("100");
    private static final DateTimeFormatter DISPLAY_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final PerfRecordMapper perfRecordMapper;
    private final IpGroupMapper ipGroupMapper;
    private final IpGroupMemberMapper ipGroupMemberMapper;
    private final PerfTemplateMapper perfTemplateMapper;
    private final PerfItemRecordMapper perfItemRecordMapper;
    private final PerfTemplateService perfTemplateService;
    private final PerfTemplateItemMapper perfTemplateItemMapper;
    private final MetricMapper metricMapper;
    private final FootballSystemUserValidator footballSystemUserValidator;
    private final PerfMetricValueResolver perfMetricValueResolver;
    private final DictService dictService;

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
        Map<Long, String> positions = loadPositions(page.getRecords());
        Map<Long, String> ipGroupNames = loadIpGroupNames(page.getRecords());
        return new PageResult<>(page.getRecords().stream()
                .map(record -> toVO(record, userNames, positions, ipGroupNames))
                .collect(Collectors.toList()), page.getTotal());
    }

    @Override
    @Transactional
    @AuditLog(module = "M3-perf", action = "create-record")
    public Long create(PerfRecordCreateReq req) {
        Long tenantId = requireTenantId();
        assertIpGroupExists(req.getIpGroupId(), tenantId);
        footballSystemUserValidator.assertEnabledInTenant(req.getTargetUserId(), tenantId, "被考核人不存在");
        SysUserDO user = footballSystemUserValidator.findLegacyUser(req.getTargetUserId());
        long duplicate = perfRecordMapper.selectCount(new LambdaQueryWrapper<PerfRecordDO>()
                .eq(PerfRecordDO::getTenantId, tenantId)
                .eq(PerfRecordDO::getTargetUserId, req.getTargetUserId())
                .eq(PerfRecordDO::getPeriodType, req.getPeriodType())
                .eq(PerfRecordDO::getPeriodStart, req.getPeriodStart())
                .eq(PerfRecordDO::getPeriodEnd, req.getPeriodEnd()));
        if (duplicate > 0) {
            throw new ServiceException(OaErrorCodes.PERF_DUPLICATE_PERIOD);
        }
        PerfTemplateDO template = resolveTemplate(req.getTemplateId(), user);
        PerfRecordDO entity = new PerfRecordDO();
        entity.setTenantId(tenantId);
        entity.setTemplateId(template.getId());
        entity.setTargetUserId(req.getTargetUserId());
        entity.setIpGroupId(req.getIpGroupId());
        entity.setPeriodType(req.getPeriodType());
        entity.setPeriodStart(req.getPeriodStart());
        entity.setPeriodEnd(req.getPeriodEnd());
        entity.setStatus("DRAFT");
        entity.setCreator(TenantContextHolder.getUsername());
        entity.setUpdater(TenantContextHolder.getUsername());
        entity.setCreateTime(LocalDateTime.now());
        entity.setUpdateTime(LocalDateTime.now());
        perfRecordMapper.insert(entity);
        PerfRecordCalculateReq calcReq = new PerfRecordCalculateReq();
        calcReq.setRecordId(entity.getId());
        calculate(calcReq);
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
        PerfTemplateDO template = perfTemplateMapper.selectById(record.getTemplateId());
        List<PerfItemRecordDO> itemRecords = perfItemRecordMapper.selectList(
                new LambdaQueryWrapper<PerfItemRecordDO>()
                        .eq(PerfItemRecordDO::getRecordId, record.getId()));
        List<PerfTemplateItemDO> templateItems = perfTemplateItemMapper.selectList(
                new LambdaQueryWrapper<PerfTemplateItemDO>()
                        .eq(PerfTemplateItemDO::getTemplateId, record.getTemplateId()));
        Map<Long, MetricDO> metrics = loadMetrics(itemRecords);
        Map<Long, PerfTemplateItemDO> templateItemByMetric = templateItems.stream()
                .collect(Collectors.toMap(PerfTemplateItemDO::getMetricId, Function.identity(), (a, b) -> a));
        Map<Long, BigDecimal> weights = templateItems.stream()
                .collect(Collectors.toMap(PerfTemplateItemDO::getMetricId, PerfTemplateItemDO::getWeight, (a, b) -> a));

        PerfRecordDetailVO vo = new PerfRecordDetailVO();
        vo.setId(record.getId());
        vo.setTargetUserId(record.getTargetUserId());
        vo.setTemplateId(record.getTemplateId());
        vo.setTargetUserName(footballSystemUserValidator.resolveDisplayName(record.getTargetUserId()));
        vo.setTemplateName(template != null ? template.getTemplateName() : null);
        vo.setPosition(resolveRecordPosition(record));
        vo.setPeriodType(record.getPeriodType());
        vo.setPeriodStart(record.getPeriodStart());
        vo.setPeriodEnd(record.getPeriodEnd());
        vo.setTotalScore(record.getTotalScore());
        vo.setBaseScore(DEFAULT_BASE_SCORE);
        vo.setMaxScore(DEFAULT_MAX_SCORE);
        vo.setGrade(record.getGrade());
        vo.setStatus(record.getStatus());
        vo.setWorkflowStatus(resolveWorkflowStatus(record.getStatus()));
        applyWorkflowFields(vo, record);
        vo.setItems(itemRecords.stream().map(item -> {
            MetricDO metric = metrics.get(item.getMetricId());
            PerfTemplateItemDO templateItem = templateItemByMetric.get(item.getMetricId());
            PerfRecordItemDetailVO itemVO = new PerfRecordItemDetailVO();
            itemVO.setId(item.getId());
            itemVO.setMetricName(metric != null ? metric.getMetricName() : null);
            itemVO.setMetricCode(metric != null ? metric.getMetricCode() : null);
            itemVO.setWeight(weights.get(item.getMetricId()));
            itemVO.setMetricValue(item.getMetricValue());
            itemVO.setActualValue(item.getMetricValue());
            itemVO.setTarget(resolveTarget(templateItem));
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
        return perfMetricValueResolver.resolve(metric, record);
    }

    private PerfRecordVO toVO(PerfRecordDO record, Map<Long, String> userNames, Map<Long, String> positions,
                              Map<Long, String> ipGroupNames) {
        PerfRecordVO vo = new PerfRecordVO();
        vo.setId(record.getId());
        vo.setTargetUserId(record.getTargetUserId());
        vo.setTargetUserName(userNames.get(record.getTargetUserId()));
        vo.setPosition(positions.get(record.getId()));
        vo.setTemplateId(record.getTemplateId());
        vo.setIpGroupId(record.getIpGroupId());
        vo.setIpGroupName(record.getIpGroupId() == null ? null : ipGroupNames.get(record.getIpGroupId()));
        vo.setPeriodType(record.getPeriodType());
        vo.setPeriodStart(record.getPeriodStart());
        vo.setPeriodEnd(record.getPeriodEnd());
        vo.setTotalScore(record.getTotalScore());
        vo.setGrade(record.getGrade());
        vo.setStatus(record.getStatus());
        // 考核人 = 创建人（creator 存的是 username，直接展示）
        vo.setEvaluatorName(record.getCreator());
        vo.setCreateTime(record.getCreateTime());
        return vo;
    }

    private void assertIpGroupExists(Long ipGroupId, Long tenantId) {
        if (ipGroupId == null) {
            throw new ServiceException(OaErrorCodes.ENTITY_NOT_EXISTS.getCode(), "IP组不存在");
        }
        IpGroupDO group = ipGroupMapper.selectById(ipGroupId);
        if (group == null || !Objects.equals(group.getTenantId(), tenantId)) {
            throw new ServiceException(OaErrorCodes.ENTITY_NOT_EXISTS.getCode(), "IP组不存在");
        }
    }

    private Map<Long, String> loadIpGroupNames(List<PerfRecordDO> records) {
        List<Long> groupIds = records.stream()
                .map(PerfRecordDO::getIpGroupId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        if (groupIds.isEmpty()) {
            return Map.of();
        }
        return ipGroupMapper.selectBatchIds(groupIds).stream()
                .filter(group -> Objects.equals(group.getTenantId(), requireTenantId()))
                .collect(Collectors.toMap(IpGroupDO::getId, IpGroupDO::getGroupName, (a, b) -> a));
    }

    private Map<Long, String> loadPositions(List<PerfRecordDO> records) {
        if (records == null || records.isEmpty()) {
            return Map.of();
        }
        Long tenantId = requireTenantId();
        List<Long> groupIds = records.stream()
                .map(PerfRecordDO::getIpGroupId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        Map<String, String> memberPositionByGroupUser = new java.util.HashMap<>();
        if (!groupIds.isEmpty()) {
            List<IpGroupMemberDO> members = ipGroupMemberMapper.selectList(new LambdaQueryWrapper<IpGroupMemberDO>()
                    .eq(IpGroupMemberDO::getTenantId, tenantId)
                    .in(IpGroupMemberDO::getIpGroupId, groupIds));
            for (IpGroupMemberDO member : members) {
                String label = resolvePositionLabel(member.getPosition());
                if (StrUtil.isBlank(label)) {
                    continue;
                }
                memberPositionByGroupUser.put(member.getIpGroupId() + ":" + member.getUserId(), label);
                Long presentableUserId = footballSystemUserValidator.resolvePresentableUserId(member.getUserId());
                if (presentableUserId != null) {
                    memberPositionByGroupUser.put(member.getIpGroupId() + ":" + presentableUserId, label);
                }
            }
        }
        Map<Long, String> result = new java.util.HashMap<>();
        for (PerfRecordDO record : records) {
            String position = null;
            if (record.getIpGroupId() != null) {
                position = memberPositionByGroupUser.get(record.getIpGroupId() + ":" + record.getTargetUserId());
            }
            if (StrUtil.isBlank(position)) {
                position = resolvePositionLabel(footballSystemUserValidator.resolveLegacyPosition(record.getTargetUserId()));
            }
            if (StrUtil.isNotBlank(position)) {
                result.put(record.getId(), position);
            }
        }
        return result;
    }

    private String resolveRecordPosition(PerfRecordDO record) {
        if (record == null) {
            return null;
        }
        if (record.getIpGroupId() != null) {
            Long tenantId = requireTenantId();
            List<IpGroupMemberDO> members = ipGroupMemberMapper.selectList(new LambdaQueryWrapper<IpGroupMemberDO>()
                    .eq(IpGroupMemberDO::getTenantId, tenantId)
                    .eq(IpGroupMemberDO::getIpGroupId, record.getIpGroupId()));
            for (IpGroupMemberDO member : members) {
                Long presentableUserId = footballSystemUserValidator.resolvePresentableUserId(member.getUserId());
                if (Objects.equals(member.getUserId(), record.getTargetUserId())
                        || Objects.equals(presentableUserId, record.getTargetUserId())) {
                    String label = resolvePositionLabel(member.getPosition());
                    if (StrUtil.isNotBlank(label)) {
                        return label;
                    }
                }
            }
        }
        return resolvePositionLabel(footballSystemUserValidator.resolveLegacyPosition(record.getTargetUserId()));
    }

    private String resolvePositionLabel(String position) {
        if (StrUtil.isBlank(position)) {
            return null;
        }
        return dictService.listByType("dict_position").stream()
                .filter(dict -> position.equals(dict.getDictValue()))
                .map(SysDictDataDO::getLabel)
                .filter(StrUtil::isNotBlank)
                .findFirst()
                .orElse(position);
    }

    private Map<Long, MetricDO> loadMetrics(List<PerfItemRecordDO> itemRecords) {
        List<Long> metricIds = itemRecords.stream().map(PerfItemRecordDO::getMetricId).distinct().toList();
        if (metricIds.isEmpty()) {
            return Map.of();
        }
        return metricMapper.selectBatchIds(metricIds).stream()
                .collect(Collectors.toMap(MetricDO::getId, Function.identity(), (a, b) -> a));
    }

    private BigDecimal resolveTarget(PerfTemplateItemDO templateItem) {
        if (templateItem == null || templateItem.getScoreStandardJson() == null) {
            return DEFAULT_MAX_SCORE;
        }
        try {
            ScoreStandardDTO standard = PerfScoreSupport.parseStandard(templateItem.getScoreStandardJson());
            return standard.getRanges().stream()
                    .map(ScoreStandardDTO.Range::getMax)
                    .filter(Objects::nonNull)
                    .max(Comparator.naturalOrder())
                    .orElse(DEFAULT_MAX_SCORE);
        } catch (Exception ignored) {
            return DEFAULT_MAX_SCORE;
        }
    }

    private Integer resolveWorkflowStatus(String status) {
        if ("CONFIRMED".equals(status)) {
            return 3;
        }
        return 0;
    }

    private void applyWorkflowFields(PerfRecordDetailVO vo, PerfRecordDO record) {
        if ("CONFIRMED".equals(record.getStatus())) {
            String confirmedAt = formatDateTime(record.getUpdateTime());
            vo.setSubmittedAt(formatDateTime(record.getCreateTime()));
            vo.setPublishedAt(confirmedAt);
            String operator = record.getUpdater() != null ? record.getUpdater() : "系统";
            vo.setReviewer1(operator + " / " + confirmedAt);
            vo.setReviewer2("HR复核 / " + confirmedAt);
            return;
        }
        vo.setSubmittedAt(null);
        vo.setPublishedAt(null);
        vo.setReviewer1(null);
        vo.setReviewer2(null);
    }

    private String formatDateTime(LocalDateTime time) {
        return time == null ? null : time.format(DISPLAY_TIME);
    }

    private Map<Long, String> loadUserNames(List<PerfRecordDO> records) {
        List<Long> userIds = records.stream().map(PerfRecordDO::getTargetUserId).distinct().toList();
        if (userIds.isEmpty()) {
            return Map.of();
        }
        return footballSystemUserValidator.loadNicknames(userIds);
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

    private PerfTemplateDO resolveTemplate(Long templateId, SysUserDO user) {
        if (templateId != null) {
            PerfTemplateDO template = perfTemplateMapper.selectById(templateId);
            if (template == null || !Objects.equals(template.getTenantId(), requireTenantId())) {
                throw new ServiceException(OaErrorCodes.ENTITY_NOT_EXISTS);
            }
            if (!Objects.equals(template.getIsActive(), 1)) {
                throw new ServiceException(OaErrorCodes.ENTITY_DISABLED);
            }
            return template;
        }
        if (user == null || user.getPosition() == null) {
            throw new ServiceException(OaErrorCodes.PERF_NO_TEMPLATE);
        }
        PerfTemplateDO template = perfTemplateService.findActiveByPosition(user.getPosition());
        if (template == null) {
            throw new ServiceException(OaErrorCodes.PERF_NO_TEMPLATE);
        }
        return template;
    }
}
