package cn.iocoder.yudao.module.oa.service.sop;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.exception.OaErrorCodes;
import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.oa.api.dto.sop.TaskCompleteReq;
import cn.iocoder.yudao.module.oa.api.dto.sop.TaskCreateReq;
import cn.iocoder.yudao.module.oa.api.dto.sop.TaskVO;
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
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TaskServiceImpl implements TaskService {

    private final TaskMapper taskMapper;
    private final SopNodeMapper sopNodeMapper;
    private final SopReviewMapper sopReviewMapper;
    private final SysUserMapper sysUserMapper;
    private final SopTemplateServiceImpl sopTemplateService;

    @Override
    public PageResult<TaskVO> list(Long ipGroupId, String status, Long executorId,
                                   LocalDate startDate, LocalDate endDate, Integer pageNum, Integer pageSize) {
        Long tenantId = requireTenantId();
        LambdaQueryWrapper<TaskDO> wrapper = new LambdaQueryWrapper<TaskDO>()
                .eq(TaskDO::getTenantId, tenantId)
                .eq(TaskDO::getVisibleInList, 1)
                .eq(ipGroupId != null, TaskDO::getIpGroupId, ipGroupId)
                .eq(StrUtil.isNotBlank(status), TaskDO::getStatus, status)
                .eq(executorId != null, TaskDO::getAssigneeId, executorId)
                .ge(startDate != null, TaskDO::getCreateTime, startDate == null ? null : startDate.atStartOfDay())
                .le(endDate != null, TaskDO::getCreateTime, endDate == null ? null : endDate.atTime(LocalTime.MAX))
                .orderByDesc(TaskDO::getId);
        Page<TaskDO> page = taskMapper.selectPage(
                new Page<>(pageNum == null ? 1 : pageNum, pageSize == null ? 20 : pageSize), wrapper);
        return new PageResult<>(page.getRecords().stream().map(this::toVO).collect(Collectors.toList()), page.getTotal());
    }

    @Override
    public PageResult<TaskVO> myTasks(Integer pageNum, Integer pageSize) {
        Long userId = TenantContextHolder.getUserId();
        if (userId == null) {
            throw new ServiceException(OaErrorCodes.UNAUTHORIZED);
        }
        return list(null, null, userId, null, null, pageNum, pageSize);
    }

    @Override
    @Transactional
    @AuditLog(module = "M2-task", action = "create")
    public Long create(TaskCreateReq req) {
        Long tenantId = requireTenantId();
        sopTemplateService.requireTemplate(req.getTemplateId());
        SopNodeDO node = sopNodeMapper.selectById(req.getNodeId());
        if (node == null || !req.getTemplateId().equals(node.getTemplateId())) {
            throw new ServiceException(OaErrorCodes.ENTITY_NOT_EXISTS);
        }
        TaskDO entity = new TaskDO();
        entity.setTenantId(tenantId);
        entity.setTemplateId(req.getTemplateId());
        entity.setNodeId(req.getNodeId());
        entity.setPlanName(req.getPlanName());
        entity.setAssigneeId(req.getAssigneeId());
        entity.setIpGroupId(req.getIpGroupId());
        entity.setAuthorId(req.getAuthorId());
        entity.setStatus("PENDING");
        entity.setNeedReview(req.getNeedReview() != null ? req.getNeedReview() : node.getNeedReview());
        entity.setCreator(TenantContextHolder.getUsername());
        entity.setUpdater(TenantContextHolder.getUsername());
        entity.setCreateTime(LocalDateTime.now());
        entity.setUpdateTime(LocalDateTime.now());
        taskMapper.insert(entity);
        return entity.getId();
    }

    @Override
    @Transactional
    @AuditLog(module = "M2-task", action = "start")
    public void start(Long id) {
        TaskDO task = requireTask(id);
        requireAssignee(task);
        if (!"PENDING".equals(task.getStatus())) {
            throw new ServiceException(OaErrorCodes.TASK_STATUS_INVALID);
        }
        task.setStatus("IN_PROGRESS");
        task.setStartTime(LocalDateTime.now());
        task.setUpdater(TenantContextHolder.getUsername());
        task.setUpdateTime(LocalDateTime.now());
        taskMapper.updateById(task);
    }

    @Override
    @Transactional
    @AuditLog(module = "M2-task", action = "complete")
    public void complete(Long id, TaskCompleteReq req) {
        TaskDO task = requireTask(id);
        requireAssignee(task);
        if (!"IN_PROGRESS".equals(task.getStatus())) {
            throw new ServiceException(OaErrorCodes.TASK_STATUS_INVALID);
        }
        task.setCompleteTime(LocalDateTime.now());
        task.setDeliverables(req == null ? null : req.getDeliverables());
        if (task.getNeedReview() != null && task.getNeedReview() == 1) {
            task.setStatus("COMPLETED");
        } else {
            task.setStatus("DONE");
        }
        task.setUpdater(TenantContextHolder.getUsername());
        task.setUpdateTime(LocalDateTime.now());
        taskMapper.updateById(task);
    }

    @Override
    @Transactional
    @AuditLog(module = "M2-task", action = "submit-review")
    public void submitReview(Long id) {
        TaskDO task = requireTask(id);
        requireAssignee(task);
        if (!"COMPLETED".equals(task.getStatus())) {
            throw new ServiceException(OaErrorCodes.TASK_STATUS_INVALID);
        }
        task.setStatus("PENDING_REVIEW");
        task.setUpdater(TenantContextHolder.getUsername());
        task.setUpdateTime(LocalDateTime.now());
        taskMapper.updateById(task);

        SopNodeDO node = sopNodeMapper.selectById(task.getNodeId());
        SopReviewDO review = new SopReviewDO();
        review.setTenantId(task.getTenantId());
        review.setTaskId(task.getId());
        review.setReviewerRole(node != null ? node.getReviewerRole() : null);
        review.setStatus("PENDING");
        review.setCreator(TenantContextHolder.getUsername());
        review.setUpdater(TenantContextHolder.getUsername());
        review.setCreateTime(LocalDateTime.now());
        review.setUpdateTime(LocalDateTime.now());
        sopReviewMapper.insert(review);
    }

    private TaskVO toVO(TaskDO entity) {
        TaskVO vo = new TaskVO();
        vo.setId(entity.getId());
        vo.setTemplateId(entity.getTemplateId());
        vo.setNodeId(entity.getNodeId());
        vo.setPlanName(entity.getPlanName());
        vo.setAssigneeId(entity.getAssigneeId());
        SysUserDO user = sysUserMapper.selectById(entity.getAssigneeId());
        if (user != null) {
            vo.setAssigneeName(user.getNickname() != null ? user.getNickname() : user.getUsername());
        }
        SopNodeDO node = sopNodeMapper.selectById(entity.getNodeId());
        if (node != null) {
            vo.setNodeName(node.getNodeName());
            vo.setExecutorRole(node.getExecutorRole());
        }
        vo.setStatus(entity.getStatus());
        vo.setNeedReview(entity.getNeedReview());
        vo.setSlaDeadline(entity.getSlaDeadline());
        vo.setDeliverables(entity.getDeliverables());
        vo.setStartTime(entity.getStartTime());
        vo.setCompleteTime(entity.getCompleteTime());
        return vo;
    }

    private void requireAssignee(TaskDO task) {
        Long userId = TenantContextHolder.getUserId();
        if (userId == null || !Objects.equals(userId, task.getAssigneeId())) {
            throw new ServiceException(OaErrorCodes.TASK_ASSIGNEE_MISMATCH);
        }
    }

    TaskDO requireTask(Long id) {
        TaskDO entity = taskMapper.selectById(id);
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
