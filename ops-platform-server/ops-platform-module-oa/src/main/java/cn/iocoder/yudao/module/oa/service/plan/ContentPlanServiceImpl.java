package cn.iocoder.yudao.module.oa.service.plan;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import cn.iocoder.yudao.framework.common.exception.OaErrorCodes;
import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.oa.api.dto.plan.ContentPlanCompetitionReq;
import cn.iocoder.yudao.module.oa.api.dto.plan.ContentPlanCompetitionVO;
import cn.iocoder.yudao.module.oa.api.dto.plan.ContentPlanCreateReq;
import cn.iocoder.yudao.module.oa.api.dto.plan.ContentPlanRespVO;
import cn.iocoder.yudao.module.oa.api.dto.plan.ContentPlanStepReq;
import cn.iocoder.yudao.module.oa.api.dto.plan.ContentPlanStepVO;
import cn.iocoder.yudao.module.oa.api.dto.plan.ContentPlanTerminateReq;
import cn.iocoder.yudao.module.oa.api.dto.plan.ContentPlanUpdateReq;
import cn.iocoder.yudao.module.oa.api.dto.sop.TaskVO;
import cn.iocoder.yudao.module.oa.dal.dataobject.auth.SysUserDO;
import cn.iocoder.yudao.module.oa.dal.dataobject.ipgroup.IpGroupDO;
import cn.iocoder.yudao.module.oa.dal.dataobject.ipgroup.IpGroupMemberDO;
import cn.iocoder.yudao.module.oa.dal.dataobject.plan.ContentPlanCompetitionDO;
import cn.iocoder.yudao.module.oa.dal.dataobject.plan.ContentPlanDO;
import cn.iocoder.yudao.module.oa.dal.dataobject.plan.ContentPlanStepDO;
import cn.iocoder.yudao.module.oa.dal.dataobject.sop.SopNodeDO;
import cn.iocoder.yudao.module.oa.dal.dataobject.sop.SopTemplateDO;
import cn.iocoder.yudao.module.oa.dal.dataobject.sop.TaskDO;
import cn.iocoder.yudao.module.oa.dal.mysql.auth.SysUserMapper;
import cn.iocoder.yudao.module.oa.dal.mysql.ipgroup.IpGroupMapper;
import cn.iocoder.yudao.module.oa.dal.mysql.ipgroup.IpGroupMemberMapper;
import cn.iocoder.yudao.module.oa.dal.mysql.plan.ContentPlanCompetitionMapper;
import cn.iocoder.yudao.module.oa.dal.mysql.plan.ContentPlanMapper;
import cn.iocoder.yudao.module.oa.dal.mysql.plan.ContentPlanStepMapper;
import cn.iocoder.yudao.module.oa.dal.mysql.sop.SopNodeMapper;
import cn.iocoder.yudao.module.oa.dal.mysql.sop.SopTemplateMapper;
import cn.iocoder.yudao.module.oa.dal.mysql.sop.TaskMapper;
import cn.iocoder.yudao.module.oa.framework.audit.AuditLog;
import cn.iocoder.yudao.module.oa.service.sop.TaskService;
import cn.iocoder.yudao.module.oa.service.notification.NotificationService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ContentPlanServiceImpl implements ContentPlanService {

    private static final DateTimeFormatter DT_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final String STATUS_DRAFT = "DRAFT";
    private static final String STATUS_IN_PROGRESS = "IN_PROGRESS";
    private static final String STATUS_TERMINATE_PENDING = "TERMINATE_PENDING";
    private static final String STATUS_TERMINATED = "TERMINATED";
    private static final String TASK_STATUS_PLAN_DRAFT = "PLAN_DRAFT";
    private static final String TASK_STATUS_PENDING = "PENDING";
    private static final String TASK_STATUS_TERMINATED = "TERMINATED";

    private final ContentPlanMapper contentPlanMapper;
    private final ContentPlanCompetitionMapper contentPlanCompetitionMapper;
    private final ContentPlanStepMapper contentPlanStepMapper;
    private final TaskMapper taskMapper;
    private final SopTemplateMapper sopTemplateMapper;
    private final SopNodeMapper sopNodeMapper;
    private final IpGroupMapper ipGroupMapper;
    private final IpGroupMemberMapper ipGroupMemberMapper;
    private final SysUserMapper sysUserMapper;
    private final TaskService taskService;
    private final NotificationService notificationService;

    @Override
    public PageResult<ContentPlanRespVO> list(String planName, String status, Integer pageNo, Integer pageSize) {
        Long tenantId = requireTenantId();
        LambdaQueryWrapper<ContentPlanDO> wrapper = new LambdaQueryWrapper<ContentPlanDO>()
                .eq(ContentPlanDO::getTenantId, tenantId)
                .like(StrUtil.isNotBlank(planName), ContentPlanDO::getPlanName, planName)
                .eq(StrUtil.isNotBlank(status), ContentPlanDO::getStatus, status)
                .orderByDesc(ContentPlanDO::getId);
        Page<ContentPlanDO> page = contentPlanMapper.selectPage(
                new Page<>(pageNo == null ? 1 : pageNo, pageSize == null ? 20 : pageSize), wrapper);
        List<ContentPlanRespVO> list = page.getRecords().stream()
                .map(this::toSummary)
                .collect(Collectors.toList());
        return new PageResult<>(list, page.getTotal());
    }

    @Override
    public ContentPlanRespVO get(Long id) {
        ContentPlanDO plan = requirePlan(id);
        return toDetail(plan);
    }

    @Override
    @Transactional
    @AuditLog(module = "M2-plan", action = "create")
    public Long create(ContentPlanCreateReq req) {
        Long tenantId = requireTenantId();
        assertTemplateInTenant(req.getTemplateId(), tenantId);
        assertIpGroupInTenant(req.getIpGroupId(), tenantId);
        if (req.getEndDate().isBefore(req.getStartDate())) {
            throw new ServiceException(OaErrorCodes.DICT_VALUE_INVALID.getCode(), "结束日期不能早于开始日期");
        }

        Map<Long, SopNodeDO> nodeMap = loadTemplateNodes(req.getTemplateId());
        Map<String, String> competitionNameMap = buildCompetitionNameMap(req.getCompetitions());
        validateSteps(req.getSteps(), nodeMap);
        validateStepCompetitions(req.getSteps(), competitionNameMap.keySet());
        validateAssignees(req.getSteps(), tenantId);
        validateAssigneesInIpGroup(req.getSteps(), req.getIpGroupId(), tenantId);

        ContentPlanDO plan = new ContentPlanDO();
        plan.setTenantId(tenantId);
        plan.setPlanName(req.getPlanName());
        plan.setTemplateId(req.getTemplateId());
        plan.setIpGroupId(req.getIpGroupId());
        plan.setStartDate(req.getStartDate());
        plan.setEndDate(req.getEndDate());
        plan.setDescription(req.getDescription());
        plan.setStatus(STATUS_DRAFT);
        plan.setCreator(TenantContextHolder.getUsername());
        plan.setUpdater(TenantContextHolder.getUsername());
        plan.setCreateTime(LocalDateTime.now());
        plan.setUpdateTime(LocalDateTime.now());
        contentPlanMapper.insert(plan);

        saveCompetitions(plan.getId(), tenantId, req.getCompetitions());
        saveStepsAndTasks(plan, req.getSteps(), nodeMap, competitionNameMap, tenantId);
        return plan.getId();
    }

    @Override
    @Transactional
    @AuditLog(module = "M2-plan", action = "update")
    public void update(ContentPlanUpdateReq req) {
        ContentPlanDO plan = requirePlan(req.getId());
        if (!STATUS_DRAFT.equals(plan.getStatus())) {
            throw new ServiceException(OaErrorCodes.TASK_STATUS_INVALID.getCode(), "仅草稿计划可编辑");
        }
        Long tenantId = requireTenantId();
        if (req.getEndDate().isBefore(req.getStartDate())) {
            throw new ServiceException(OaErrorCodes.DICT_VALUE_INVALID.getCode(), "结束日期不能早于开始日期");
        }

        Map<Long, SopNodeDO> nodeMap = loadTemplateNodes(plan.getTemplateId());
        Map<String, String> competitionNameMap = buildCompetitionNameMap(req.getCompetitions());
        validateSteps(req.getSteps(), nodeMap);
        validateStepCompetitions(req.getSteps(), competitionNameMap.keySet());
        validateAssignees(req.getSteps(), tenantId);
        validateAssigneesInIpGroup(req.getSteps(), plan.getIpGroupId(), tenantId);

        plan.setPlanName(req.getPlanName());
        plan.setStartDate(req.getStartDate());
        plan.setEndDate(req.getEndDate());
        plan.setDescription(req.getDescription());
        plan.setUpdater(TenantContextHolder.getUsername());
        plan.setUpdateTime(LocalDateTime.now());
        contentPlanMapper.updateById(plan);

        replacePlanChildren(plan, req.getCompetitions(), req.getSteps(), nodeMap, competitionNameMap, tenantId);
    }

    @Override
    @Transactional
    @AuditLog(module = "M2-plan", action = "start")
    public void start(Long id) {
        ContentPlanDO plan = requirePlan(id);
        if (!STATUS_DRAFT.equals(plan.getStatus())) {
            throw new ServiceException(OaErrorCodes.TASK_STATUS_INVALID.getCode(), "仅草稿计划可启动");
        }
        plan.setStatus(STATUS_IN_PROGRESS);
        plan.setUpdater(TenantContextHolder.getUsername());
        plan.setUpdateTime(LocalDateTime.now());
        contentPlanMapper.updateById(plan);

        taskMapper.update(null, new LambdaUpdateWrapper<TaskDO>()
                .eq(TaskDO::getTenantId, plan.getTenantId())
                .eq(TaskDO::getPlanId, plan.getId())
                .set(TaskDO::getStatus, TASK_STATUS_PENDING)
                .set(TaskDO::getVisibleInList, 1)
                .set(TaskDO::getUpdater, TenantContextHolder.getUsername())
                .set(TaskDO::getUpdateTime, LocalDateTime.now()));
        try {
            notificationService.notifyPlanStarted(plan.getTenantId(), plan.getId(), plan.getPlanName());
        } catch (Exception e) {
            // 通知失败不影响计划启动
        }
    }

    @Override
    @Transactional
    @AuditLog(module = "M2-plan", action = "terminate-submit")
    public void submitTerminate(Long id, ContentPlanTerminateReq req) {
        ContentPlanDO plan = requirePlan(id);
        if (!STATUS_IN_PROGRESS.equals(plan.getStatus())) {
            throw new ServiceException(OaErrorCodes.TASK_STATUS_INVALID.getCode(), "仅进行中的计划可提交终止");
        }
        plan.setStatus(STATUS_TERMINATE_PENDING);
        if (req != null && StrUtil.isNotBlank(req.getReason())) {
            plan.setDescription(appendReason(plan.getDescription(), req.getReason()));
        }
        plan.setUpdater(TenantContextHolder.getUsername());
        plan.setUpdateTime(LocalDateTime.now());
        contentPlanMapper.updateById(plan);
    }

    @Override
    @Transactional
    @AuditLog(module = "M2-plan", action = "terminate-approve")
    public void approveTerminate(Long id) {
        requireOpsLeader();
        ContentPlanDO plan = requirePlan(id);
        if (!STATUS_TERMINATE_PENDING.equals(plan.getStatus())) {
            throw new ServiceException(OaErrorCodes.TASK_STATUS_INVALID.getCode(), "计划不在终止审批中");
        }
        plan.setStatus(STATUS_TERMINATED);
        plan.setUpdater(TenantContextHolder.getUsername());
        plan.setUpdateTime(LocalDateTime.now());
        contentPlanMapper.updateById(plan);

        taskMapper.update(null, new LambdaUpdateWrapper<TaskDO>()
                .eq(TaskDO::getTenantId, plan.getTenantId())
                .eq(TaskDO::getPlanId, plan.getId())
                .set(TaskDO::getStatus, TASK_STATUS_TERMINATED)
                .set(TaskDO::getUpdater, TenantContextHolder.getUsername())
                .set(TaskDO::getUpdateTime, LocalDateTime.now()));
    }

    @Override
    @Transactional
    @AuditLog(module = "M2-plan", action = "terminate-reject")
    public void rejectTerminate(Long id) {
        requireOpsLeader();
        ContentPlanDO plan = requirePlan(id);
        if (!STATUS_TERMINATE_PENDING.equals(plan.getStatus())) {
            throw new ServiceException(OaErrorCodes.TASK_STATUS_INVALID.getCode(), "计划不在终止审批中");
        }
        plan.setStatus(STATUS_IN_PROGRESS);
        plan.setUpdater(TenantContextHolder.getUsername());
        plan.setUpdateTime(LocalDateTime.now());
        contentPlanMapper.updateById(plan);
    }

    @Override
    @Transactional
    @AuditLog(module = "M2-plan", action = "delete")
    public void delete(Long id) {
        ContentPlanDO plan = requirePlan(id);
        if (!STATUS_DRAFT.equals(plan.getStatus())) {
            throw new ServiceException(OaErrorCodes.TASK_STATUS_INVALID.getCode(), "仅草稿计划可删除");
        }
        taskMapper.delete(new LambdaQueryWrapper<TaskDO>()
                .eq(TaskDO::getTenantId, plan.getTenantId())
                .eq(TaskDO::getPlanId, plan.getId()));
        contentPlanStepMapper.delete(new LambdaQueryWrapper<ContentPlanStepDO>()
                .eq(ContentPlanStepDO::getTenantId, plan.getTenantId())
                .eq(ContentPlanStepDO::getPlanId, plan.getId()));
        contentPlanCompetitionMapper.delete(new LambdaQueryWrapper<ContentPlanCompetitionDO>()
                .eq(ContentPlanCompetitionDO::getTenantId, plan.getTenantId())
                .eq(ContentPlanCompetitionDO::getPlanId, plan.getId()));
        contentPlanMapper.deleteById(plan.getId());
    }

    private void replacePlanChildren(ContentPlanDO plan, List<ContentPlanCompetitionReq> competitions,
                                     List<ContentPlanStepReq> steps, Map<Long, SopNodeDO> nodeMap,
                                     Map<String, String> competitionNameMap, Long tenantId) {
        taskMapper.delete(new LambdaQueryWrapper<TaskDO>()
                .eq(TaskDO::getTenantId, tenantId)
                .eq(TaskDO::getPlanId, plan.getId()));
        contentPlanStepMapper.delete(new LambdaQueryWrapper<ContentPlanStepDO>()
                .eq(ContentPlanStepDO::getTenantId, tenantId)
                .eq(ContentPlanStepDO::getPlanId, plan.getId()));
        contentPlanCompetitionMapper.delete(new LambdaQueryWrapper<ContentPlanCompetitionDO>()
                .eq(ContentPlanCompetitionDO::getTenantId, tenantId)
                .eq(ContentPlanCompetitionDO::getPlanId, plan.getId()));
        saveCompetitions(plan.getId(), tenantId, competitions);
        saveStepsAndTasks(plan, steps, nodeMap, competitionNameMap, tenantId);
    }

    private void saveCompetitions(Long planId, Long tenantId, List<ContentPlanCompetitionReq> competitions) {
        for (ContentPlanCompetitionReq item : competitions) {
            ContentPlanCompetitionDO entity = new ContentPlanCompetitionDO();
            entity.setTenantId(tenantId);
            entity.setPlanId(planId);
            entity.setCompetitionId(item.getCompetitionId());
            entity.setCompetitionName(item.getCompetitionName());
            entity.setCreator(TenantContextHolder.getUsername());
            entity.setUpdater(TenantContextHolder.getUsername());
            entity.setCreateTime(LocalDateTime.now());
            entity.setUpdateTime(LocalDateTime.now());
            contentPlanCompetitionMapper.insert(entity);
        }
    }

    private Map<String, String> buildCompetitionNameMap(List<ContentPlanCompetitionReq> competitions) {
        Map<String, String> map = new HashMap<>();
        for (ContentPlanCompetitionReq item : competitions) {
            map.put(item.getCompetitionId(), item.getCompetitionName());
        }
        return map;
    }

    private void saveStepsAndTasks(ContentPlanDO plan, List<ContentPlanStepReq> steps,
                                   Map<Long, SopNodeDO> nodeMap, Map<String, String> competitionNameMap,
                                   Long tenantId) {
        LocalDateTime defaultStart = plan.getStartDate().atStartOfDay();
        LocalDateTime defaultEnd = plan.getEndDate().atTime(23, 59, 59);
        for (ContentPlanStepReq stepReq : steps) {
            SopNodeDO node = nodeMap.get(stepReq.getNodeId());
            List<String> competitionIds = resolveCompetitionIds(stepReq);
            LocalDateTime stepStart = stepReq.getScheduledStart() != null ? stepReq.getScheduledStart() : defaultStart;
            LocalDateTime stepEnd = stepReq.getScheduledEnd() != null ? stepReq.getScheduledEnd() : defaultEnd;

            ContentPlanStepDO step = new ContentPlanStepDO();
            step.setTenantId(tenantId);
            step.setPlanId(plan.getId());
            step.setNodeId(stepReq.getNodeId());
            step.setCompetitionId(competitionIds.get(0));
            step.setCompetitionName(competitionNameMap.get(competitionIds.get(0)));
            step.setCompetitionIdsJson(JSONUtil.toJsonStr(competitionIds));
            step.setAssigneeIdsJson(JSONUtil.toJsonStr(stepReq.getAssigneeIds()));
            step.setScheduledStart(stepStart);
            step.setScheduledEnd(stepEnd);
            step.setCreator(TenantContextHolder.getUsername());
            step.setUpdater(TenantContextHolder.getUsername());
            step.setCreateTime(LocalDateTime.now());
            step.setUpdateTime(LocalDateTime.now());
            contentPlanStepMapper.insert(step);

            for (Long assigneeId : stepReq.getAssigneeIds()) {
                for (String competitionId : competitionIds) {
                    TaskDO task = new TaskDO();
                    task.setTenantId(tenantId);
                    task.setPlanId(plan.getId());
                    task.setCompetitionId(competitionId);
                    task.setTemplateId(plan.getTemplateId());
                    task.setNodeId(node.getId());
                    task.setPlanName(plan.getPlanName());
                    task.setAssigneeId(assigneeId);
                    task.setIpGroupId(plan.getIpGroupId());
                    task.setStatus(TASK_STATUS_PLAN_DRAFT);
                    task.setVisibleInList(0);
                    task.setScheduledStart(stepStart);
                    task.setScheduledEnd(stepEnd);
                    task.setNeedReview(node.getNeedReview());
                    task.setCreator(TenantContextHolder.getUsername());
                    task.setUpdater(TenantContextHolder.getUsername());
                    task.setCreateTime(LocalDateTime.now());
                    task.setUpdateTime(LocalDateTime.now());
                    taskMapper.insert(task);
                }
            }
        }
    }

    private ContentPlanRespVO toSummary(ContentPlanDO plan) {
        ContentPlanRespVO vo = new ContentPlanRespVO();
        vo.setId(plan.getId());
        vo.setPlanName(plan.getPlanName());
        vo.setTemplateId(plan.getTemplateId());
        vo.setIpGroupId(plan.getIpGroupId());
        vo.setStartDate(plan.getStartDate().format(DATE_FMT));
        vo.setEndDate(plan.getEndDate().format(DATE_FMT));
        vo.setDescription(plan.getDescription());
        vo.setStatus(plan.getStatus());
        vo.setProgress(calcProgress(plan));
        if (plan.getCreateTime() != null) {
            vo.setCreateTime(plan.getCreateTime().format(DT_FMT));
        }
        SopTemplateDO template = sopTemplateMapper.selectById(plan.getTemplateId());
        if (template != null) {
            vo.setTemplateName(template.getTemplateName());
        }
        IpGroupDO ipGroup = ipGroupMapper.selectById(plan.getIpGroupId());
        if (ipGroup != null) {
            vo.setIpGroupName(ipGroup.getGroupName());
        }
        return vo;
    }

    private ContentPlanRespVO toDetail(ContentPlanDO plan) {
        ContentPlanRespVO vo = toSummary(plan);
        vo.setCompetitions(contentPlanCompetitionMapper.selectList(new LambdaQueryWrapper<ContentPlanCompetitionDO>()
                        .eq(ContentPlanCompetitionDO::getTenantId, plan.getTenantId())
                        .eq(ContentPlanCompetitionDO::getPlanId, plan.getId()))
                .stream()
                .map(item -> {
                    ContentPlanCompetitionVO c = new ContentPlanCompetitionVO();
                    c.setCompetitionId(item.getCompetitionId());
                    c.setCompetitionName(item.getCompetitionName());
                    return c;
                })
                .collect(Collectors.toList()));

        Map<String, String> competitionNameMap = new HashMap<>();
        for (ContentPlanCompetitionVO item : vo.getCompetitions()) {
            competitionNameMap.put(item.getCompetitionId(), item.getCompetitionName());
        }

        Map<Long, SopNodeDO> nodeMap = loadTemplateNodes(plan.getTemplateId());
        vo.setSteps(contentPlanStepMapper.selectList(new LambdaQueryWrapper<ContentPlanStepDO>()
                        .eq(ContentPlanStepDO::getTenantId, plan.getTenantId())
                        .eq(ContentPlanStepDO::getPlanId, plan.getId())
                        .orderByAsc(ContentPlanStepDO::getId))
                .stream()
                .map(step -> {
                    ContentPlanStepVO stepVO = new ContentPlanStepVO();
                    stepVO.setNodeId(step.getNodeId());
                    List<String> competitionIds = parseCompetitionIds(step);
                    stepVO.setCompetitionIds(competitionIds);
                    if (!competitionIds.isEmpty()) {
                        stepVO.setCompetitionId(competitionIds.get(0));
                        stepVO.setCompetitionName(competitionNameMap.get(competitionIds.get(0)));
                        List<String> names = new ArrayList<>();
                        for (String id : competitionIds) {
                            String name = competitionNameMap.get(id);
                            if (StrUtil.isNotBlank(name)) {
                                names.add(name);
                            }
                        }
                        stepVO.setCompetitionNames(names);
                    }
                    SopNodeDO node = nodeMap.get(step.getNodeId());
                    if (node != null) {
                        stepVO.setNodeName(node.getNodeName());
                        stepVO.setNodeOrder(node.getNodeOrder());
                        stepVO.setExecutorRole(node.getExecutorRole());
                    }
                    stepVO.setAssigneeIds(JSONUtil.toList(step.getAssigneeIdsJson(), Long.class));
                    if (step.getScheduledStart() != null) {
                        stepVO.setScheduledStart(step.getScheduledStart().format(DT_FMT));
                    }
                    if (step.getScheduledEnd() != null) {
                        stepVO.setScheduledEnd(step.getScheduledEnd().format(DT_FMT));
                    }
                    return stepVO;
                })
                .sorted((a, b) -> Integer.compare(
                        a.getNodeOrder() == null ? 0 : a.getNodeOrder(),
                        b.getNodeOrder() == null ? 0 : b.getNodeOrder()))
                .collect(Collectors.toList()));
        vo.setTasks(taskService.listTasksForPlan(plan.getId()));
        return vo;
    }

    private int calcProgress(ContentPlanDO plan) {
        if (STATUS_DRAFT.equals(plan.getStatus())) {
            return 0;
        }
        if (STATUS_TERMINATED.equals(plan.getStatus())) {
            return 100;
        }
        Long total = taskMapper.selectCount(new LambdaQueryWrapper<TaskDO>()
                .eq(TaskDO::getTenantId, plan.getTenantId())
                .eq(TaskDO::getPlanId, plan.getId()));
        if (total == null || total == 0) {
            return 0;
        }
        Long done = taskMapper.selectCount(new LambdaQueryWrapper<TaskDO>()
                .eq(TaskDO::getTenantId, plan.getTenantId())
                .eq(TaskDO::getPlanId, plan.getId())
                .in(TaskDO::getStatus, List.of("DONE", "APPROVED", TASK_STATUS_TERMINATED)));
        return (int) Math.min(100, done * 100 / total);
    }

    private Map<Long, SopNodeDO> loadTemplateNodes(Long templateId) {
        List<SopNodeDO> nodes = sopNodeMapper.selectList(new LambdaQueryWrapper<SopNodeDO>()
                .eq(SopNodeDO::getTemplateId, templateId)
                .orderByAsc(SopNodeDO::getNodeOrder));
        Map<Long, SopNodeDO> nodeMap = new HashMap<>();
        for (SopNodeDO node : nodes) {
            nodeMap.put(node.getId(), node);
        }
        return nodeMap;
    }

    private void validateSteps(List<ContentPlanStepReq> steps, Map<Long, SopNodeDO> nodeMap) {
        if (nodeMap.isEmpty()) {
            throw new ServiceException(OaErrorCodes.ENTITY_NOT_EXISTS.getCode(), "SOP 模板无节点");
        }
        Set<Long> seen = new HashSet<>();
        for (ContentPlanStepReq step : steps) {
            if (!nodeMap.containsKey(step.getNodeId())) {
                throw new ServiceException(OaErrorCodes.ENTITY_NOT_EXISTS.getCode(), "SOP 节点不存在");
            }
            if (!seen.add(step.getNodeId())) {
                throw new ServiceException(OaErrorCodes.DUPLICATE_ENTITY.getCode(), "SOP 节点重复配置");
            }
        }
        if (seen.size() != nodeMap.size()) {
            throw new ServiceException(OaErrorCodes.DICT_VALUE_INVALID.getCode(), "须为每个 SOP 节点分配执行人");
        }
    }

    private void validateStepCompetitions(List<ContentPlanStepReq> steps, Set<String> planCompetitionIds) {
        for (ContentPlanStepReq step : steps) {
            List<String> competitionIds = resolveCompetitionIds(step);
            if (competitionIds.isEmpty()) {
                throw new ServiceException(OaErrorCodes.DICT_VALUE_INVALID.getCode(), "每步骤须分配赛事");
            }
            for (String competitionId : competitionIds) {
                if (!planCompetitionIds.contains(competitionId)) {
                    throw new ServiceException(OaErrorCodes.ENTITY_NOT_EXISTS.getCode(), "步骤赛事不在计划赛事池内");
                }
            }
        }
    }

    private void validateAssigneesInIpGroup(List<ContentPlanStepReq> steps, Long ipGroupId, Long tenantId) {
        Set<Long> memberUserIds = ipGroupMemberMapper.selectList(new LambdaQueryWrapper<IpGroupMemberDO>()
                        .eq(IpGroupMemberDO::getTenantId, tenantId)
                        .eq(IpGroupMemberDO::getIpGroupId, ipGroupId))
                .stream()
                .map(IpGroupMemberDO::getUserId)
                .collect(Collectors.toSet());
        for (ContentPlanStepReq step : steps) {
            for (Long assigneeId : step.getAssigneeIds()) {
                if (!memberUserIds.contains(assigneeId)) {
                    throw new ServiceException(OaErrorCodes.ENTITY_NOT_EXISTS.getCode(), "执行人须为所选 IP 组成员");
                }
            }
        }
    }

    private List<String> resolveCompetitionIds(ContentPlanStepReq step) {
        if (step.getCompetitionIds() != null && !step.getCompetitionIds().isEmpty()) {
            return step.getCompetitionIds().stream()
                    .filter(StrUtil::isNotBlank)
                    .distinct()
                    .collect(Collectors.toList());
        }
        if (StrUtil.isNotBlank(step.getCompetitionId())) {
            return List.of(step.getCompetitionId());
        }
        return Collections.emptyList();
    }

    private List<String> parseCompetitionIds(ContentPlanStepDO step) {
        if (StrUtil.isNotBlank(step.getCompetitionIdsJson())) {
            List<String> ids = JSONUtil.toList(step.getCompetitionIdsJson(), String.class);
            if (ids != null && !ids.isEmpty()) {
                return ids;
            }
        }
        if (StrUtil.isNotBlank(step.getCompetitionId())) {
            return List.of(step.getCompetitionId());
        }
        return Collections.emptyList();
    }

    private void validateAssignees(List<ContentPlanStepReq> steps, Long tenantId) {
        for (ContentPlanStepReq step : steps) {
            if (step.getAssigneeIds() == null || step.getAssigneeIds().size() != 1) {
                throw new ServiceException(OaErrorCodes.DICT_VALUE_INVALID.getCode(), "每步骤仅可分配一名执行人");
            }
            for (Long assigneeId : step.getAssigneeIds()) {
                SysUserDO user = sysUserMapper.selectById(assigneeId);
                if (user == null || !tenantId.equals(user.getTenantId())) {
                    throw new ServiceException(OaErrorCodes.ENTITY_NOT_EXISTS.getCode(), "执行人不存在");
                }
            }
        }
    }

    private void assertTemplateInTenant(Long templateId, Long tenantId) {
        SopTemplateDO template = sopTemplateMapper.selectById(templateId);
        if (template == null || !tenantId.equals(template.getTenantId())) {
            throw new ServiceException(OaErrorCodes.ENTITY_NOT_EXISTS.getCode(), "SOP 模板不存在");
        }
    }

    private void assertIpGroupInTenant(Long ipGroupId, Long tenantId) {
        IpGroupDO ipGroup = ipGroupMapper.selectById(ipGroupId);
        if (ipGroup == null || !tenantId.equals(ipGroup.getTenantId())) {
            throw new ServiceException(OaErrorCodes.ENTITY_NOT_EXISTS.getCode(), "IP 组不存在");
        }
    }

    private ContentPlanDO requirePlan(Long id) {
        ContentPlanDO plan = contentPlanMapper.selectById(id);
        if (plan == null) {
            throw new ServiceException(OaErrorCodes.ENTITY_NOT_EXISTS);
        }
        if (!requireTenantId().equals(plan.getTenantId())) {
            throw new ServiceException(OaErrorCodes.TENANT_FORBIDDEN);
        }
        return plan;
    }

    private void requireOpsLeader() {
        SysUserDO user = sysUserMapper.selectById(TenantContextHolder.getUserId());
        if (user == null || !"OPS_LEADER".equals(user.getPosition())) {
            throw new ServiceException(OaErrorCodes.FORBIDDEN.getCode(), "仅运营组长可审批计划终止");
        }
    }

    private String appendReason(String description, String reason) {
        String base = StrUtil.blankToDefault(description, "");
        return base + (base.isEmpty() ? "" : "\n") + "[终止申请] " + reason;
    }

    private Long requireTenantId() {
        Long tenantId = TenantContextHolder.getTenantId();
        if (tenantId == null) {
            throw new ServiceException(OaErrorCodes.UNAUTHORIZED);
        }
        return tenantId;
    }
}
