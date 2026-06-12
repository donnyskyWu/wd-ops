package cn.iocoder.yudao.module.oa.service.system;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.exception.OaErrorCodes;
import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.oa.api.dto.system.PermissionRespVO;
import cn.iocoder.yudao.module.oa.api.dto.system.RoleAssignPermissionReq;
import cn.iocoder.yudao.module.oa.api.dto.system.RoleCreateReq;
import cn.iocoder.yudao.module.oa.api.dto.system.RoleRespVO;
import cn.iocoder.yudao.module.oa.api.dto.system.RoleUpdateReq;
import cn.iocoder.yudao.module.oa.dal.dataobject.auth.SysPermissionDO;
import cn.iocoder.yudao.module.oa.dal.dataobject.auth.SysRoleDO;
import cn.iocoder.yudao.module.oa.dal.dataobject.auth.SysRolePermissionDO;
import cn.iocoder.yudao.module.oa.dal.mysql.auth.SysPermissionMapper;
import cn.iocoder.yudao.module.oa.dal.mysql.auth.SysRoleMapper;
import cn.iocoder.yudao.module.oa.dal.mysql.auth.SysRolePermissionMapper;
import cn.iocoder.yudao.module.oa.dal.mysql.auth.SysUserRoleMapper;
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
public class RoleServiceImpl implements RoleService {

    private final SysRoleMapper sysRoleMapper;
    private final SysPermissionMapper sysPermissionMapper;
    private final SysRolePermissionMapper sysRolePermissionMapper;
    private final SysUserRoleMapper sysUserRoleMapper;

    @Override
    public PageResult<RoleRespVO> list(String name, String code, Integer pageNo, Integer pageSize) {
        Long tenantId = requireTenantId();
        LambdaQueryWrapper<SysRoleDO> wrapper = new LambdaQueryWrapper<SysRoleDO>()
                .eq(SysRoleDO::getTenantId, tenantId)
                .like(StrUtil.isNotBlank(name), SysRoleDO::getName, name)
                .like(StrUtil.isNotBlank(code), SysRoleDO::getCode, code)
                .orderByDesc(SysRoleDO::getId);
        Page<SysRoleDO> page = sysRoleMapper.selectPage(
                new Page<>(pageNo == null ? 1 : pageNo, pageSize == null ? 10 : pageSize), wrapper);
        List<RoleRespVO> list = page.getRecords().stream().map(this::toResp).collect(Collectors.toList());
        return new PageResult<>(list, page.getTotal());
    }

    @Override
    @Transactional
    @AuditLog(module = "M9-role", action = "create")
    public Long create(RoleCreateReq req) {
        Long tenantId = requireTenantId();
        assertCodeUnique(tenantId, req.getCode(), null);

        SysRoleDO entity = new SysRoleDO();
        entity.setTenantId(tenantId);
        entity.setCode(req.getCode());
        entity.setName(req.getName());
        entity.setRemark(req.getRemark());
        entity.setStatus("ENABLED");
        entity.setCreator(TenantContextHolder.getUsername());
        entity.setUpdater(TenantContextHolder.getUsername());
        entity.setCreateTime(LocalDateTime.now());
        entity.setUpdateTime(LocalDateTime.now());
        sysRoleMapper.insert(entity);
        return entity.getId();
    }

    @Override
    @Transactional
    @AuditLog(module = "M9-role", action = "update")
    public void update(RoleUpdateReq req) {
        SysRoleDO existing = getRequiredInTenant(req.getId());
        if (StrUtil.isNotBlank(req.getName())) {
            existing.setName(req.getName());
        }
        if (req.getRemark() != null) {
            existing.setRemark(req.getRemark());
        }
        existing.setUpdater(TenantContextHolder.getUsername());
        existing.setUpdateTime(LocalDateTime.now());
        sysRoleMapper.updateById(existing);
    }

