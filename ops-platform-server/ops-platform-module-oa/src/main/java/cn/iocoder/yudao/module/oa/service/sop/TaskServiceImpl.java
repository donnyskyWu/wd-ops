package cn.iocoder.yudao.module.oa.service.sop;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.exception.OaErrorCodes;
import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.oa.api.dto.sop.TaskAttachmentVO;
import cn.iocoder.yudao.module.oa.api.dto.sop.TaskCompleteReq;
import cn.iocoder.yudao.module.oa.api.dto.sop.TaskCreateReq;
import cn.iocoder.yudao.module.oa.api.dto.sop.TaskExecuteSaveReq;
import cn.iocoder.yudao.module.oa.api.dto.sop.TaskExecuteVO;
import cn.iocoder.yudao.module.oa.api.dto.sop.TaskLinkedContentVO;
import cn.iocoder.yudao.module.oa.api.dto.sop.TaskVO;
import cn.iocoder.yudao.module.oa.dal.dataobject.auth.SysUserDO;
import cn.iocoder.yudao.module.oa.dal.dataobject.dict.SysDictDataDO;
import cn.iocoder.yudao.module.oa.dal.dataobject.content.ProductionContentDO;
import cn.iocoder.yudao.module.oa.dal.dataobject.ipgroup.IpGroupDO;
import cn.iocoder.yudao.module.oa.dal.dataobject.plan.ContentPlanCompetitionDO;
import cn.iocoder.yudao.module.oa.dal.dataobject.plan.ContentPlanStepDO;
import cn.iocoder.yudao.module.oa.dal.dataobject.sop.SopNodeDO;
import cn.iocoder.yudao.module.oa.dal.dataobject.sop.SopReviewDO;
import cn.iocoder.yudao.module.oa.dal.dataobject.sop.TaskDO;
import cn.iocoder.yudao.module.oa.dal.mysql.auth.SysUserMapper;
import cn.iocoder.yudao.module.oa.dal.mysql.dict.SysDictDataMapper;
import cn.iocoder.yudao.module.oa.dal.mysql.content.ProductionContentMapper;
import cn.iocoder.yudao.module.oa.dal.mysql.ipgroup.IpGroupMapper;
import cn.iocoder.yudao.module.oa.dal.mysql.plan.ContentPlanCompetitionMapper;
import cn.iocoder.yudao.module.oa.dal.mysql.plan.ContentPlanStepMapper;
import cn.iocoder.yudao.module.oa.dal.mysql.sop.SopNodeMapper;
import cn.iocoder.yudao.module.oa.dal.mysql.sop.SopReviewMapper;
import cn.iocoder.yudao.module.oa.dal.mysql.sop.TaskMapper;
import cn.iocoder.yudao.module.oa.service.file.LocalFileStorageService;
import cn.iocoder.yudao.module.oa.framework.audit.AuditLog;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Collections;
import java.util.List;
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
    private final ProductionContentMapper productionContentMapper;
    private final ContentPlanStepMapper contentPlanStepMapper;
    private final ContentPlanCompetitionMapper contentPlanCompetitionMapper;
    private final IpGroupMapper ipGroupMapper;
    private final LocalFileStorageService localFileStorageService;
    private final SysDictDataMapper sysDictDataMapper;

    private static final String NODE_TYPE_CONTENT_GENERATION = "CONTENT_GENERATION";

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
    public List<TaskVO> listTasksForPlan(Long planId) {
        Long tenantId = requireTenantId();
        List<TaskDO> tasks = taskMapper.selectList(new LambdaQueryWrapper<TaskDO>()
                .eq(TaskDO::getTenantId, tenantId)
                .eq(TaskDO::getPlanId, planId)
                .orderByAsc(TaskDO::getId));
        return tasks.stream().map(this::toPlanDetailVO).collect(Collectors.toList());
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

    @Override
    @Transactional
    @AuditLog(module = "M2-task", action = "execute-get")
    public TaskExecuteVO getExecuteContext(Long id) {
        TaskDO task = requireTask(id);
        requireAssignee(task);
        ensureExecutableStatus(task);
        if ("PENDING".equals(task.getStatus())) {
            task.setStatus("IN_PROGRESS");
            task.setStartTime(LocalDateTime.now());
            task.setUpdater(TenantContextHolder.getUsername());
            task.setUpdateTime(LocalDateTime.now());
            taskMapper.updateById(task);
        }
        return buildExecuteVO(task);
    }

    @Override
    @Transactional
    @AuditLog(module = "M2-task", action = "execute-save")
    public void saveExecute(Long id, TaskExecuteSaveReq req) {
        TaskDO task = requireTask(id);
        requireAssignee(task);
        ensureExecutableStatus(task);
        if (req == null) {
            return;
        }
        boolean changed = false;
        if (req.getDeliverables() != null) {
            task.setDeliverables(req.getDeliverables());
            changed = true;
        }
        if (req.getDeliverableAttachments() != null) {
            task.setDeliverableAttachmentsJson(SopJsonHelper.toAttachmentJson(req.getDeliverableAttachments()));
            changed = true;
        }
        if (changed) {
            task.setUpdater(TenantContextHolder.getUsername());
            task.setUpdateTime(LocalDateTime.now());
            taskMapper.updateById(task);
        }
    }

    @Override
    @Transactional
    @AuditLog(module = "M2-task", action = "execute-upload")
    public TaskAttachmentVO uploadExecuteAttachment(Long id, MultipartFile file) {
        TaskDO task = requireTask(id);
        requireAssignee(task);
        ensureExecutableStatus(task);
        return localFileStorageService.storeTaskAttachment(file, task.getTenantId(), task.getId());
    }

    @Override
    @Transactional
    @AuditLog(module = "M2-task", action = "execute-complete")
    public void executeComplete(Long id) {
        TaskDO task = requireTask(id);
        requireAssignee(task);
        if (!"IN_PROGRESS".equals(task.getStatus())) {
            throw new ServiceException(OaErrorCodes.TASK_STATUS_INVALID);
        }
        SopNodeDO node = sopNodeMapper.selectById(task.getNodeId());
        if (node != null && NODE_TYPE_CONTENT_GENERATION.equals(node.getNodeType())) {
            assertLinkedContentCompleted(task.getId(), task.getTenantId());
        }
        task.setCompleteTime(LocalDateTime.now());
        if (task.getNeedReview() != null && task.getNeedReview() == 1) {
            task.setStatus("PENDING_REVIEW");
            task.setUpdater(TenantContextHolder.getUsername());
            task.setUpdateTime(LocalDateTime.now());
            taskMapper.updateById(task);
            createPendingReview(task, node);
        } else {
            task.setStatus("DONE");
            task.setUpdater(TenantContextHolder.getUsername());
            task.setUpdateTime(LocalDateTime.now());
            taskMapper.updateById(task);
        }
    }

    private void assertLinkedContentCompleted(Long taskId, Long tenantId) {
        ProductionContentDO content = productionContentMapper.selectOne(
                new LambdaQueryWrapper<ProductionContentDO>()
                        .eq(ProductionContentDO::getTenantId, tenantId)
                        .eq(ProductionContentDO::getTaskId, taskId)
                        .last("LIMIT 1"));
        if (content == null || !isLinkedContentSubmitted(content.getStatus())) {
            throw new ServiceException(OaErrorCodes.TASK_CONTENT_NOT_COMPLETED);
        }
    }

    /** 任务内容已提交审核（含历史 COMPLETED 与 V74 审核流状态） */
    private boolean isLinkedContentSubmitted(String status) {
        return status != null && !"DRAFT".equals(status) && !"REJECTED".equals(status);
    }

    private void createPendingReview(TaskDO task, SopNodeDO node) {
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

    private TaskExecuteVO buildExecuteVO(TaskDO task) {
        SopNodeDO node = sopNodeMapper.selectById(task.getNodeId());
        TaskExecuteVO vo = new TaskExecuteVO();
        vo.setId(task.getId());
        vo.setPlanName(task.getPlanName());
        vo.setCompetitionId(task.getCompetitionId());
        vo.setCompetitionName(resolveCompetitionName(task));
        vo.setSlaDeadline(task.getSlaDeadline());
        vo.setStatus(task.getStatus());
        vo.setNeedReview(task.getNeedReview());
        vo.setDeliverables(task.getDeliverables());
        vo.setIpGroupId(task.getIpGroupId());
        if (task.getIpGroupId() != null) {
            IpGroupDO ipGroup = ipGroupMapper.selectById(task.getIpGroupId());
            if (ipGroup != null) {
                vo.setIpGroupName(ipGroup.getGroupName());
            }
        }
        if (node != null) {
            vo.setNodeName(node.getNodeName());
            vo.setNodeType(node.getNodeType());
            String instruction = node.getInstructionText();
            vo.setExecutionInstruction(StrUtil.isNotBlank(instruction) ? instruction : node.getNodeName());
            List<TaskAttachmentVO> attachments = SopJsonHelper.fromAttachmentJson(node.getAttachmentUrls());
            vo.setAttachments(attachments == null ? Collections.emptyList() : attachments);
        } else {
            vo.setAttachments(Collections.emptyList());
        }
        List<TaskAttachmentVO> deliverableAttachments = SopJsonHelper.fromAttachmentJson(task.getDeliverableAttachmentsJson());
        vo.setDeliverableAttachments(deliverableAttachments == null ? Collections.emptyList() : deliverableAttachments);
        ProductionContentDO linked = productionContentMapper.selectOne(
                new LambdaQueryWrapper<ProductionContentDO>()
                        .eq(ProductionContentDO::getTenantId, task.getTenantId())
                        .eq(ProductionContentDO::getTaskId, task.getId())
                        .last("LIMIT 1"));
        if (linked != null) {
            TaskLinkedContentVO linkedVO = new TaskLinkedContentVO();
            linkedVO.setId(linked.getId());
            linkedVO.setTitle(linked.getTitle());
            linkedVO.setStatus(linked.getStatus());
            vo.setLinkedContent(linkedVO);
        }
        return vo;
    }

    private String resolveCompetitionName(TaskDO task) {
        if (task.getPlanId() == null || StrUtil.isBlank(task.getCompetitionId())) {
            return null;
        }
        ContentPlanStepDO step = contentPlanStepMapper.selectOne(
                new LambdaQueryWrapper<ContentPlanStepDO>()
                        .eq(ContentPlanStepDO::getTenantId, task.getTenantId())
                        .eq(ContentPlanStepDO::getPlanId, task.getPlanId())
                        .eq(ContentPlanStepDO::getNodeId, task.getNodeId())
                        .eq(ContentPlanStepDO::getCompetitionId, task.getCompetitionId())
                        .last("LIMIT 1"));
        if (step != null && StrUtil.isNotBlank(step.getCompetitionName())) {
            return step.getCompetitionName();
        }
        ContentPlanCompetitionDO competition = contentPlanCompetitionMapper.selectOne(
                new LambdaQueryWrapper<ContentPlanCompetitionDO>()
                        .eq(ContentPlanCompetitionDO::getTenantId, task.getTenantId())
                        .eq(ContentPlanCompetitionDO::getPlanId, task.getPlanId())
                        .eq(ContentPlanCompetitionDO::getCompetitionId, task.getCompetitionId())
                        .last("LIMIT 1"));
        return competition != null ? competition.getCompetitionName() : null;
    }

    private void ensureExecutableStatus(TaskDO task) {
        String status = task.getStatus();
        if (!"PENDING".equals(status) && !"IN_PROGRESS".equals(status) && !"REJECTED".equals(status)) {
            throw new ServiceException(OaErrorCodes.TASK_STATUS_INVALID);
        }
    }

    private TaskVO toPlanDetailVO(TaskDO entity) {
        TaskVO vo = toVO(entity);
        vo.setCompetitionName(resolveCompetitionName(entity));
        if (StrUtil.isNotBlank(vo.getExecutorRole())) {
            vo.setExecutorRoleText(resolvePositionLabel(vo.getExecutorRole()));
        }
        return vo;
    }

    private String resolvePositionLabel(String position) {
        if (StrUtil.isBlank(position)) {
            return null;
        }
        SysDictDataDO dict = sysDictDataMapper.selectOne(new LambdaQueryWrapper<SysDictDataDO>()
                .eq(SysDictDataDO::getDictType, "dict_position")
                .eq(SysDictDataDO::getDictValue, position)
                .eq(SysDictDataDO::getStatus, "ENABLED")
                .last("LIMIT 1"));
        return dict != null && StrUtil.isNotBlank(dict.getLabel()) ? dict.getLabel() : position;
    }

    private TaskVO toVO(TaskDO entity) {
        TaskVO vo = new TaskVO();
        vo.setId(entity.getId());
        vo.setTemplateId(entity.getTemplateId());
        vo.setNodeId(entity.getNodeId());
        vo.setCompetitionId(entity.getCompetitionId());
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
        vo.setScheduledStart(entity.getScheduledStart());
        vo.setScheduledEnd(entity.getScheduledEnd());
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
