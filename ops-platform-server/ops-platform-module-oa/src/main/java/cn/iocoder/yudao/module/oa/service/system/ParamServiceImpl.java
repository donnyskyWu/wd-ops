package cn.iocoder.yudao.module.oa.service.system;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.exception.OaErrorCodes;
import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.oa.api.dto.system.ParamCreateReq;
import cn.iocoder.yudao.module.oa.api.dto.system.ParamRespVO;
import cn.iocoder.yudao.module.oa.api.dto.system.ParamUpdateReq;
import cn.iocoder.yudao.module.oa.dal.dataobject.system.SysParamDO;
import cn.iocoder.yudao.module.oa.dal.mysql.system.SysParamMapper;
import cn.iocoder.yudao.module.oa.framework.audit.AuditLog;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ParamServiceImpl implements ParamService {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final SysParamMapper sysParamMapper;

    @Override
    public PageResult<ParamRespVO> list(String paramName, String paramKey, String category,
                                        Integer pageNo, Integer pageSize) {
        Long tenantId = requireTenantId();
        LambdaQueryWrapper<SysParamDO> wrapper = new LambdaQueryWrapper<SysParamDO>()
                .eq(SysParamDO::getTenantId, tenantId)
                .like(StrUtil.isNotBlank(paramName), SysParamDO::getParamName, paramName)
                .like(StrUtil.isNotBlank(paramKey), SysParamDO::getParamKey, paramKey)
                .eq(StrUtil.isNotBlank(category), SysParamDO::getCategory, category)
                .orderByDesc(SysParamDO::getId);
        Page<SysParamDO> page = sysParamMapper.selectPage(
                new Page<>(pageNo == null ? 1 : pageNo, pageSize == null ? 10 : pageSize), wrapper);
        List<ParamRespVO> list = page.getRecords().stream().map(this::toVO).collect(Collectors.toList());
        return new PageResult<>(list, page.getTotal());
    }

    @Override
    @Transactional
    @AuditLog(module = "M9-param", action = "create")
    public Long create(ParamCreateReq req) {
        Long tenantId = requireTenantId();
        assertKeyUnique(tenantId, req.getParamKey(), null);
        SysParamDO row = new SysParamDO();
        row.setTenantId(tenantId);
        row.setParamName(req.getParamName());
        row.setParamKey(req.getParamKey());
        row.setParamValue(req.getParamValue());
        row.setParamType(req.getParamType());
        row.setCategory(req.getCategory());
        row.setRemark(req.getRemark());
        row.setCreator(TenantContextHolder.getUsername());
        row.setUpdater(TenantContextHolder.getUsername());
        sysParamMapper.insert(row);
        return row.getId();
    }

    @Override
    @Transactional
    @AuditLog(module = "M9-param", action = "update")
    public void update(ParamUpdateReq req) {
        Long tenantId = requireTenantId();
        SysParamDO existing = requireInTenant(tenantId, req.getId());
        assertKeyUnique(tenantId, req.getParamKey(), req.getId());
        existing.setParamName(req.getParamName());
        existing.setParamKey(req.getParamKey());
        existing.setParamValue(req.getParamValue());
        existing.setParamType(req.getParamType());
        existing.setCategory(req.getCategory());
        existing.setRemark(req.getRemark());
        existing.setUpdater(TenantContextHolder.getUsername());
        sysParamMapper.updateById(existing);
    }

    @Override
    @Transactional
    @AuditLog(module = "M9-param", action = "delete")
    public void delete(Long id) {
        Long tenantId = requireTenantId();
        requireInTenant(tenantId, id);
        sysParamMapper.deleteById(id);
    }

    private SysParamDO requireInTenant(Long tenantId, Long id) {
        SysParamDO row = sysParamMapper.selectById(id);
        if (row == null || !tenantId.equals(row.getTenantId())) {
            throw new ServiceException(OaErrorCodes.ENTITY_NOT_EXISTS);
        }
        return row;
    }

    private void assertKeyUnique(Long tenantId, String paramKey, Long excludeId) {
        LambdaQueryWrapper<SysParamDO> wrapper = new LambdaQueryWrapper<SysParamDO>()
                .eq(SysParamDO::getTenantId, tenantId)
                .eq(SysParamDO::getParamKey, paramKey);
        if (excludeId != null) {
            wrapper.ne(SysParamDO::getId, excludeId);
        }
        Long count = sysParamMapper.selectCount(wrapper);
        if (count != null && count > 0) {
            throw new ServiceException(OaErrorCodes.DUPLICATE_ENTITY);
        }
    }

    private ParamRespVO toVO(SysParamDO row) {
        ParamRespVO vo = new ParamRespVO();
        vo.setId(row.getId());
        vo.setParamName(row.getParamName());
        vo.setParamKey(row.getParamKey());
        vo.setParamValue(row.getParamValue());
        vo.setParamType(row.getParamType());
        vo.setCategory(row.getCategory());
        vo.setRemark(row.getRemark());
        if (row.getUpdateTime() != null) {
            vo.setUpdateTime(row.getUpdateTime().format(FMT));
        }
        return vo;
    }

    private Long requireTenantId() {
        Long tenantId = TenantContextHolder.getTenantId();
        if (tenantId == null) {
            throw new ServiceException(OaErrorCodes.TENANT_FORBIDDEN);
        }
        return tenantId;
    }
}
