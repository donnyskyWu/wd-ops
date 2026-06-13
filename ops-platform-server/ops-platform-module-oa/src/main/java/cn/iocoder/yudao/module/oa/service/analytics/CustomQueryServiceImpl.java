package cn.iocoder.yudao.module.oa.service.analytics;

import cn.iocoder.yudao.framework.common.exception.OaErrorCodes;
import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.oa.api.dto.analytics.CustomQueryCreateReq;
import cn.iocoder.yudao.module.oa.api.dto.analytics.CustomQueryPreviewReq;
import cn.iocoder.yudao.module.oa.api.dto.analytics.CustomQueryUpdateReq;
import cn.iocoder.yudao.module.oa.dal.dataobject.analytics.CustomQueryDO;
import cn.iocoder.yudao.module.oa.dal.mysql.analytics.CustomQueryMapper;
import cn.iocoder.yudao.module.oa.framework.audit.AuditLog;
import cn.iocoder.yudao.module.oa.service.support.DashboardSqlParamBinder;
import cn.iocoder.yudao.module.oa.service.support.SqlSafetySupport;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class CustomQueryServiceImpl implements CustomQueryService {

    private static final Pattern TRAILING_LIMIT = Pattern.compile(
            "(?i)\\s+LIMIT\\s+\\d+(\\s+OFFSET\\s+\\d+)?\\s*$");

    private final CustomQueryMapper customQueryMapper;
    private final JdbcTemplate jdbcTemplate;

    @Override
    public PageResult<CustomQueryDO> list(String status, Integer pageNum, Integer pageSize) {
        Long tenantId = requireTenantId();
        Page<CustomQueryDO> page = customQueryMapper.selectPage(
                new Page<>(pageNum == null ? 1 : pageNum, pageSize == null ? 20 : pageSize),
                new LambdaQueryWrapper<CustomQueryDO>()
                        .eq(CustomQueryDO::getTenantId, tenantId)
                        .eq(status != null, CustomQueryDO::getStatus, status)
                        .orderByDesc(CustomQueryDO::getId));
        return new PageResult<>(page.getRecords(), page.getTotal());
    }

    @Override
    @Transactional
    @AuditLog(module = "M6-custom-query", action = "create")
    public Long create(CustomQueryCreateReq req) {
        SqlSafetySupport.assertSelectOnly(req.getSqlText());
        Long tenantId = requireTenantId();
        CustomQueryDO entity = new CustomQueryDO();
        entity.setTenantId(tenantId);
        entity.setQueryName(req.getQueryName());
        entity.setStatus(req.getStatus() != null ? req.getStatus() : "DRAFT");
        entity.setSqlText(req.getSqlText());
        entity.setParamsJson(req.getParams());
        entity.setCreator(TenantContextHolder.getUsername());
        entity.setUpdater(TenantContextHolder.getUsername());
        customQueryMapper.insert(entity);
        return entity.getId();
    }

    @Override
    @Transactional
    @AuditLog(module = "M6-custom-query", action = "update")
    public void update(CustomQueryUpdateReq req) {
        CustomQueryDO existing = getRequired(req.getId());
        SqlSafetySupport.assertSelectOnly(req.getSqlText());
        existing.setQueryName(req.getQueryName());
        if (req.getStatus() != null) {
            existing.setStatus(req.getStatus());
        }
        existing.setSqlText(req.getSqlText());
        existing.setParamsJson(req.getParams());
        existing.setUpdater(TenantContextHolder.getUsername());
        customQueryMapper.updateById(existing);
    }

    @Override
    public Map<String, Object> preview(CustomQueryPreviewReq req) {
        Long tenantId = requireTenantId();
        DashboardSqlParamBinder.BindParams params = DashboardSqlParamBinder.BindParams.of(
                tenantId, null, null, null, null);
        return runSelectSql(req.getSqlText(), null, params, null, req.getPageNum(), req.getPageSize());
    }

    @Override
    public Map<String, Object> execute(Long id) {
        Long tenantId = requireTenantId();
        return execute(id, DashboardSqlParamBinder.BindParams.of(tenantId, null, null, null, null));
    }

    @Override
    public Map<String, Object> execute(Long id, Integer pageNum, Integer pageSize) {
        Long tenantId = requireTenantId();
        CustomQueryDO query = getRequired(id);
        DashboardSqlParamBinder.BindParams params = DashboardSqlParamBinder.BindParams.of(
                tenantId, null, null, null, null);
        return runSelectSql(query.getSqlText(), id, params, null, pageNum, pageSize);
    }

    @Override
    public Map<String, Object> execute(Long id, DashboardSqlParamBinder.BindParams bindParams) {
        return execute(id, bindParams, null);
    }

    @Override
    public Map<String, Object> execute(Long id, DashboardSqlParamBinder.BindParams bindParams,
                                       Map<String, Object> widgetDef) {
        CustomQueryDO query = getRequired(id);
        return runSelectSql(query.getSqlText(), id, bindParams, widgetDef, null, null);
    }

    private Map<String, Object> runSelectSql(String sqlText, Long queryId, DashboardSqlParamBinder.BindParams bindParams,
                                             Map<String, Object> widgetDef, Integer pageNum, Integer pageSize) {
        SqlSafetySupport.assertSelectOnly(sqlText);
        String sql = DashboardSqlParamBinder.prepareSql(sqlText, bindParams, widgetDef);
        String baseSql = stripTrailingLimit(sql);
        Map<String, Object> result = new HashMap<>();
        if (queryId != null) {
            result.put("queryId", queryId);
        }

        if (pageNum != null || pageSize != null) {
            int pn = pageNum == null || pageNum < 1 ? 1 : pageNum;
            int ps = pageSize == null || pageSize < 1 ? 20 : Math.min(pageSize, 500);
            Long total = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM (" + baseSql + ") _cq", Long.class);
            int offset = (pn - 1) * ps;
            List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                    "SELECT * FROM (" + baseSql + ") _cq LIMIT " + ps + " OFFSET " + offset);
            result.put("rows", rows);
            result.put("total", total != null ? total : rows.size());
            result.put("pageNum", pn);
            result.put("pageSize", ps);
            return result;
        }

        if (!baseSql.toUpperCase(Locale.ROOT).contains(" LIMIT ")) {
            baseSql = baseSql + " LIMIT 100";
        }
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(baseSql);
        result.put("rows", rows);
        return result;
    }

    private static String stripTrailingLimit(String sql) {
        if (sql == null) {
            return "";
        }
        return TRAILING_LIMIT.matcher(sql.trim()).replaceFirst("").trim();
    }

    @Override
    @Transactional
    @AuditLog(module = "M6-custom-query", action = "publish")
    public void publish(Long id) {
        CustomQueryDO query = getRequired(id);
        query.setStatus("PUBLISHED");
        query.setUpdater(TenantContextHolder.getUsername());
        customQueryMapper.updateById(query);
    }

    private CustomQueryDO getRequired(Long id) {
        CustomQueryDO entity = customQueryMapper.selectById(id);
        if (entity == null) {
            throw new ServiceException(OaErrorCodes.ENTITY_NOT_EXISTS);
        }
        if (!Objects.equals(entity.getTenantId(), requireTenantId())) {
            throw new ServiceException(OaErrorCodes.TENANT_FORBIDDEN);
        }
        return entity;
    }

    private Long requireTenantId() {
        Long tenantId = TenantContextHolder.getTenantId();
        if (tenantId == null) {
            throw new ServiceException(OaErrorCodes.UNAUTHORIZED.getCode(), "缺少租户上下文");
        }
        return tenantId;
    }
}
