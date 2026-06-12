package cn.iocoder.yudao.module.oa.service.sop;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.exception.OaErrorCodes;
import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.oa.api.dto.sop.SopReviewActionReq;
import cn.iocoder.yudao.module.oa.api.dto.sop.SopReviewVO;
import cn.iocoder.yudao.module.oa.dal.dataobject.auth.SysUserDO;
import cn.iocoder.yudao.module.oa.dal.dataobject.sop.SopNodeDO;
import cn.iocoder.yudao.module.oa.dal.dataobject.sop.SopReviewDO;
import cn.iocoder.yudao.module.oa.dal.dataobject.sop.TaskDO;
import cn.iocoder.yudao.module.oa.dal.mysql.auth.SysUserMapper;
import cn.iocoder.yudao.module.oa.dal.mysql.sop.SopNodeMapper;
import cn.iocoder.yudao.module.oa.dal.mysql.sop.SopReviewMapper;
import cn.iocoder.yudao.module.oa.dal.mysql.sop.TaskMapper;
import cn.iocoder.yudao.module.oa.framework.audit.AuditLog;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SopReviewServiceImpl implements SopReviewService {

    private final SopReviewMapper sopReviewMapper;
    private final TaskMapper taskMapper;
    private final SopNodeMapper sopNodeMapper;
    private final SysUserMapper sysUserMapper;

    @Override
    public List<SopReviewVO> pending(Long reviewerId) {
        Long tenantId = requireTenantId();
        Long currentUserId = reviewerId != null ? reviewerId : TenantContextHolder.getUserId();
        SysUserDO user = sysUserMapper.selectById(currentUserId);
        if (user == null) {
            throw new ServiceException(OaErrorCodes.ENTITY_NOT_EXISTS);
        }
        String position = user.getPosition();
        return sopReviewMapper.selectList(new LambdaQueryWrapper<SopReviewDO>()
                        .eq(SopReviewDO::getTenantId, tenantId)
                        .eq(SopReviewDO::getStatus, "PENDING")
                        .and(w -> w.eq(SopReviewDO::getReviewerId, currentUserId)
                                .or()
                                .eq(StrUtil.isNotBlank(position), SopReviewDO::getReviewerRole, position))
                        .orderByDesc(SopReviewDO::getId))
                .stream()
                .map(this::toVO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    @AuditLog(module = "M2-sop", action = "approve-review")
    public void approve(SopReviewActionReq req) {
        SopReviewDO review = requirePendingReview(req.getReviewId());
        validateReviewer(review);
        review.setStatus("APPROVED");
        review.setComment(req.getComment());
        review.setReviewerId(TenantContextHolder.getUserId());
        review.setUpdater(TenantContextHolder.getUsername());
        review.setUpdateTime(LocalDateTime.now());
        sopReviewMapper.updateById(review);

        TaskDO task = taskMapper.selectById(review.getTaskId());
        if (task != null) {
            task.setStatus("DONE");
            task.setUpdater(TenantContextHolder.getUsername());
            task.setUpdateTime(LocalDateTime.now());
            taskMapper.updateById(task);
        }
    }

    @Override
    @Transactional
    @AuditLog(module = "M2-sop", action = "reject-review")
    public void reject(SopReviewActionReq req) {
        SopReviewDO review = requirePendingReview(req.getReviewId());
        validateReviewer(review);
        review.setStatus("REJECTED");
        review.setComment(req.getComment());
        review.setReviewerId(TenantContextHolder.getUserId());
        review.setUpdater(TenantContextHolder.getUsername());
        review.setUpdateTime(LocalDateTime.now());
        sopReviewMapper.updateById(review);

        TaskDO task = taskMapper.selectById(review.getTaskId());
        if (task != null) {
            task.setStatus("IN_PROGRESS");
            task.setUpdater(TenantContextHolder.getUsername());
            task.setUpdateTime(LocalDateTime.now());
            taskMapper.updateById(task);
        }
    }

    private void validateReviewer(SopReviewDO review) {
        Long userId = TenantContextHolder.getUserId();
        SysUserDO user = sysUserMapper.selectById(userId);
        if (user == null) {
            throw new ServiceException(OaErrorCodes.UNAUTHORIZED);
        }
        if (StrUtil.isNotBlank(review.getReviewerRole())
                && !review.getReviewerRole().equals(user.getPosition())
                && !Objects.equals(review.getReviewerId(), userId)) {
            throw new ServiceException(OaErrorCodes.REVIEWER_POSITION_MISMATCH);
        }
    }

    private SopReviewVO toVO(SopReviewDO entity) {
        SopReviewVO vo = new SopReviewVO();
        vo.setId(entity.getId());
        vo.setTaskId(entity.getTaskId());
        vo.setReviewerId(entity.getReviewerId());
        vo.setReviewerRole(entity.getReviewerRole());
        vo.setStatus(entity.getStatus());
        vo.setComment(entity.getComment());
        vo.setCreateTime(entity.getCreateTime());
        TaskDO task = taskMapper.selectById(entity.getTaskId());
        if (task != null) {
            vo.setPlanName(task.getPlanName());
            SopNodeDO node = sopNodeMapper.selectById(task.getNodeId());
            if (node != null) {
                vo.setNodeName(node.getNodeName());
            }
        }
        return vo;
    }

    private SopReviewDO requirePendingReview(Long id) {
        SopReviewDO entity = sopReviewMapper.selectById(id);
        if (entity == null) {
            throw new ServiceException(OaErrorCodes.ENTITY_NOT_EXISTS);
        }
        if (!Objects.equals(entity.getTenantId(), requireTenantId())) {
            throw new ServiceException(OaErrorCodes.TENANT_FORBIDDEN);
        }
        if (!"PENDING".equals(entity.getStatus())) {
            throw new ServiceException(OaErrorCodes.TASK_STATUS_INVALID);
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
