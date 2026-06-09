package cn.iocoder.yudao.module.oa.service.analytics;

import cn.iocoder.yudao.framework.common.exception.OaErrorCodes;
import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.oa.api.dto.analytics.DashboardCreateReq;
import cn.iocoder.yudao.module.oa.api.dto.analytics.DashboardVO;
import cn.iocoder.yudao.module.oa.dal.dataobject.analytics.DashboardDO;
import cn.iocoder.yudao.module.oa.dal.mysql.analytics.DashboardMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DashboardServiceImpl implements DashboardService {

    private final DashboardMapper dashboardMapper;

    @Override
    public DashboardVO get(Long id) {
        return toVO(getRequired(id));
    }

    @Override
    @Transactional
    public Long create(DashboardCreateReq req) {
        Long tenantId = requireTenantId();
        DashboardDO entity = new DashboardDO();
        entity.setTenantId(tenantId);
        entity.setDashboardName(req.getDashboardName());
        entity.setDashboardType(req.getDashboardType());
        entity.setLayoutJson(req.getLayout());
        entity.setStatus(1);
        entity.setCreator(TenantContextHolder.getUsername());
        entity.setUpdater(TenantContextHolder.getUsername());
        dashboardMapper.insert(entity);
        return entity.getId();
    }

    @Override
    public PageResult<DashboardVO> listConfig(Integer pageNum, Integer pageSize) {
        Long tenantId = requireTenantId();
        Page<DashboardDO> page = dashboardMapper.selectPage(
                new Page<>(pageNum == null ? 1 : pageNum, pageSize == null ? 20 : pageSize),
                new LambdaQueryWrapper<DashboardDO>().eq(DashboardDO::getTenantId, tenantId).orderByDesc(DashboardDO::getId));
        List<DashboardVO> list = page.getRecords().stream().map(this::toVO).collect(Collectors.toList());
        return new PageResult<>(list, page.getTotal());
    }

    @Override
    @Transactional
    public void updateConfig(Long id, String layout) {
        DashboardDO entity = getRequired(id);
        entity.setLayoutJson(layout);
        entity.setUpdater(TenantContextHolder.getUsername());
        dashboardMapper.updateById(entity);
    }

    private DashboardDO getRequired(Long id) {
        DashboardDO entity = dashboardMapper.selectById(id);
        if (entity == null) {
            throw new ServiceException(OaErrorCodes.ENTITY_NOT_EXISTS);
        }
        if (!Objects.equals(entity.getTenantId(), requireTenantId())) {
            throw new ServiceException(OaErrorCodes.TENANT_FORBIDDEN);
        }
        return entity;
    }

    private DashboardVO toVO(DashboardDO row) {
        DashboardVO vo = new DashboardVO();
        vo.setId(row.getId());
        vo.setDashboardName(row.getDashboardName());
        vo.setDashboardType(row.getDashboardType());
        vo.setLayout(row.getLayoutJson());
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
