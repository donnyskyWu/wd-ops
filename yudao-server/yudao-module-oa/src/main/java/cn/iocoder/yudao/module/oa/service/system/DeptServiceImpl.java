package cn.iocoder.yudao.module.oa.service.system;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.exception.OaErrorCodes;
import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.oa.api.dto.system.DeptCreateReq;
import cn.iocoder.yudao.module.oa.api.dto.system.DeptTreeVO;
import cn.iocoder.yudao.module.oa.api.dto.system.DeptUpdateReq;
import cn.iocoder.yudao.module.oa.api.dto.system.DingTalkSyncResultVO;
import cn.iocoder.yudao.module.oa.dal.dataobject.auth.SysDeptDO;
import cn.iocoder.yudao.module.oa.dal.dataobject.auth.SysRoleDO;
import cn.iocoder.yudao.module.oa.dal.dataobject.auth.SysUserDO;
import cn.iocoder.yudao.module.oa.dal.dataobject.auth.SysUserRoleDO;
import cn.iocoder.yudao.module.oa.dal.mysql.auth.SysDeptMapper;
import cn.iocoder.yudao.module.oa.dal.mysql.auth.SysRoleMapper;
import cn.iocoder.yudao.module.oa.dal.mysql.auth.SysUserMapper;
import cn.iocoder.yudao.module.oa.dal.mysql.auth.SysUserRoleMapper;
import cn.iocoder.yudao.module.oa.framework.audit.AuditLog;
import cn.iocoder.yudao.module.oa.framework.dingtalk.DingTalkClient;
import cn.iocoder.yudao.module.oa.framework.dingtalk.DingTalkClient.DingDeptNode;
import cn.iocoder.yudao.module.oa.framework.dingtalk.DingTalkClient.DingUserDetail;
import cn.iocoder.yudao.module.oa.util.AesUtil;
import cn.hutool.crypto.digest.DigestUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class DeptServiceImpl implements DeptService {

    private static final long DING_ROOT_DEPT_ID = 1L;
    private static final String DEFAULT_SYNC_ROLE_CODE = "OPS_OPERATOR";

    private final SysDeptMapper sysDeptMapper;
    private final SysUserMapper sysUserMapper;
    private final SysRoleMapper sysRoleMapper;
    private final SysUserRoleMapper sysUserRoleMapper;
    private final DingTalkClient dingTalkClient;
    private final AesUtil aesUtil;

    @Override
    public List<DeptTreeVO> getTree() {
        Long tenantId = requireTenantId();
        List<SysDeptDO> all = sysDeptMapper.selectList(new LambdaQueryWrapper<SysDeptDO>()
                .eq(SysDeptDO::getTenantId, tenantId)
                .orderByAsc(SysDeptDO::getSort)
                .orderByAsc(SysDeptDO::getId));
        if (all.isEmpty()) {
            return Collections.emptyList();
        }
        Map<Long, List<SysDeptDO>> childrenMap = all.stream()
                .filter(d -> d.getParentId() != null)
                .collect(Collectors.groupingBy(SysDeptDO::getParentId));
        return all.stream()
                .filter(d -> d.getParentId() == null)
                .map(root -> toTreeNode(root, childrenMap))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    @AuditLog(module = "M9-dept", action = "create")
    public Long create(DeptCreateReq req) {
        Long tenantId = requireTenantId();
        if (req.getParentId() != null) {
            assertDeptInTenant(req.getParentId(), tenantId);
        }
        SysDeptDO entity = new SysDeptDO();
        entity.setTenantId(tenantId);
        entity.setParentId(req.getParentId());
        entity.setName(req.getName());
        entity.setSort(req.getSort() == null ? 0 : req.getSort());
        entity.setStatus(StrUtil.blankToDefault(req.getStatus(), "ENABLED"));
        entity.setCreator(TenantContextHolder.getUsername());
        entity.setUpdater(TenantContextHolder.getUsername());
        entity.setCreateTime(LocalDateTime.now());
        entity.setUpdateTime(LocalDateTime.now());
        sysDeptMapper.insert(entity);
        return entity.getId();
    }

    @Override
    @Transactional
    @AuditLog(module = "M9-dept", action = "update")
    public void update(DeptUpdateReq req) {
        Long tenantId = requireTenantId();
        SysDeptDO existing = assertDeptInTenant(req.getId(), tenantId);
        if (req.getParentId() != null) {
            if (req.getParentId().equals(req.getId())) {
                throw new ServiceException(OaErrorCodes.BAD_REQUEST.getCode(), "上级部门不能是自身");
            }
            assertDeptInTenant(req.getParentId(), tenantId);
            existing.setParentId(req.getParentId());
        }
        if (StrUtil.isNotBlank(req.getName())) {
            existing.setName(req.getName());
        }
        if (req.getSort() != null) {
            existing.setSort(req.getSort());
        }
        if (StrUtil.isNotBlank(req.getStatus())) {
            existing.setStatus(req.getStatus());
        }
        existing.setUpdater(TenantContextHolder.getUsername());
        existing.setUpdateTime(LocalDateTime.now());
        sysDeptMapper.updateById(existing);
    }

    @Override
    @Transactional
    @AuditLog(module = "M9-dept", action = "delete")
    public void delete(Long id) {
        Long tenantId = requireTenantId();
        assertDeptInTenant(id, tenantId);
        long childCount = sysDeptMapper.selectCount(new LambdaQueryWrapper<SysDeptDO>()
                .eq(SysDeptDO::getTenantId, tenantId)
                .eq(SysDeptDO::getParentId, id));
        if (childCount > 0) {
            throw new ServiceException(OaErrorCodes.ENTITY_ALREADY_BOUND.getCode(), "存在子部门，无法删除");
        }
        long userCount = sysUserMapper.selectCount(new LambdaQueryWrapper<SysUserDO>()
                .eq(SysUserDO::getTenantId, tenantId)
                .eq(SysUserDO::getDeptId, id));
        if (userCount > 0) {
            throw new ServiceException(OaErrorCodes.ENTITY_ALREADY_BOUND.getCode(), "部门下存在用户，无法删除");
        }
        sysDeptMapper.deleteById(id);
    }

    @Override
    @Transactional
    @AuditLog(module = "M9-dept", action = "sync-dingtalk")
    public DingTalkSyncResultVO syncDepartmentsFromDingTalk() {
        dingTalkClient.assertConfigured();
        Long tenantId = requireTenantId();
        DingTalkSyncResultVO result = new DingTalkSyncResultVO();

        Map<Long, Long> dingToLocal = loadDingDeptIdMap(tenantId);
        syncDeptRecursive(tenantId, DING_ROOT_DEPT_ID, null, dingToLocal, result);

        return result;
    }

    @Override
    @Transactional
    @AuditLog(module = "M9-dept", action = "sync-dingtalk-users")
    public DingTalkSyncResultVO syncUsersFromDingTalk() {
        dingTalkClient.assertConfigured();
        Long tenantId = requireTenantId();
        DingTalkSyncResultVO result = new DingTalkSyncResultVO();

        List<SysDeptDO> depts = sysDeptMapper.selectList(new LambdaQueryWrapper<SysDeptDO>()
                .eq(SysDeptDO::getTenantId, tenantId)
                .isNotNull(SysDeptDO::getDingDeptId));
        if (depts.isEmpty()) {
            throw new ServiceException(OaErrorCodes.BAD_REQUEST.getCode(), "请先同步钉钉部门");
        }

        Map<Long, Long> dingDeptToLocal = depts.stream()
                .collect(Collectors.toMap(SysDeptDO::getDingDeptId, SysDeptDO::getId, (a, b) -> a));
        Long defaultRoleId = resolveDefaultRoleId(tenantId);
        Set<String> processedUserIds = new HashSet<>();

        for (SysDeptDO dept : depts) {
            List<String> userIds;
            try {
                userIds = dingTalkClient.listUserIds(dept.getDingDeptId());
            } catch (ServiceException ex) {
                log.warn("listUserIds failed for dept {}: {}", dept.getDingDeptId(), ex.getMessage());
                result.setSkipped(result.getSkipped() + 1);
                continue;
            }
            for (String dingUserId : userIds) {
                if (!processedUserIds.add(dingUserId)) {
                    continue;
                }
                upsertDingUser(tenantId, dingUserId, dept.getId(), dingDeptToLocal, defaultRoleId, result);
            }
        }
        return result;
    }

    private void syncDeptRecursive(Long tenantId, long dingDeptId, Long localParentId,
                                   Map<Long, Long> dingToLocal, DingTalkSyncResultVO result) {
        List<DingDeptNode> children;
        try {
            children = dingTalkClient.listSubDepartments(dingDeptId);
        } catch (ServiceException ex) {
            log.warn("listSubDepartments failed for dingDeptId={}: {}", dingDeptId, ex.getMessage());
            result.setSkipped(result.getSkipped() + 1);
            return;
        }
        for (DingDeptNode node : children) {
            Long localId = dingToLocal.get(node.deptId());
            if (localId == null) {
                SysDeptDO entity = new SysDeptDO();
                entity.setTenantId(tenantId);
                entity.setParentId(localParentId);
                entity.setName(node.name());
                entity.setDingDeptId(node.deptId());
                entity.setSort(0);
                entity.setStatus("ENABLED");
                entity.setCreator("dingtalk-sync");
                entity.setUpdater("dingtalk-sync");
                entity.setCreateTime(LocalDateTime.now());
                entity.setUpdateTime(LocalDateTime.now());
                sysDeptMapper.insert(entity);
                localId = entity.getId();
                dingToLocal.put(node.deptId(), localId);
                result.setCreated(result.getCreated() + 1);
            } else {
                SysDeptDO existing = sysDeptMapper.selectById(localId);
                if (existing != null) {
                    existing.setName(node.name());
                    existing.setParentId(localParentId);
                    existing.setUpdater("dingtalk-sync");
                    existing.setUpdateTime(LocalDateTime.now());
                    sysDeptMapper.updateById(existing);
                    result.setUpdated(result.getUpdated() + 1);
                }
            }
            syncDeptRecursive(tenantId, node.deptId(), localId, dingToLocal, result);
        }
    }

    private void upsertDingUser(Long tenantId, String dingUserId, Long fallbackDeptId,
                                Map<Long, Long> dingDeptToLocal, Long defaultRoleId,
                                DingTalkSyncResultVO result) {
        DingUserDetail detail;
        try {
            detail = dingTalkClient.getUser(dingUserId);
        } catch (ServiceException ex) {
            log.warn("getUser failed for {}: {}", dingUserId, ex.getMessage());
            result.setSkipped(result.getSkipped() + 1);
            return;
        }
        if (detail == null || StrUtil.isBlank(detail.getName())) {
            result.setSkipped(result.getSkipped() + 1);
            return;
        }

        Long deptId = fallbackDeptId;
        if (detail.getPrimaryDeptId() != null && dingDeptToLocal.containsKey(detail.getPrimaryDeptId())) {
            deptId = dingDeptToLocal.get(detail.getPrimaryDeptId());
        }

        SysUserDO existing = sysUserMapper.selectOne(new LambdaQueryWrapper<SysUserDO>()
                .eq(SysUserDO::getTenantId, tenantId)
                .eq(SysUserDO::getDingUserId, dingUserId)
                .last("LIMIT 1"));

        if (existing == null) {
            existing = sysUserMapper.selectOne(new LambdaQueryWrapper<SysUserDO>()
                    .eq(SysUserDO::getTenantId, tenantId)
                    .eq(SysUserDO::getUsername, dingUserId)
                    .last("LIMIT 1"));
        }

        if (existing == null) {
            SysUserDO entity = new SysUserDO();
            entity.setTenantId(tenantId);
            entity.setUsername(dingUserId);
            entity.setNickname(detail.getName());
            entity.setDingUserId(dingUserId);
            entity.setDeptId(deptId);
            entity.setEmail(detail.getEmail());
            applyPhone(entity, detail.getMobile());
            entity.setStatus(detail.isActive() ? "ENABLED" : "DISABLED");
            entity.setRemark("钉钉同步");
            entity.setCreator("dingtalk-sync");
            entity.setUpdater("dingtalk-sync");
            entity.setCreateTime(LocalDateTime.now());
            entity.setUpdateTime(LocalDateTime.now());
            sysUserMapper.insert(entity);
            bindDefaultRole(entity.getId(), defaultRoleId);
            result.setCreated(result.getCreated() + 1);
        } else {
            existing.setNickname(detail.getName());
            existing.setDingUserId(dingUserId);
            existing.setDeptId(deptId);
            if (StrUtil.isNotBlank(detail.getEmail())) {
                existing.setEmail(detail.getEmail());
            }
            if (StrUtil.isNotBlank(detail.getMobile())) {
                applyPhone(existing, detail.getMobile());
            }
            existing.setStatus(detail.isActive() ? "ENABLED" : "DISABLED");
            existing.setUpdater("dingtalk-sync");
            existing.setUpdateTime(LocalDateTime.now());
            sysUserMapper.updateById(existing);
            result.setUpdated(result.getUpdated() + 1);
        }
    }

    private void bindDefaultRole(Long userId, Long roleId) {
        if (roleId == null) {
            return;
        }
        Long count = sysUserRoleMapper.selectCount(new LambdaQueryWrapper<SysUserRoleDO>()
                .eq(SysUserRoleDO::getUserId, userId)
                .eq(SysUserRoleDO::getRoleId, roleId));
        if (count == 0) {
            SysUserRoleDO rel = new SysUserRoleDO();
            rel.setUserId(userId);
            rel.setRoleId(roleId);
            rel.setCreator("dingtalk-sync");
            rel.setCreateTime(LocalDateTime.now());
            sysUserRoleMapper.insert(rel);
        }
    }

    private Long resolveDefaultRoleId(Long tenantId) {
        SysRoleDO role = sysRoleMapper.selectOne(new LambdaQueryWrapper<SysRoleDO>()
                .eq(SysRoleDO::getTenantId, tenantId)
                .eq(SysRoleDO::getCode, DEFAULT_SYNC_ROLE_CODE)
                .last("LIMIT 1"));
        return role != null ? role.getId() : null;
    }

    private Map<Long, Long> loadDingDeptIdMap(Long tenantId) {
        List<SysDeptDO> list = sysDeptMapper.selectList(new LambdaQueryWrapper<SysDeptDO>()
                .eq(SysDeptDO::getTenantId, tenantId)
                .isNotNull(SysDeptDO::getDingDeptId));
        Map<Long, Long> map = new HashMap<>();
        for (SysDeptDO dept : list) {
            map.put(dept.getDingDeptId(), dept.getId());
        }
        return map;
    }

    private void applyPhone(SysUserDO entity, String phone) {
        if (StrUtil.isBlank(phone)) {
            return;
        }
        entity.setPhoneEncrypted(aesUtil.encrypt(phone));
        entity.setPhoneHash(DigestUtil.sha256Hex(phone));
    }

    private DeptTreeVO toTreeNode(SysDeptDO dept, Map<Long, List<SysDeptDO>> childrenMap) {
        DeptTreeVO vo = new DeptTreeVO();
        vo.setId(dept.getId());
        vo.setParentId(dept.getParentId());
        vo.setName(dept.getName());
        vo.setDingDeptId(dept.getDingDeptId());
        vo.setSort(dept.getSort());
        vo.setStatus(dept.getStatus());
        List<SysDeptDO> children = childrenMap.getOrDefault(dept.getId(), Collections.emptyList());
        vo.setChildren(children.stream().map(c -> toTreeNode(c, childrenMap)).collect(Collectors.toList()));
        return vo;
    }

    private SysDeptDO assertDeptInTenant(Long id, Long tenantId) {
        SysDeptDO entity = sysDeptMapper.selectById(id);
        if (entity == null) {
            throw new ServiceException(OaErrorCodes.ENTITY_NOT_EXISTS);
        }
        if (!tenantId.equals(entity.getTenantId())) {
            throw new ServiceException(OaErrorCodes.TENANT_FORBIDDEN);
        }
        return entity;
    }

    private Long requireTenantId() {
        Long tenantId = TenantContextHolder.getTenantId();
        if (tenantId == null) {
            throw new ServiceException(OaErrorCodes.UNAUTHORIZED);
        }
        return tenantId;
    }
}
