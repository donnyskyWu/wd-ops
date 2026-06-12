package cn.iocoder.yudao.module.oa.service.operations;

import cn.iocoder.yudao.framework.common.exception.OaErrorCodes;
import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.oa.api.dto.operations.OpsAnchorCreateReq;
import cn.iocoder.yudao.module.oa.api.dto.operations.OpsAnchorRelVO;
import cn.iocoder.yudao.module.oa.api.dto.operations.OpsAnchorStatsVO;
import cn.iocoder.yudao.module.oa.api.dto.operations.OpsAnchorUpdateReq;
import cn.iocoder.yudao.module.oa.dal.dataobject.auth.SysUserDO;
import cn.iocoder.yudao.module.oa.dal.dataobject.ipgroup.IpGroupDO;
import cn.iocoder.yudao.module.oa.dal.dataobject.operations.OpsAnchorRelDO;
import cn.iocoder.yudao.module.oa.dal.mysql.auth.SysUserMapper;
import cn.iocoder.yudao.module.oa.dal.mysql.ipgroup.IpGroupMapper;
import cn.iocoder.yudao.module.oa.dal.mysql.operations.OpsAnchorRelMapper;
import cn.iocoder.yudao.module.oa.framework.audit.AuditLog;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OpsAnchorServiceImpl implements OpsAnchorService {

    private final OpsAnchorRelMapper opsAnchorRelMapper;
    private final SysUserMapper sysUserMapper;
    private final IpGroupMapper ipGroupMapper;

    @Override
    public PageResult<OpsAnchorRelVO> list(Long opsUserId, Long anchorUserId, Integer pageNo, Integer pageSize) {
        Long tenantId = requireTenantId();
        LambdaQueryWrapper<OpsAnchorRelDO> wrapper = new LambdaQueryWrapper<OpsAnchorRelDO>()
                .eq(OpsAnchorRelDO::getTenantId, tenantId)
                .eq(opsUserId != null, OpsAnchorRelDO::getOpsUserId, opsUserId)
                .eq(anchorUserId != null, OpsAnchorRelDO::getAnchorUserId, anchorUserId)
                .orderByDesc(OpsAnchorRelDO::getId);
        Page<OpsAnchorRelDO> page = opsAnchorRelMapper.selectPage(
                new Page<>(pageNo == null ? 1 : pageNo, pageSize == null ? 20 : pageSize), wrapper);
        return new PageResult<>(page.getRecords().stream().map(this::toVO).collect(Collectors.toList()), page.getTotal());
    }

    @Override
    @Transactional
    @AuditLog(module = "M1-ops-anchor", action = "create")
    public Long create(OpsAnchorCreateReq req) {
        Long tenantId = requireTenantId();
        assertUsersExist(tenantId, req.getOpsUserId(), req.getAnchorUserId());
        assertNoOverlap(tenantId, req.getOpsUserId(), req.getAnchorUserId(), req.getStartDate(), req.getEndDate(), null);

        OpsAnchorRelDO entity = new OpsAnchorRelDO();
        entity.setTenantId(tenantId);
        entity.setOpsUserId(req.getOpsUserId());
        entity.setAnchorUserId(req.getAnchorUserId());
        entity.setIpGroupId(req.getIpGroupId());
        entity.setStartDate(req.getStartDate());
        entity.setEndDate(req.getEndDate());
        entity.setCreator(TenantContextHolder.getUsername());
        entity.setUpdater(TenantContextHolder.getUsername());
        entity.setCreateTime(LocalDateTime.now());
        entity.setUpdateTime(LocalDateTime.now());
        opsAnchorRelMapper.insert(entity);
        return entity.getId();
    }

    @Override
    @Transactional
    @AuditLog(module = "M1-ops-anchor", action = "update")
    public void update(OpsAnchorUpdateReq req) {
        OpsAnchorRelDO existing = requireRel(req.getId());
        Long opsUserId = req.getOpsUserId() != null ? req.getOpsUserId() : existing.getOpsUserId();
        Long anchorUserId = req.getAnchorUserId() != null ? req.getAnchorUserId() : existing.getAnchorUserId();
        LocalDate startDate = req.getStartDate() != null ? req.getStartDate() : existing.getStartDate();
        LocalDate endDate = req.getEndDate() != null ? req.getEndDate() : existing.getEndDate();
        assertNoOverlap(existing.getTenantId(), opsUserId, anchorUserId, startDate, endDate, existing.getId());

        if (req.getOpsUserId() != null) {
            existing.setOpsUserId(req.getOpsUserId());
        }
        if (req.getAnchorUserId() != null) {
            existing.setAnchorUserId(req.getAnchorUserId());
        }
        if (req.getIpGroupId() != null) {
            existing.setIpGroupId(req.getIpGroupId());
        }
        if (req.getStartDate() != null) {
            existing.setStartDate(req.getStartDate());
        }
        if (req.getEndDate() != null) {
            existing.setEndDate(req.getEndDate());
        }
        existing.setUpdater(TenantContextHolder.getUsername());
        existing.setUpdateTime(LocalDateTime.now());
        opsAnchorRelMapper.updateById(existing);
    }

    @Override
    @Transactional
    @AuditLog(module = "M1-ops-anchor", action = "delete")
    public void delete(Long id) {
        requireRel(id);
        opsAnchorRelMapper.deleteById(id);
    }

    @Override
    public OpsAnchorStatsVO anchorStats(Long userId) {
        Long tenantId = requireTenantId();
        SysUserDO user = sysUserMapper.selectById(userId);
        if (user == null || !Objects.equals(user.getTenantId(), tenantId)) {
            throw new ServiceException(OaErrorCodes.ENTITY_NOT_EXISTS);
        }
        OpsAnchorStatsVO vo = new OpsAnchorStatsVO();
        vo.setOpsUserId(userId);
        vo.setOpsUserName(user.getNickname() != null ? user.getNickname() : user.getUsername());
        long anchorCount = opsAnchorRelMapper.selectCount(new LambdaQueryWrapper<OpsAnchorRelDO>()
                .eq(OpsAnchorRelDO::getTenantId, tenantId)
                .eq(OpsAnchorRelDO::getOpsUserId, userId));
        vo.setAnchorCount(Math.toIntExact(anchorCount));
        return vo;
    }

    private OpsAnchorRelVO toVO(OpsAnchorRelDO entity) {
        OpsAnchorRelVO vo = new OpsAnchorRelVO();
        vo.setId(entity.getId());
        vo.setOpsUserId(entity.getOpsUserId());
        vo.setAnchorUserId(entity.getAnchorUserId());
        vo.setIpGroupId(entity.getIpGroupId());
        vo.setStartDate(entity.getStartDate());
        vo.setEndDate(entity.getEndDate());
        vo.setCreateTime(entity.getCreateTime());

        Set<Long> userIds = Set.of(entity.getOpsUserId(), entity.getAnchorUserId());
        Map<Long, SysUserDO> userMap = sysUserMapper.selectList(new LambdaQueryWrapper<SysUserDO>()
                        .in(SysUserDO::getId, userIds))
                .stream()
                .collect(Collectors.toMap(SysUserDO::getId, u -> u, (a, b) -> a));
        SysUserDO ops = userMap.get(entity.getOpsUserId());
        if (ops != null) {
            vo.setOpsUserName(ops.getNickname() != null ? ops.getNickname() : ops.getUsername());
        }
        SysUserDO anchor = userMap.get(entity.getAnchorUserId());
        if (anchor != null) {
            vo.setAnchorUserName(anchor.getNickname() != null ? anchor.getNickname() : anchor.getUsername());
        }
        if (entity.getIpGroupId() != null) {
            IpGroupDO group = ipGroupMapper.selectById(entity.getIpGroupId());
            if (group != null) {
                vo.setIpGroupName(group.getGroupName());
            }
        }
        return vo;
    }

    private void assertUsersExist(Long tenantId, Long opsUserId, Long anchorUserId) {
        for (Long userId : List.of(opsUserId, anchorUserId)) {
            SysUserDO user = sysUserMapper.selectById(userId);
            if (user == null || !Objects.equals(user.getTenantId(), tenantId)) {
                throw new ServiceException(OaErrorCodes.ENTITY_NOT_EXISTS);
            }
        }
    }

    private void assertNoOverlap(Long tenantId, Long opsUserId, Long anchorUserId,
                                 LocalDate startDate, LocalDate endDate, Long excludeId) {
        LambdaQueryWrapper<OpsAnchorRelDO> wrapper = new LambdaQueryWrapper<OpsAnchorRelDO>()
                .eq(OpsAnchorRelDO::getTenantId, tenantId)
                .eq(OpsAnchorRelDO::getOpsUserId, opsUserId)
                .eq(OpsAnchorRelDO::getAnchorUserId, anchorUserId)
                .le(OpsAnchorRelDO::getStartDate, endDate)
                .ge(OpsAnchorRelDO::getEndDate, startDate);
        if (excludeId != null) {
            wrapper.ne(OpsAnchorRelDO::getId, excludeId);
        }
        if (opsAnchorRelMapper.selectCount(wrapper) > 0) {
            throw new ServiceException(OaErrorCodes.OPS_ANCHOR_OVERLAP);
        }
    }

    private OpsAnchorRelDO requireRel(Long id) {
        OpsAnchorRelDO entity = opsAnchorRelMapper.selectById(id);
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
