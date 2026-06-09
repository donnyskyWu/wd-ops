package cn.iocoder.yudao.module.oa.service.analytics;

import cn.iocoder.yudao.framework.common.exception.OaErrorCodes;
import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.oa.api.dto.analytics.CustomQueryCreateReq;
import cn.iocoder.yudao.module.oa.dal.dataobject.analytics.CustomQueryDO;
import cn.iocoder.yudao.module.oa.dal.mysql.analytics.CustomQueryMapper;
import cn.iocoder.yudao.module.oa.service.support.SqlSafetySupport;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class CustomQueryServiceImpl implements CustomQueryService {

    private final CustomQueryMapper customQueryMapper;

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
    public Map<String, Object> execute(Long id) {
        CustomQueryDO query = getRequired(id);
        SqlSafetySupport.assertSelectOnly(query.getSqlText());
        Map<String, Object> result = new HashMap<>();
        result.put("queryId", id);
        result.put("rows", List.of(Map.of("message", "stub execute ok")));
        return result;
    }

    @Override
    @Transactional
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
