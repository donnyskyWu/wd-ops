package cn.iocoder.yudao.module.oa.service.system;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.exception.OaErrorCodes;
import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.oa.api.dto.system.TenantCreateReq;
import cn.iocoder.yudao.module.oa.api.dto.system.TenantRespVO;
import cn.iocoder.yudao.module.oa.api.dto.system.TenantUpdateReq;
import cn.iocoder.yudao.module.oa.dal.dataobject.auth.SysTenantDO;
import cn.iocoder.yudao.module.oa.dal.mysql.auth.SysTenantMapper;
import cn.iocoder.yudao.module.oa.framework.audit.AuditLog;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TenantServiceImpl implements TenantService {

    private static final List<Long> PROTECTED_TENANT_IDS = List.of(1L, 2L);

    private final SysTenantMapper sysTenantMapper;

    @Override
    public PageResult<TenantRespVO> list(String tenantName, String contactName, String status,
                                       Integer pageNo, Integer pageSize) {
        LambdaQueryWrapper<SysTenantDO> wrapper = new LambdaQueryWrapper<SysTenantDO>()
                .like(StrUtil.isNotBlank(tenantName), SysTenantDO::getName, tenantName)
                .like(StrUtil.isNotBlank(contactName), SysTenantDO::getContactName, contactName)
                .eq(StrUtil.isNotBlank(status), SysTenantDO::getStatus, status)
                .orderByDesc(SysTenantDO::getId);
        Page<SysTenantDO> page = sysTenantMapper.selectPage(
                new Page<>(pageNo == null ? 1 : pageNo, pageSize == null ? 10 : pageSize), wrapper);
        List<TenantRespVO> list = page.getRecords().stream().map(this::toResp).collect(Collectors.toList());
        return new PageResult<>(list, page.getTotal());
    }

    @Override
    @Transactional
    @AuditLog(module = "M9-tenant", action = "create")
    public Long create(TenantCreateReq req) {
        assertNameUnique(req.getTenantName(), null);

        SysTenantDO entity = new SysTenantDO();
        entity.setName(req.getTenantName());
        entity.setContactName(req.getContactName());
        entity.setContactPhone(req.getContactPhone());
        entity.setContactEmail(req.getContactEmail());
        entity.setExpireTime(req.getExpireTime());
        entity.setMaxAccounts(req.getMaxAccounts() == null ? 10 : req.getMaxAccounts());
        entity.setStatus(StrUtil.blankToDefault(req.getStatus(), "TRIAL"));
        entity.setRemark(req.getRemark());
        entity.setCreator(currentUsername());
        entity.setUpdater(currentUsername());
        entity.setCreateTime(LocalDateTime.now());
        entity.setUpdateTime(LocalDateTime.now());
        sysTenantMapper.insert(entity);
        return entity.getId();
    }

    @Override
    @Transactional
    @AuditLog(module = "M9-tenant", action = "update")
    public void update(TenantUpdateReq req) {
        SysTenantDO existing = getRequired(req.getId());
        if (StrUtil.isNotBlank(req.getTenantName()) && !req.getTenantName().equals(existing.getName())) {
            assertNameUnique(req.getTenantName(), existing.getId());
            existing.setName(req.getTenantName());
        }
        if (StrUtil.isNotBlank(req.getContactName())) {
            existing.setContactName(req.getContactName());
        }
        if (StrUtil.isNotBlank(req.getContactPhone())) {
            existing.setContactPhone(req.getContactPhone());
        }
        if (req.getContactEmail() != null) {
            existing.setContactEmail(req.getContactEmail());
        }
        if (req.getExpireTime() != null) {
            existing.setExpireTime(req.getExpireTime());
        }
        if (req.getMaxAccounts() != null) {
            existing.setMaxAccounts(req.getMaxAccounts());
        }
        if (StrUtil.isNotBlank(req.getStatus())) {
            existing.setStatus(req.getStatus());
        }
        if (req.getRemark() != null) {
            existing.setRemark(req.getRemark());
        }
        existing.setUpdater(currentUsername());
        existing.setUpdateTime(LocalDateTime.now());
        sysTenantMapper.updateById(existing);
    }

    @Override
    @Transactional
    @AuditLog(module = "M9-tenant", action = "delete")
    public void delete(Long id) {
        if (PROTECTED_TENANT_IDS.contains(id)) {
            throw new ServiceException(OaErrorCodes.ENTITY_ALREADY_BOUND.getCode(), "内置租户不可删除");
        }
        getRequired(id);
        Long userCount = sysTenantMapper.countUsersByTenantId(id);
        if (userCount != null && userCount > 0) {
            throw new ServiceException(OaErrorCodes.ENTITY_ALREADY_BOUND.getCode(), "租户下仍有用户，不可删除");
        }
        sysTenantMapper.deleteById(id);
    }

    private SysTenantDO getRequired(Long id) {
        SysTenantDO entity = sysTenantMapper.selectById(id);
        if (entity == null) {
            throw new ServiceException(OaErrorCodes.ENTITY_NOT_EXISTS);
        }
        return entity;
    }

    private void assertNameUnique(String name, Long excludeId) {
        LambdaQueryWrapper<SysTenantDO> wrapper = new LambdaQueryWrapper<SysTenantDO>()
                .eq(SysTenantDO::getName, name);
        if (excludeId != null) {
            wrapper.ne(SysTenantDO::getId, excludeId);
        }
        if (sysTenantMapper.selectCount(wrapper) > 0) {
            throw new ServiceException(OaErrorCodes.DUPLICATE_ENTITY.getCode(), "租户名称已存在");
        }
    }

    private TenantRespVO toResp(SysTenantDO entity) {
        TenantRespVO vo = new TenantRespVO();
        vo.setId(entity.getId());
        vo.setTenantName(entity.getName());
        vo.setContactName(entity.getContactName());
        vo.setContactPhone(entity.getContactPhone());
        vo.setContactEmail(entity.getContactEmail());
        vo.setExpireTime(entity.getExpireTime());
        vo.setMaxAccounts(entity.getMaxAccounts());
        vo.setAccountCount(sysTenantMapper.countUsersByTenantId(entity.getId()));
        vo.setStatus(entity.getStatus());
        vo.setRemark(entity.getRemark());
        vo.setCreateTime(entity.getCreateTime());
        return vo;
    }

    private String currentUsername() {
        String username = TenantContextHolder.getUsername();
        return StrUtil.blankToDefault(username, "system");
    }
}
