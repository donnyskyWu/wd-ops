package cn.iocoder.yudao.module.oa.service.operations;

import cn.iocoder.yudao.framework.common.exception.OaErrorCodes;
import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.oa.api.dto.operations.OpsAnchorCreateReq;
import cn.iocoder.yudao.module.oa.api.dto.operations.OpsAnchorRelVO;
import cn.iocoder.yudao.module.oa.api.dto.operations.OpsAnchorStatsVO;
import cn.iocoder.yudao.module.oa.api.dto.operations.OpsAnchorUpdateReq;
import cn.iocoder.yudao.module.oa.dal.dataobject.ipgroup.IpGroupDO;
import cn.iocoder.yudao.module.oa.dal.dataobject.operations.OpsAnchorRelDO;
import cn.iocoder.yudao.module.oa.dal.mysql.ipgroup.IpGroupMapper;
import cn.iocoder.yudao.module.oa.dal.mysql.operations.OpsAnchorRelMapper;
import com.mzt.logapi.context.LogRecordContext;
import com.mzt.logapi.service.impl.DiffParseFunction;
import com.mzt.logapi.starter.annotation.LogRecord;

import static cn.iocoder.yudao.module.oa.framework.operatelog.OaLogRecordConstants.*;
import cn.iocoder.yudao.module.oa.service.support.FootballSystemUserValidator;
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
    private final FootballSystemUserValidator footballSystemUserValidator;
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
    @LogRecord(type = M1_OPS_ANCHOR_TYPE, subType = M1_OPS_ANCHOR_CREATE_SUB_TYPE, bizNo = "{{#anchorRel.id}}",
            success = M1_OPS_ANCHOR_CREATE_SUCCESS)
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
        LogRecordContext.putVariable("anchorRel", entity);
        return entity.getId();
    }

    @Override
    @Transactional
    @LogRecord(type = M1_OPS_ANCHOR_TYPE, subType = M1_OPS_ANCHOR_UPDATE_SUB_TYPE, bizNo = "{{#anchorRel.id}}",
            success = M1_OPS_ANCHOR_UPDATE_SUCCESS)
    public void update(OpsAnchorUpdateReq req) {
        OpsAnchorRelDO existing = requireRel(req.getId());
        LogRecordContext.putVariable(DiffParseFunction.OLD_OBJECT, toUpdateReq(existing));
        LogRecordContext.putVariable("anchorRel", existing);
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
    @LogRecord(type = M1_OPS_ANCHOR_TYPE, subType = M1_OPS_ANCHOR_DELETE_SUB_TYPE, bizNo = "{{#anchorRel.id}}",
            success = M1_OPS_ANCHOR_DELETE_SUCCESS)
    public void delete(Long id) {
        OpsAnchorRelDO existing = requireRel(id);
        LogRecordContext.putVariable("anchorRel", existing);
        opsAnchorRelMapper.deleteById(id);
    }

    @Override
    public OpsAnchorStatsVO anchorStats(Long userId) {
        Long tenantId = requireTenantId();
        footballSystemUserValidator.assertInTenant(userId, tenantId, "用户不存在");
        OpsAnchorStatsVO vo = new OpsAnchorStatsVO();
        vo.setOpsUserId(userId);
        vo.setOpsUserName(footballSystemUserValidator.resolveDisplayName(userId));
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
        Map<Long, String> userNames = footballSystemUserValidator.loadNicknames(userIds);
        vo.setOpsUserName(userNames.get(entity.getOpsUserId()));
        vo.setAnchorUserName(userNames.get(entity.getAnchorUserId()));
        if (entity.getIpGroupId() != null) {
            IpGroupDO group = ipGroupMapper.selectById(entity.getIpGroupId());
            if (group != null) {
                vo.setIpGroupName(group.getGroupName());
            }
        }
        return vo;
    }

    private void assertUsersExist(Long tenantId, Long opsUserId, Long anchorUserId) {
        footballSystemUserValidator.assertInTenant(opsUserId, tenantId, "用户不存在");
        footballSystemUserValidator.assertInTenant(anchorUserId, tenantId, "用户不存在");
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

    private static OpsAnchorUpdateReq toUpdateReq(OpsAnchorRelDO entity) {
        OpsAnchorUpdateReq req = new OpsAnchorUpdateReq();
        req.setId(entity.getId());
        req.setOpsUserId(entity.getOpsUserId());
        req.setAnchorUserId(entity.getAnchorUserId());
        req.setIpGroupId(entity.getIpGroupId());
        req.setStartDate(entity.getStartDate());
        req.setEndDate(entity.getEndDate());
        return req;
    }
}
