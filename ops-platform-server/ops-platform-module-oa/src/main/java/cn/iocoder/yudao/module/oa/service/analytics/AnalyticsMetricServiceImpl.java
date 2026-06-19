package cn.iocoder.yudao.module.oa.service.analytics;

import cn.iocoder.yudao.framework.common.exception.OaErrorCodes;
import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.oa.api.dto.analytics.MetricCreateReq;
import cn.iocoder.yudao.module.oa.api.dto.analytics.MetricPreviewReq;
import cn.iocoder.yudao.module.oa.api.dto.analytics.MetricPreviewVO;
import cn.iocoder.yudao.module.oa.api.dto.analytics.MetricUpdateReq;
import cn.iocoder.yudao.module.oa.api.dto.analytics.MetricVO;
import cn.iocoder.yudao.module.oa.dal.dataobject.perf.MetricDO;
import cn.iocoder.yudao.module.oa.dal.dataobject.perf.PerfTemplateItemDO;
import cn.iocoder.yudao.module.oa.dal.mysql.perf.MetricMapper;
import cn.iocoder.yudao.module.oa.dal.mysql.perf.PerfTemplateItemMapper;
import cn.iocoder.yudao.module.oa.framework.audit.AuditLog;
import cn.iocoder.yudao.module.oa.service.support.DashboardSqlParamBinder;
import cn.iocoder.yudao.module.oa.service.support.SqlSafetySupport;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AnalyticsMetricServiceImpl implements AnalyticsMetricService {

    private final MetricMapper metricMapper;
    private final PerfTemplateItemMapper perfTemplateItemMapper;
    private final JdbcTemplate jdbcTemplate;

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
    @AuditLog(module = "M6-metric", action = "create")
    public Long create(MetricCreateReq req) {
        Long tenantId = requireTenantId();
        assertCodeUnique(tenantId, req.getMetricCode(), null);
        MetricDO entity = new MetricDO();
        entity.setTenantId(tenantId);
        entity.setMetricName(req.getMetricName());
        entity.setMetricCode(req.getMetricCode());
        entity.setUnit(req.getUnit());
        entity.setCategory(req.getMetricType() != null ? req.getMetricType() : "ANALYTICS");
        entity.setMetricFormula(req.getMetricFormula());
        entity.setDataSource(req.getDataSource());
        entity.setParamsJson(req.getParamsJson());
        entity.setStatus(1);
        entity.setCreator(TenantContextHolder.getUsername());
        entity.setUpdater(TenantContextHolder.getUsername());
        metricMapper.insert(entity);
        return entity.getId();
    }

    @Override
    @Transactional
    @AuditLog(module = "M6-metric", action = "update")
    public void update(MetricUpdateReq req) {
        MetricDO existing = getRequired(req.getId());
        assertCodeUnique(existing.getTenantId(), req.getMetricCode(), existing.getId());
        existing.setMetricName(req.getMetricName());
        existing.setMetricCode(req.getMetricCode());
        existing.setUnit(req.getUnit());
        if (req.getMetricType() != null) {
            existing.setCategory(req.getMetricType());
        }
        if (req.getMetricFormula() != null) {
            existing.setMetricFormula(req.getMetricFormula());
        }
        if (req.getDataSource() != null) {
            existing.setDataSource(req.getDataSource());
        }
        if (req.getParamsJson() != null) {
            existing.setParamsJson(req.getParamsJson());
        }
        if (req.getStatus() != null) {
            existing.setStatus(req.getStatus());
        }
        existing.setUpdater(TenantContextHolder.getUsername());
        metricMapper.updateById(existing);
    }

    @Override
    @Transactional
    @AuditLog(module = "M6-metric", action = "delete")
    public void delete(Long id) {
        MetricDO existing = getRequired(id);
        Long refCount = perfTemplateItemMapper.selectCount(new LambdaQueryWrapper<PerfTemplateItemDO>()
                .eq(PerfTemplateItemDO::getMetricId, id));
        if (refCount != null && refCount > 0) {
            throw new ServiceException(OaErrorCodes.ENTITY_ALREADY_BOUND);
        }
        metricMapper.deleteById(existing.getId());
    }

    @Override
    public MetricPreviewVO preview(MetricPreviewReq req) {
        Long tenantId = requireTenantId();
        return preview(req, DashboardSqlParamBinder.BindParams.of(tenantId, null, null, null, null));
    }

    @Override
    public MetricPreviewVO preview(MetricPreviewReq req, DashboardSqlParamBinder.BindParams bindParams) {
        return preview(req, bindParams, null);
    }

    @Override
    public MetricPreviewVO preview(MetricPreviewReq req, DashboardSqlParamBinder.BindParams bindParams,
                                     Map<String, Object> widgetDef) {
        SqlSafetySupport.assertSelectOnly(req.getMetricFormula());
        String sql = DashboardSqlParamBinder.prepareSql(req.getMetricFormula(), bindParams, widgetDef);
        sql = DashboardSqlParamBinder.bindCustomParams(sql, req.getBindParams());
        if (!sql.toUpperCase(Locale.ROOT).contains(" LIMIT ")) {
            sql = sql + " LIMIT 20";
        }
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql);
        MetricPreviewVO vo = new MetricPreviewVO();
        vo.setRows(rows);
        return vo;
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
        vo.setMetricFormula(row.getMetricFormula());
        vo.setDataSource(row.getDataSource());
        vo.setParamsJson(row.getParamsJson());
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
