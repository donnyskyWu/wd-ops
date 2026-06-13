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

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ParamServiceImpl implements ParamService {

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
        return new PageResult<>(page.getRecords().stream().map(this::toResp).collect(Collectors.toList()), page.getTotal());
    }

    @Override
    @Transactional
    @AuditLog(module = "M9-param", action = "create")
    public Long create(ParamCreateReq req) {
        Long tenantId = requireTenantId();
        assertKeyUnique(tenantId, req.getParamKey(), null);
        SysParamDO entity = new SysParamDO();
        entity.setTenantId(tenantId);
        entity.setParamName(req.getParamName().trim());
        entity.setParamKey(req.getParamKey().trim());
        entity.setParamValue(req.getParamValue());
        entity.setParamType(req.getParamType());
        entity.setCategory(req.getCategory());
        entity.setRemark(req.getRemark());
        entity.setCreator(TenantContextHolder.getUsername());
        entity.setUpdater(TenantContextHolder.getUsername());
        entity.setCreateTime(LocalDateTime.now());
        entity.setUpdateTime(LocalDateTime.now());
        sysParamMapper.insert(entity);
        return entity.getId();
    }

    @Override
    @Transactional
    @AuditLog(module = "M9-param", action = "update")
    public void update(ParamUpdateReq req) {
        SysParamDO existing = requireParam(req.getId());
        assertKeyUnique(existing.getTenantId(), req.getParamKey(), existing.getId());
        existing.setParamName(req.getParamName().trim());
        existing.setParamKey(req.getParamKey().trim());
        existing.setParamValue(req.getParamValue());
        existing.setParamType(req.getParamType());
        existing.setCategory(req.getCategory());
        existing.setRemark(req.getRemark());
        existing.setUpdater(TenantContextHolder.getUsername());
        existing.setUpdateTime(LocalDateTime.now());
        sysParamMapper.updateById(existing);
    }

    @Override
    @Transactional
    @AuditLog(module = "M9-param", action = "delete")
    public void delete(Long id) {
        requireParam(id);
        sysParamMapper.deleteById(id);
    }

    @Override
    public String getString(Long tenantId, String paramKey, String defaultValue) {
        SysParamDO param = selectByKey(tenantId, paramKey);
        if (param == null || StrUtil.isBlank(param.getParamValue())) {
            return defaultValue;
        }
        return param.getParamValue().trim();
    }

    @Override
    public boolean getBoolean(Long tenantId, String paramKey, boolean defaultValue) {
        String value = getString(tenantId, paramKey, null);
        if (value == null) {
            return defaultValue;
        }
        return "true".equalsIgnoreCase(value) || "1".equals(value) || "yes".equalsIgnoreCase(value);
    }

    private SysParamDO selectByKey(Long tenantId, String paramKey) {
        return sysParamMapper.selectOne(new LambdaQueryWrapper<SysParamDO>()
                .eq(SysParamDO::getTenantId, tenantId)
                .eq(SysParamDO::getParamKey, paramKey)
                .last("LIMIT 1"));
    }

    private void assertKeyUnique(Long tenantId, String paramKey, Long excludeId) {
        LambdaQueryWrapper<SysParamDO> wrapper = new LambdaQueryWrapper<SysParamDO>()
                .eq(SysParamDO::getTenantId, tenantId)
                .eq(SysParamDO::getParamKey, paramKey);
        if (excludeId != null) {
            wrapper.ne(SysParamDO::getId, excludeId);
        }
        if (sysParamMapper.selectCount(wrapper) > 0) {
            throw new ServiceException(OaErrorCodes.DUPLICATE_ENTITY.getCode(), "参数键已存在");
        }
    }

    private SysParamDO requireParam(Long id) {
        SysParamDO entity = sysParamMapper.selectById(id);
        if (entity == null) {
            throw new ServiceException(OaErrorCodes.ENTITY_NOT_EXISTS);
        }
        if (!Objects.equals(entity.getTenantId(), requireTenantId())) {
            throw new ServiceException(OaErrorCodes.TENANT_FORBIDDEN);
        }
        return entity;
    }

    private ParamRespVO toResp(SysParamDO entity) {
        ParamRespVO vo = new ParamRespVO();
        vo.setId(entity.getId());
        vo.setParamName(entity.getParamName());
        vo.setParamKey(entity.getParamKey());
        vo.setParamValue(entity.getParamValue());
        vo.setParamType(entity.getParamType());
        vo.setCategory(entity.getCategory());
        vo.setRemark(entity.getRemark());
        vo.setUpdateTime(entity.getUpdateTime());
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
