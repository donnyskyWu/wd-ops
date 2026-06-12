package cn.iocoder.yudao.module.oa.service.system;

import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.digest.DigestUtil;
import cn.iocoder.yudao.framework.common.exception.OaErrorCodes;
import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.oa.api.dto.system.UserCreateReq;
import cn.iocoder.yudao.module.oa.api.dto.system.UserRespVO;
import cn.iocoder.yudao.module.oa.api.dto.system.UserUpdateReq;
import cn.iocoder.yudao.module.oa.dal.dataobject.auth.SysDeptDO;
import cn.iocoder.yudao.module.oa.dal.dataobject.auth.SysRoleDO;
import cn.iocoder.yudao.module.oa.dal.dataobject.auth.SysUserDO;
import cn.iocoder.yudao.module.oa.dal.dataobject.auth.SysUserRoleDO;
import cn.iocoder.yudao.module.oa.dal.mysql.auth.SysDeptMapper;
import cn.iocoder.yudao.module.oa.dal.mysql.auth.SysRoleMapper;
import cn.iocoder.yudao.module.oa.dal.mysql.auth.SysUserMapper;
import cn.iocoder.yudao.module.oa.dal.mysql.auth.SysUserRoleMapper;
import cn.iocoder.yudao.module.oa.framework.audit.AuditLog;
import cn.iocoder.yudao.module.oa.util.AesUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final SysUserMapper sysUserMapper;
    private final SysDeptMapper sysDeptMapper;
    private final SysRoleMapper sysRoleMapper;
    private final SysUserRoleMapper sysUserRoleMapper;
    private final AesUtil aesUtil;

    @Override
    public PageResult<UserRespVO> list(String username, String nickname, Long roleId, Long deptId, String status,
                                       Integer pageNo, Integer pageSize) {
        Long tenantId = requireTenantId();
        LambdaQueryWrapper<SysUserDO> wrapper = new LambdaQueryWrapper<SysUserDO>()
                .eq(SysUserDO::getTenantId, tenantId)
                .like(StrUtil.isNotBlank(username), SysUserDO::getUsername, username)
                .like(StrUtil.isNotBlank(nickname), SysUserDO::getNickname, nickname)
                .eq(deptId != null, SysUserDO::getDeptId, deptId)
                .eq(StrUtil.isNotBlank(status), SysUserDO::getStatus, status)
                .orderByDesc(SysUserDO::getId);

        if (roleId != null) {
            List<Long> userIds = sysUserRoleMapper.selectList(new LambdaQueryWrapper<SysUserRoleDO>()
                            .eq(SysUserRoleDO::getRoleId, roleId))
                    .stream().map(SysUserRoleDO::getUserId).distinct().toList();
            if (userIds.isEmpty()) {
                return new PageResult<>(Collections.emptyList(), 0L);
            }
            wrapper.in(SysUserDO::getId, userIds);
        }

        Page<SysUserDO> page = sysUserMapper.selectPage(
                new Page<>(pageNo == null ? 1 : pageNo, pageSize == null ? 10 : pageSize), wrapper);
        List<UserRespVO> list = page.getRecords().stream().map(this::toResp).collect(Collectors.toList());
        return new PageResult<>(list, page.getTotal());
    }

    @Override
    @Transactional
    @AuditLog(module = "M9-user", action = "create")
    public Long create(UserCreateReq req) {
        Long tenantId = requireTenantId();
        assertUsernameUnique(tenantId, req.getUsername(), null);
        assertRolesInTenant(tenantId, req.getRoleIds());
        if (req.getDeptId() != null) {
            assertDeptInTenant(tenantId, req.getDeptId());
        }

        SysUserDO entity = new SysUserDO();
        entity.setTenantId(tenantId);
        entity.setUsername(req.getUsername());
        entity.setNickname(req.getNickname());
        entity.setEmail(req.getEmail());
        applyPhone(entity, req.getPhone());
        entity.setPosition(req.getPosition());
        entity.setIpGroupId(req.getIpGroupId());
        entity.setDeptId(req.getDeptId());
        entity.setRemark(req.getRemark());
        entity.setStatus(StrUtil.blankToDefault(req.getStatus(), "ENABLED"));
        entity.setCreator(TenantContextHolder.getUsername());
        entity.setUpdater(TenantContextHolder.getUsername());
        entity.setCreateTime(LocalDateTime.now());
        entity.setUpdateTime(LocalDateTime.now());
        sysUserMapper.insert(entity);

        bindRoles(entity.getId(), req.getRoleIds());
        return entity.getId();
    }

    @Override
    @Transactional
    @AuditLog(module = "M9-user", action = "update")
    public void update(UserUpdateReq req) {
        SysUserDO existing = getRequiredInTenant(req.getId());
        if (StrUtil.isNotBlank(req.getNickname())) {
            existing.setNickname(req.getNickname());
        }
        if (req.getEmail() != null) {
            existing.setEmail(req.getEmail());
        }
        if (req.getPhone() != null) {
            applyPhone(existing, req.getPhone());
        }
        if (req.getPosition() != null) {
            existing.setPosition(req.getPosition());
        }
        if (req.getIpGroupId() != null) {
            existing.setIpGroupId(req.getIpGroupId());
        }
        if (req.getDeptId() != null) {
            assertDeptInTenant(existing.getTenantId(), req.getDeptId());
            existing.setDeptId(req.getDeptId());
        }
        if (req.getRemark() != null) {
            existing.setRemark(req.getRemark());
        }
        if (StrUtil.isNotBlank(req.getStatus())) {
            existing.setStatus(req.getStatus());
        }
        existing.setUpdater(TenantContextHolder.getUsername());
        existing.setUpdateTime(LocalDateTime.now());
        sysUserMapper.updateById(existing);

        if (req.getRoleIds() != null) {
            assertRolesInTenant(existing.getTenantId(), req.getRoleIds());
            sysUserRoleMapper.deleteByUserId(existing.getId());
            bindRoles(existing.getId(), req.getRoleIds());
        }
    }

    @Override
    @Transactional
    @AuditLog(module = "M9-user", action = "delete")
    public void delete(Long id) {
        SysUserDO existing = getRequiredInTenant(id);
        if ("oa-admin".equals(existing.getUsername())) {
            throw new ServiceException(OaErrorCodes.ENTITY_ALREADY_BOUND.getCode(), "内置管理员不可删除");
        }
        sysUserRoleMapper.deleteByUserId(id);
        sysUserMapper.deleteById(id);
    }

    @Override
    public UserRespVO profile() {
        return toResp(getRequiredInTenant(TenantContextHolder.getUserId()));
    }

    private void bindRoles(Long userId, List<Long> roleIds) {
        for (Long roleId : roleIds) {
            SysUserRoleDO rel = new SysUserRoleDO();
            rel.setUserId(userId);
            rel.setRoleId(roleId);
            rel.setCreator(TenantContextHolder.getUsername());
            rel.setCreateTime(LocalDateTime.now());
            sysUserRoleMapper.insert(rel);
        }
    }

    private UserRespVO toResp(SysUserDO entity) {
        UserRespVO vo = new UserRespVO();
        vo.setId(entity.getId());
        vo.setUsername(entity.getUsername());
        vo.setNickname(entity.getNickname());
        vo.setEmail(entity.getEmail());
        vo.setPhoneMasked(maskPhone(entity.getPhoneEncrypted()));
        vo.setPosition(entity.getPosition());
        vo.setIpGroupId(entity.getIpGroupId());
        vo.setDeptId(entity.getDeptId());
        vo.setDingUserId(entity.getDingUserId());
        if (entity.getDeptId() != null) {
            SysDeptDO dept = sysDeptMapper.selectById(entity.getDeptId());
            if (dept != null) {
                vo.setDeptName(dept.getName());
            }
        }
        vo.setStatus(entity.getStatus());
        vo.setRemark(entity.getRemark());
        vo.setCreateTime(entity.getCreateTime());

        List<Long> roleIds = sysUserRoleMapper.selectRoleIdsByUserId(entity.getId());
        vo.setRoleIds(roleIds);
        List<String> roleNames = new ArrayList<>();
        for (Long roleId : roleIds) {
            SysRoleDO role = sysRoleMapper.selectById(roleId);
            if (role != null) {
                roleNames.add(role.getName());
            }
        }
        vo.setRoleNames(roleNames);
        return vo;
    }

    private void applyPhone(SysUserDO entity, String phone) {
        if (StrUtil.isBlank(phone)) {
            entity.setPhoneEncrypted(null);
            entity.setPhoneHash(null);
            return;
        }
        entity.setPhoneEncrypted(aesUtil.encrypt(phone));
        entity.setPhoneHash(DigestUtil.sha256Hex(phone));
    }

    private String maskPhone(String encrypted) {
        if (StrUtil.isBlank(encrypted)) {
            return null;
        }
        try {
            String plain = aesUtil.decrypt(encrypted);
            if (plain.length() < 7) {
                return "****";
            }
            return plain.substring(0, 3) + "****" + plain.substring(plain.length() - 4);
        } catch (Exception ex) {
            return "****";
        }
    }

    private SysUserDO getRequiredInTenant(Long id) {
        SysUserDO entity = sysUserMapper.selectById(id);
        if (entity == null) {
            throw new ServiceException(OaErrorCodes.ENTITY_NOT_EXISTS);
        }
        if (!requireTenantId().equals(entity.getTenantId())) {
            throw new ServiceException(OaErrorCodes.TENANT_FORBIDDEN);
        }
        return entity;
    }

    private void assertUsernameUnique(Long tenantId, String username, Long excludeId) {
        LambdaQueryWrapper<SysUserDO> wrapper = new LambdaQueryWrapper<SysUserDO>()
                .eq(SysUserDO::getTenantId, tenantId)
                .eq(SysUserDO::getUsername, username);
        if (excludeId != null) {
            wrapper.ne(SysUserDO::getId, excludeId);
        }
        if (sysUserMapper.selectCount(wrapper) > 0) {
            throw new ServiceException(OaErrorCodes.DUPLICATE_ENTITY.getCode(), "用户名已存在");
        }
    }

    private void assertDeptInTenant(Long tenantId, Long deptId) {
        SysDeptDO dept = sysDeptMapper.selectById(deptId);
        if (dept == null) {
            throw new ServiceException(OaErrorCodes.ENTITY_NOT_EXISTS.getCode(), "部门不存在");
        }
        if (!tenantId.equals(dept.getTenantId())) {
            throw new ServiceException(OaErrorCodes.TENANT_FORBIDDEN);
        }
    }

    private void assertRolesInTenant(Long tenantId, List<Long> roleIds) {
        for (Long roleId : roleIds) {
            SysRoleDO role = sysRoleMapper.selectById(roleId);
            if (role == null) {
                throw new ServiceException(OaErrorCodes.ENTITY_NOT_EXISTS.getCode(), "角色不存在");
            }
            if (!tenantId.equals(role.getTenantId())) {
                throw new ServiceException(OaErrorCodes.TENANT_FORBIDDEN);
            }
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
