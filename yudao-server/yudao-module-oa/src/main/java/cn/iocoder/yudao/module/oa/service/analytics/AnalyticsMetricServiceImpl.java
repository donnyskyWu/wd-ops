package cn.iocoder.yudao.module.oa.service.analytics;

import cn.iocoder.yudao.framework.common.exception.OaErrorCodes;
import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.oa.api.dto.analytics.MetricCreateReq;
import cn.iocoder.yudao.module.oa.api.dto.analytics.MetricUpdateReq;
import cn.iocoder.yudao.module.oa.api.dto.analytics.MetricVO;
import cn.iocoder.yudao.module.oa.dal.dataobject.perf.MetricDO;
import cn.iocoder.yudao.module.oa.dal.dataobject.perf.PerfTemplateItemDO;
import cn.iocoder.yudao.module.oa.dal.mysql.perf.MetricMapper;
import cn.iocoder.yudao.module.oa.dal.mysql.perf.PerfTemplateItemMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AnalyticsMetricServiceImpl implements AnalyticsMetricService {

    private final MetricMapper metricMapper;
    private final PerfTemplateItemMapper perfTemplateItemMapper;

    @Override
    public PageResult<MetricVO> list(String metricType, Integer pageNum, Integer pageSize) {
        Long tenantId = requireTenantId();
        LambdaQueryWrapper<MetricDO> wrapper = new LambdaQueryWrapper<MetricDO>()
                .eq(MetricDO::getTenantId, tenantId)
                .eq(metricType != null, MetricDO::getCategory, metricType)
                .orderByDesc(MetricDO::getId);
        Page<MetricDO> page = metricMapper.selectPage(
                new Page<>(pageNum == null ? 1 : pageNum, pageSize == null ? 20 : pageSize), wrapper);
        return new PageResult<>(page.getRecords().stream().map(this::toVO).collect(Collectors.toList()), page.getTotal());
    }

    @Override
    @Transactional
    public Long create(MetricCreateReq req) {
        Long tenantId = requireTenantId();
        assertCodeUnique(tenantId, req.getMetricCode(), null);
        MetricDO entity = new MetricDO();
        entity.setTenantId(tenantId);
        entity.setMetricName(req.getMetricName());
        entity.setMetricCode(req.getMetricCode());
        entity.setUnit(req.getUnit());
        entity.setCategory(req.getMetricType() != null ? req.getMetricType() : "ANALYTICS");
        entity.setStatus(1);
        entity.setCreator(TenantContextHolder.getUsername());
        entity.setUpdater(TenantContextHolder.getUsername());
        metricMapper.insert(entity);
        return entity.getId();
    }

    @Override
    @Transactional
    public void update(MetricUpdateReq req) {
        MetricDO existing = getRequired(req.getId());
        assertCodeUnique(existing.getTenantId(), req.getMetricCode(), existing.getId());
        existing.setMetricName(req.getMetricName());
        existing.setMetricCode(req.getMetricCode());
        existing.setUnit(req.getUnit());
        if (req.getMetricType() != null) {
            existing.setCategory(req.getMetricType());
        }
        if (req.getStatus() != null) {
            existing.setStatus(req.getStatus());
        }
        existing.setUpdater(TenantContextHolder.getUsername());
        metricMapper.updateById(existing);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        MetricDO existing = getRequired(id);
        Long refCount = perfTemplateItemMapper.selectCount(new LambdaQueryWrapper<PerfTemplateItemDO>()
                .eq(PerfTemplateItemDO::getMetricId, id));
        if (refCount != null && refCount > 0) {
            throw new ServiceException(OaErrorCodes.ENTITY_ALREADY_BOUND);
        }
        metricMapper.deleteById(existing.getId());
    }

    private MetricDO getRequired(Long id) {
        MetricDO entity = metricMapper.selectById(id);
        if (entity == null) {
            throw new ServiceException(OaErrorCodes.ENTITY_NOT_EXISTS);
        }
        if (!Objects.equals(entity.getTenantId(), requireTenantId())) {
            throw new ServiceException(OaErrorCodes.TENANT_FORBIDDEN);
        }
        return entity;
    }

    private void assertCodeUnique(Long tenantId, String code, Long excludeId) {
        LambdaQueryWrapper<MetricDO> wrapper = new LambdaQueryWrapper<MetricDO>()
                .eq(MetricDO::getTenantId, tenantId)
                .eq(MetricDO::getMetricCode, code);
        if (excludeId != null) {
            wrapper.ne(MetricDO::getId, excludeId);
        }
        if (metricMapper.selectCount(wrapper) > 0) {
            throw new ServiceException(OaErrorCodes.DUPLICATE_ENTITY);
        }
    }

    private MetricVO toVO(MetricDO row) {
        MetricVO vo = new MetricVO();
        vo.setId(row.getId());
        vo.setMetricName(row.getMetricName());
        vo.setMetricCode(row.getMetricCode());
        vo.setMetricType(row.getCategory());
        vo.setCategory(row.getCategory());
        vo.setUnit(row.getUnit());
        vo.setStatus(row.getStatus());
        return vo;
    }

    private Long requireTenantId() {
        Long tenantId = TenantContextHolder.getTenantId();
        if (tenantId == null) {
            throw new ServiceException(OaErrorCodes.UNAUTHORIZED.getCode(), "缺少租户上下文");
        }
        return tenantId;
    }
}
