package cn.iocoder.yudao.module.oa.service.perf;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.exception.OaErrorCodes;
import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.oa.api.dto.perf.PerfTemplateActivateReq;
import cn.iocoder.yudao.module.oa.api.dto.perf.PerfTemplateCreateReq;
import cn.iocoder.yudao.module.oa.api.dto.perf.PerfTemplateItemReq;
import cn.iocoder.yudao.module.oa.api.dto.perf.PerfTemplateItemVO;
import cn.iocoder.yudao.module.oa.api.dto.perf.PerfTemplateItemsVO;
import cn.iocoder.yudao.module.oa.api.dto.perf.PerfTemplateUpdateReq;
import cn.iocoder.yudao.module.oa.api.dto.perf.PerfTemplateVO;
import cn.iocoder.yudao.module.oa.dal.dataobject.dict.SysDictDataDO;
import cn.iocoder.yudao.module.oa.dal.dataobject.perf.MetricDO;
import cn.iocoder.yudao.module.oa.dal.dataobject.perf.PerfTemplateDO;
import cn.iocoder.yudao.module.oa.dal.dataobject.perf.PerfTemplateItemDO;
import cn.iocoder.yudao.module.oa.dal.mysql.dict.SysDictDataMapper;
import cn.iocoder.yudao.module.oa.dal.mysql.perf.MetricMapper;
import cn.iocoder.yudao.module.oa.dal.mysql.perf.PerfTemplateItemMapper;
import cn.iocoder.yudao.module.oa.dal.mysql.perf.PerfTemplateMapper;
import cn.iocoder.yudao.module.oa.framework.audit.AuditLog;
import cn.iocoder.yudao.module.oa.service.dict.DictService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PerfTemplateServiceImpl implements PerfTemplateService {

    private final PerfTemplateMapper perfTemplateMapper;
    private final PerfTemplateItemMapper perfTemplateItemMapper;
    private final MetricMapper metricMapper;
    private final SysDictDataMapper sysDictDataMapper;
    private final DictService dictService;

    @Override
    public PageResult<PerfTemplateVO> list(String position, Integer isActive, Integer pageNum, Integer pageSize) {
        Long tenantId = requireTenantId();
        LambdaQueryWrapper<PerfTemplateDO> wrapper = new LambdaQueryWrapper<PerfTemplateDO>()
                .eq(PerfTemplateDO::getTenantId, tenantId)
                .like(StrUtil.isNotBlank(position), PerfTemplateDO::getPositionsJson, jsonPositionToken(position))
                .eq(isActive != null, PerfTemplateDO::getIsActive, isActive)
                .orderByDesc(PerfTemplateDO::getId);
        Page<PerfTemplateDO> page = perfTemplateMapper.selectPage(
                new Page<>(pageNum == null ? 1 : pageNum, pageSize == null ? 20 : pageSize), wrapper);
        Map<String, String> positionLabels = loadPositionLabels();
        return new PageResult<>(page.getRecords().stream()
                .map(entity -> toVO(entity, positionLabels))
                .collect(Collectors.toList()), page.getTotal());
    }

    @Override
    @Transactional
    @AuditLog(module = "M3-perf", action = "create-template")
    public Long create(PerfTemplateCreateReq req) {
        Long tenantId = requireTenantId();
        List<String> positions = validateAndNormalizePositions(req.getPositions());
        validateItems(req.getItems());
        if (Objects.equals(req.getIsActive(), 1)) {
            deactivateByPositions(tenantId, positions, null);
        }
        PerfTemplateDO entity = new PerfTemplateDO();
        entity.setTenantId(tenantId);
        entity.setPositionsJson(PerfTemplatePositionSupport.toJson(positions));
        entity.setTemplateName(req.getTemplateName().trim());
        entity.setIsActive(req.getIsActive() == null ? 0 : req.getIsActive());
        entity.setCreator(TenantContextHolder.getUsername());
        entity.setUpdater(TenantContextHolder.getUsername());
        entity.setCreateTime(LocalDateTime.now());
        entity.setUpdateTime(LocalDateTime.now());
        perfTemplateMapper.insert(entity);
        saveItems(entity.getId(), req.getItems());
        return entity.getId();
    }

    @Override
    @Transactional
    @AuditLog(module = "M3-perf", action = "update-template")
    public void update(PerfTemplateUpdateReq req) {
        PerfTemplateDO existing = requireTemplate(req.getId());
        List<String> positions = validateAndNormalizePositions(req.getPositions());
        validateItems(req.getItems());
        if (StrUtil.isNotBlank(req.getTemplateName())) {
            existing.setTemplateName(req.getTemplateName().trim());
        }
        existing.setPositionsJson(PerfTemplatePositionSupport.toJson(positions));
        if (req.getIsActive() != null) {
            if (req.getIsActive() == 1) {
                deactivateByPositions(existing.getTenantId(), positions, existing.getId());
            }
            existing.setIsActive(req.getIsActive());
        }
        existing.setUpdater(TenantContextHolder.getUsername());
        existing.setUpdateTime(LocalDateTime.now());
        perfTemplateMapper.updateById(existing);
        perfTemplateItemMapper.delete(new LambdaQueryWrapper<PerfTemplateItemDO>()
                .eq(PerfTemplateItemDO::getTemplateId, existing.getId()));
        saveItems(existing.getId(), req.getItems());
    }

    @Override
    @Transactional
    @AuditLog(module = "M3-perf", action = "activate-template")
    public void activate(PerfTemplateActivateReq req) {
        PerfTemplateDO existing = requireTemplate(req.getId());
        List<String> positions = PerfTemplatePositionSupport.parse(existing.getPositionsJson());
        deactivateByPositions(existing.getTenantId(), positions, existing.getId());
        existing.setIsActive(1);
        existing.setUpdater(TenantContextHolder.getUsername());
        existing.setUpdateTime(LocalDateTime.now());
        perfTemplateMapper.updateById(existing);
    }

    @Override
    public PerfTemplateItemsVO getItems(Long id) {
        PerfTemplateDO template = requireTemplate(id);
        List<PerfTemplateItemDO> items = perfTemplateItemMapper.selectList(new LambdaQueryWrapper<PerfTemplateItemDO>()
                .eq(PerfTemplateItemDO::getTemplateId, id)
                .orderByAsc(PerfTemplateItemDO::getId));
        Map<Long, String> metricNames = loadMetricNames(items);
        Map<String, String> positionLabels = loadPositionLabels();
        List<String> positions = PerfTemplatePositionSupport.parse(template.getPositionsJson());
        PerfTemplateItemsVO vo = new PerfTemplateItemsVO();
        vo.setId(template.getId());
        vo.setTemplateName(template.getTemplateName());
        vo.setPositions(positions);
        vo.setPositionLabels(toPositionLabelList(positions, positionLabels));
        vo.setIsActive(template.getIsActive());
        vo.setRemark(template.getRemark());
        vo.setItems(items.stream().map(item -> toItemVO(item, metricNames)).collect(Collectors.toList()));
        return vo;
    }

    private List<String> validateAndNormalizePositions(List<String> positions) {
        PerfTemplatePositionSupport.validatePositions(positions, dictService);
        return PerfTemplatePositionSupport.normalize(positions);
    }

    private void validateItems(List<PerfTemplateItemReq> items) {
        if (items == null || items.isEmpty()) {
            throw new ServiceException(OaErrorCodes.PERF_WEIGHT_NOT_100);
        }
        PerfScoreSupport.validateWeightSum(items.stream().map(PerfTemplateItemReq::getWeight).toList());
        for (PerfTemplateItemReq item : items) {
            requireMetric(item.getMetricId());
            PerfScoreSupport.validateRanges(item.getScoreStandard().getRanges());
        }
    }

    private void saveItems(Long templateId, List<PerfTemplateItemReq> items) {
        for (PerfTemplateItemReq item : items) {
            PerfTemplateItemDO entity = new PerfTemplateItemDO();
            entity.setTemplateId(templateId);
            entity.setMetricId(item.getMetricId());
            entity.setWeight(item.getWeight());
            entity.setCalcRule(StrUtil.blankToDefault(item.getCalcRule(), "AUTO"));
            entity.setScoreStandardJson(PerfScoreSupport.toJson(item.getScoreStandard()));
            entity.setCreator(TenantContextHolder.getUsername());
            entity.setUpdater(TenantContextHolder.getUsername());
            entity.setCreateTime(LocalDateTime.now());
            entity.setUpdateTime(LocalDateTime.now());
            perfTemplateItemMapper.insert(entity);
        }
    }

    private void deactivateByPositions(Long tenantId, List<String> positions, Long excludeId) {
        if (positions == null || positions.isEmpty()) {
            return;
        }
        for (String position : positions) {
            deactivateByPosition(tenantId, position, excludeId);
        }
    }

    private void deactivateByPosition(Long tenantId, String position, Long excludeId) {
        List<PerfTemplateDO> activeTemplates = perfTemplateMapper.selectList(new LambdaQueryWrapper<PerfTemplateDO>()
                .eq(PerfTemplateDO::getTenantId, tenantId)
                .eq(PerfTemplateDO::getIsActive, 1)
                .like(PerfTemplateDO::getPositionsJson, jsonPositionToken(position))
                .ne(excludeId != null, PerfTemplateDO::getId, excludeId));
        for (PerfTemplateDO template : activeTemplates) {
            if (!PerfTemplatePositionSupport.contains(template.getPositionsJson(), position)) {
                continue;
            }
            template.setIsActive(0);
            template.setUpdater(TenantContextHolder.getUsername());
            template.setUpdateTime(LocalDateTime.now());
            perfTemplateMapper.updateById(template);
        }
    }

    private String jsonPositionToken(String position) {
        return "\"" + position + "\"";
    }

    private PerfTemplateVO toVO(PerfTemplateDO entity, Map<String, String> positionLabels) {
        PerfTemplateVO vo = new PerfTemplateVO();
        vo.setId(entity.getId());
        List<String> positions = PerfTemplatePositionSupport.parse(entity.getPositionsJson());
        vo.setPositions(positions);
        vo.setPositionLabels(toPositionLabelList(positions, positionLabels));
        vo.setPositionLabel(PerfTemplatePositionSupport.joinLabels(positions, positionLabels));
        vo.setTemplateName(entity.getTemplateName());
        vo.setIsActive(entity.getIsActive());
        vo.setCreateTime(entity.getCreateTime());
        Long itemCount = perfTemplateItemMapper.selectCount(new LambdaQueryWrapper<PerfTemplateItemDO>()
                .eq(PerfTemplateItemDO::getTemplateId, entity.getId()));
        vo.setItemCount(itemCount == null ? 0 : itemCount.intValue());
        return vo;
    }

    private PerfTemplateItemVO toItemVO(PerfTemplateItemDO item, Map<Long, String> metricNames) {
        PerfTemplateItemVO vo = new PerfTemplateItemVO();
        vo.setId(item.getId());
        vo.setMetricId(item.getMetricId());
        vo.setMetricName(metricNames.get(item.getMetricId()));
        vo.setWeight(item.getWeight());
        vo.setCalcRule(item.getCalcRule());
        vo.setScoreStandard(PerfScoreSupport.parseStandard(item.getScoreStandardJson()));
        return vo;
    }

    private Map<Long, String> loadMetricNames(List<PerfTemplateItemDO> items) {
        List<Long> metricIds = items.stream().map(PerfTemplateItemDO::getMetricId).distinct().toList();
        if (metricIds.isEmpty()) {
            return Map.of();
        }
        return metricMapper.selectBatchIds(metricIds).stream()
                .collect(Collectors.toMap(MetricDO::getId, MetricDO::getMetricName, (a, b) -> a));
    }

    private Map<String, String> loadPositionLabels() {
        return sysDictDataMapper.selectList(new LambdaQueryWrapper<SysDictDataDO>()
                        .eq(SysDictDataDO::getDictType, "dict_position")
                        .eq(SysDictDataDO::getStatus, "ENABLED"))
                .stream()
                .collect(Collectors.toMap(SysDictDataDO::getDictValue, SysDictDataDO::getLabel, (a, b) -> a));
    }

    private List<String> toPositionLabelList(List<String> positions, Map<String, String> positionLabels) {
        return positions.stream()
                .map(position -> positionLabels.getOrDefault(position, position))
                .collect(Collectors.toCollection(ArrayList::new));
    }

    private MetricDO requireMetric(Long metricId) {
        MetricDO metric = metricMapper.selectById(metricId);
        if (metric == null || !Objects.equals(metric.getTenantId(), requireTenantId())) {
            throw new ServiceException(OaErrorCodes.ENTITY_NOT_EXISTS);
        }
        if (!Objects.equals(metric.getStatus(), 1)) {
            throw new ServiceException(OaErrorCodes.ENTITY_DISABLED);
        }
        return metric;
    }

    PerfTemplateDO requireTemplate(Long id) {
        PerfTemplateDO entity = perfTemplateMapper.selectById(id);
        if (entity == null) {
            throw new ServiceException(OaErrorCodes.ENTITY_NOT_EXISTS);
        }
        if (!Objects.equals(entity.getTenantId(), requireTenantId())) {
            throw new ServiceException(OaErrorCodes.TENANT_FORBIDDEN);
        }
        return entity;
    }

    @Override
    public PerfTemplateDO findActiveByPosition(String position) {
        if (StrUtil.isBlank(position)) {
            return null;
        }
        return perfTemplateMapper.selectList(new LambdaQueryWrapper<PerfTemplateDO>()
                        .eq(PerfTemplateDO::getTenantId, requireTenantId())
                        .eq(PerfTemplateDO::getIsActive, 1)
                        .like(PerfTemplateDO::getPositionsJson, jsonPositionToken(position))
                        .orderByDesc(PerfTemplateDO::getId))
                .stream()
                .filter(template -> PerfTemplatePositionSupport.contains(template.getPositionsJson(), position))
                .findFirst()
                .orElse(null);
    }

    private Long requireTenantId() {
        Long tenantId = TenantContextHolder.getTenantId();
        if (tenantId == null) {
            throw new ServiceException(OaErrorCodes.UNAUTHORIZED.getCode(), "缺少租户上下文");
        }
        return tenantId;
    }
}
