package cn.iocoder.yudao.module.oa.service.config;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.exception.OaErrorCodes;
import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.oa.api.dto.config.ThresholdConfigCreateReq;
import cn.iocoder.yudao.module.oa.api.dto.config.ThresholdConfigRespVO;
import cn.iocoder.yudao.module.oa.api.dto.config.ThresholdConfigUpdateReq;
import cn.iocoder.yudao.module.oa.dal.dataobject.config.ThresholdConfigDO;
import cn.iocoder.yudao.module.oa.dal.mysql.config.ThresholdConfigMapper;
import cn.iocoder.yudao.module.oa.framework.audit.AuditLog;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ThresholdConfigServiceImpl implements ThresholdConfigService {

    private final ThresholdConfigMapper thresholdConfigMapper;

    @Override
    public PageResult<ThresholdConfigRespVO> list(String thresholdCategory, String metricName, String metricType,
                                                  String status, Integer pageNo, Integer pageSize) {
        Long tenantId = ConfigTenantSupport.requireTenantId();
        LambdaQueryWrapper<ThresholdConfigDO> wrapper = new LambdaQueryWrapper<ThresholdConfigDO>()
                .eq(ThresholdConfigDO::getTenantId, tenantId)
                .eq(StrUtil.isNotBlank(thresholdCategory), ThresholdConfigDO::getThresholdCategory, thresholdCategory)
                .like(StrUtil.isNotBlank(metricName), ThresholdConfigDO::getMetricName, metricName)
                .eq(StrUtil.isNotBlank(metricType), ThresholdConfigDO::getMetricType, metricType)
                .eq(StrUtil.isNotBlank(status), ThresholdConfigDO::getStatus, status)
                .orderByDesc(ThresholdConfigDO::getId);
        Page<ThresholdConfigDO> page = thresholdConfigMapper.selectPage(
                new Page<>(pageNo == null ? 1 : pageNo, pageSize == null ? 10 : pageSize), wrapper);
        List<ThresholdConfigRespVO> list = page.getRecords().stream().map(this::toResp).collect(Collectors.toList());
        return new PageResult<>(list, page.getTotal());
    }

    @Override
    @Transactional
    @AuditLog(module = "M8-threshold", action = "create")
    public Long create(ThresholdConfigCreateReq req) {
        ThresholdConfigDO entity = new ThresholdConfigDO();
        entity.setThresholdCategory(req.getThresholdCategory());
        entity.setThresholdType(req.getThresholdType());
        entity.setMetricName(req.getMetricName());
        entity.setMetricType(req.getMetricType());
        entity.setPlatformType(req.getPlatformType());
        entity.setContentType(req.getContentType());
        entity.setJudgeMode(StrUtil.blankToDefault(req.getJudgeMode(), "AND"));
        entity.setLowFans(req.getLowFans());
        entity.setHighFans(req.getHighFans());
        entity.setDailyLow(req.getDailyLow());
        entity.setDailyHigh(req.getDailyHigh());
        entity.setHotValue(req.getHotValue());
        entity.setLowValue(req.getLowValue());
        entity.setOverrideAccountId(req.getOverrideAccountId());
        entity.setOverrideValue(req.getOverrideValue());
        entity.setIpGroupId(req.getIpGroupId());
        entity.setCompareOperator(StrUtil.blankToDefault(req.getCompareOperator(), "GTE"));
        entity.setThresholdValue(req.getThresholdValue());
        entity.setAlertLevel(StrUtil.blankToDefault(req.getAlertLevel(), "WARNING"));
        entity.setNotifyMethods(req.getNotifyMethods());
        entity.setStatus(StrUtil.blankToDefault(req.getStatus(), "ENABLED"));
        entity.setRemark(req.getRemark());
        ConfigTenantSupport.fillCreate(entity);
        thresholdConfigMapper.insert(entity);
        return entity.getId();
    }

    @Override
    @Transactional
    @AuditLog(module = "M8-threshold", action = "update")
    public void update(ThresholdConfigUpdateReq req) {
        ThresholdConfigDO existing = getRequired(req.getId());
        if (StrUtil.isNotBlank(req.getMetricName())) {
            existing.setMetricName(req.getMetricName());
        }
        if (StrUtil.isNotBlank(req.getMetricType())) {
            existing.setMetricType(req.getMetricType());
        }
        if (req.getPlatformType() != null) {
            existing.setPlatformType(req.getPlatformType());
        }
        if (req.getIpGroupId() != null) {
            existing.setIpGroupId(req.getIpGroupId());
        }
        if (StrUtil.isNotBlank(req.getCompareOperator())) {
            existing.setCompareOperator(req.getCompareOperator());
        }
        if (req.getThresholdValue() != null) {
            existing.setThresholdValue(req.getThresholdValue());
        }
        if (StrUtil.isNotBlank(req.getAlertLevel())) {
            existing.setAlertLevel(req.getAlertLevel());
        }
        if (req.getNotifyMethods() != null) {
            existing.setNotifyMethods(req.getNotifyMethods());
        }
        if (StrUtil.isNotBlank(req.getStatus())) {
            existing.setStatus(req.getStatus());
        }
        if (req.getRemark() != null) {
            existing.setRemark(req.getRemark());
        }
        if (req.getThresholdCategory() != null) {
            existing.setThresholdCategory(req.getThresholdCategory());
        }
        if (req.getThresholdType() != null) {
            existing.setThresholdType(req.getThresholdType());
        }
        if (req.getContentType() != null) {
            existing.setContentType(req.getContentType());
        }
        if (req.getJudgeMode() != null) {
            existing.setJudgeMode(req.getJudgeMode());
        }
        if (req.getLowFans() != null) {
            existing.setLowFans(req.getLowFans());
        }
        if (req.getHighFans() != null) {
            existing.setHighFans(req.getHighFans());
        }
        if (req.getDailyLow() != null) {
            existing.setDailyLow(req.getDailyLow());
        }
        if (req.getDailyHigh() != null) {
            existing.setDailyHigh(req.getDailyHigh());
        }
        if (req.getHotValue() != null) {
            existing.setHotValue(req.getHotValue());
        }
        if (req.getLowValue() != null) {
            existing.setLowValue(req.getLowValue());
        }
        if (req.getOverrideAccountId() != null) {
            existing.setOverrideAccountId(req.getOverrideAccountId());
        }
        if (req.getOverrideValue() != null) {
            existing.setOverrideValue(req.getOverrideValue());
        }
        ConfigTenantSupport.fillUpdate(existing);
        thresholdConfigMapper.updateById(existing);
    }

    @Override
    @Transactional
    @AuditLog(module = "M8-threshold", action = "delete")
    public void delete(Long id) {
        ThresholdConfigDO existing = getRequired(id);
        thresholdConfigMapper.deleteById(existing.getId());
    }

    private ThresholdConfigDO getRequired(Long id) {
        ThresholdConfigDO entity = thresholdConfigMapper.selectById(id);
        return ConfigTenantSupport.getRequiredInTenant(entity);
    }

    private ThresholdConfigRespVO toResp(ThresholdConfigDO entity) {
        ThresholdConfigRespVO vo = new ThresholdConfigRespVO();
        vo.setId(entity.getId());
        vo.setThresholdCategory(entity.getThresholdCategory());
        vo.setThresholdType(entity.getThresholdType());
        vo.setMetricName(entity.getMetricName());
        vo.setMetricType(entity.getMetricType());
        vo.setPlatformType(entity.getPlatformType());
        vo.setContentType(entity.getContentType());
        vo.setJudgeMode(entity.getJudgeMode());
        vo.setLowFans(entity.getLowFans());
        vo.setHighFans(entity.getHighFans());
        vo.setDailyLow(entity.getDailyLow());
        vo.setDailyHigh(entity.getDailyHigh());
        vo.setHotValue(entity.getHotValue());
        vo.setLowValue(entity.getLowValue());
        vo.setOverrideAccountId(entity.getOverrideAccountId());
        vo.setOverrideValue(entity.getOverrideValue());
        vo.setIpGroupId(entity.getIpGroupId());
        vo.setCompareOperator(entity.getCompareOperator());
        vo.setThresholdValue(entity.getThresholdValue());
        vo.setAlertLevel(entity.getAlertLevel());
        vo.setNotifyMethods(entity.getNotifyMethods());
        vo.setStatus(entity.getStatus());
        vo.setRemark(entity.getRemark());
        vo.setCreateTime(entity.getCreateTime());
        return vo;
    }
}