    @Override
    @Transactional
    @AuditLog(module = "M9-role", action = "delete")
    public void delete(Long id) {
        SysRoleDO existing = getRequiredInTenant(id);
        if ("OA_ADMIN".equals(existing.getCode())) {
            throw new ServiceException(OaErrorCodes.ENTITY_ALREADY_BOUND.getCode(), "内置管理员角色不可删除");
        }
        long boundUsers = sysUserRoleMapper.selectCount(new LambdaQueryWrapper<cn.iocoder.yudao.module.oa.dal.dataobject.auth.SysUserRoleDO>()
                .eq(cn.iocoder.yudao.module.oa.dal.dataobject.auth.SysUserRoleDO::getRoleId, id));
        if (boundUsers > 0) {
            throw new ServiceException(OaErrorCodes.ENTITY_ALREADY_BOUND.getCode(), "角色已分配用户，不可删除");
        }
        sysRolePermissionMapper.deleteByRoleId(id);
        sysRoleMapper.deleteById(id);
    }

    @Override
    @Transactional
    @AuditLog(module = "M9-role", action = "assign-permission")
    public void assignPermission(RoleAssignPermissionReq req) {
        SysRoleDO role = getRequiredInTenant(req.getRoleId());
        for (Long permissionId : req.getPermissionIds()) {
            SysPermissionDO permission = sysPermissionMapper.selectById(permissionId);
            if (permission == null) {
                throw new ServiceException(OaErrorCodes.ENTITY_NOT_EXISTS.getCode(), "权限点不存在");
            }
        }
        sysRolePermissionMapper.deleteByRoleId(role.getId());
        for (Long permissionId : req.getPermissionIds()) {
            SysRolePermissionDO rel = new SysRolePermissionDO();
            rel.setRoleId(role.getId());
            rel.setPermissionId(permissionId);
            rel.setCreator(TenantContextHolder.getUsername());
            rel.setCreateTime(LocalDateTime.now());
            sysRolePermissionMapper.insert(rel);
        }
    }

    @Override
    public List<PermissionRespVO> listPermissions(Long roleId) {
        getRequiredInTenant(roleId);
        return sysPermissionMapper.selectByRoleId(roleId).stream().map(p -> {
            PermissionRespVO vo = new PermissionRespVO();
            vo.setId(p.getId());
            vo.setCode(p.getCode());
            vo.setName(p.getName());
            vo.setModule(p.getModule());
            return vo;
        }).collect(Collectors.toList());
    }

    private RoleRespVO toResp(SysRoleDO entity) {
        RoleRespVO vo = new RoleRespVO();
        vo.setId(entity.getId());
        vo.setCode(entity.getCode());
        vo.setName(entity.getName());
        vo.setStatus(entity.getStatus());
        vo.setRemark(entity.getRemark());
        vo.setCreateTime(entity.getCreateTime());
        List<SysPermissionDO> permissions = sysPermissionMapper.selectByRoleId(entity.getId());
        vo.setPermissionIds(permissions.stream().map(SysPermissionDO::getId).collect(Collectors.toList()));
        vo.setPermissionCodes(permissions.stream().map(SysPermissionDO::getCode).collect(Collectors.toList()));
        return vo;
    }

    private SysRoleDO getRequiredInTenant(Long id) {
        SysRoleDO entity = sysRoleMapper.selectById(id);
        if (entity == null) {
            throw new ServiceException(OaErrorCodes.ENTITY_NOT_EXISTS);
        }
        if (!requireTenantId().equals(entity.getTenantId())) {
            throw new ServiceException(OaErrorCodes.TENANT_FORBIDDEN);
        }
        return entity;
    }

    private void assertCodeUnique(Long tenantId, String code, Long excludeId) {
        LambdaQueryWrapper<SysRoleDO> wrapper = new LambdaQueryWrapper<SysRoleDO>()
                .eq(SysRoleDO::getTenantId, tenantId)
                .eq(SysRoleDO::getCode, code);
        if (excludeId != null) {
            wrapper.ne(SysRoleDO::getId, excludeId);
        }
        if (sysRoleMapper.selectCount(wrapper) > 0) {
            throw new ServiceException(OaErrorCodes.DUPLICATE_ENTITY.getCode(), "角色编码已存在");
        }
    }

    private Long requireTenantId() {
        Long tenantId = TenantContextHolder.getTenantId();
        if (tenantId == null) {
            throw new ServiceException(OaErrorCodes.UNAUTHORIZED);
        }
        return tenantId;
    }
}
