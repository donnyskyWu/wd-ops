package cn.iocoder.yudao.module.oa.service.sop;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.exception.OaErrorCodes;
import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.oa.api.dto.sop.DagValidateReq;
import cn.iocoder.yudao.module.oa.api.dto.sop.DagValidateResp;
import cn.iocoder.yudao.module.oa.api.dto.sop.SopNodeCreateReq;
import cn.iocoder.yudao.module.oa.api.dto.sop.SopNodeUpdateReq;
import cn.iocoder.yudao.module.oa.api.dto.sop.SopNodeVO;
import cn.iocoder.yudao.module.oa.dal.dataobject.sop.SopNodeDO;
import cn.iocoder.yudao.module.oa.dal.mysql.sop.SopNodeMapper;
import cn.iocoder.yudao.module.oa.framework.audit.AuditLog;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SopNodeServiceImpl implements SopNodeService {

    private final SopNodeMapper sopNodeMapper;
    private final SopTemplateServiceImpl sopTemplateService;

    @Override
    public List<SopNodeVO> listByTemplateId(Long templateId) {
        sopTemplateService.requireTemplate(templateId);
        return sopNodeMapper.selectList(new LambdaQueryWrapper<SopNodeDO>()
                        .eq(SopNodeDO::getTemplateId, templateId)
                        .orderByAsc(SopNodeDO::getNodeOrder)
                        .orderByAsc(SopNodeDO::getId))
                .stream()
                .map(this::toVO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    @AuditLog(module = "M2-sop", action = "create-node")
    public Long create(SopNodeCreateReq req) {
        sopTemplateService.requireTemplate(req.getTemplateId());
        validateNodeRoles(req.getExecutorRole(), req.getNeedReview(), req.getReviewerRole());
        validatePredecessors(req.getTemplateId(), req.getPredecessors(), null);

        SopNodeDO entity = new SopNodeDO();
        entity.setTemplateId(req.getTemplateId());
        entity.setNodeName(req.getNodeName().trim());
        entity.setNodeOrder(req.getNodeOrder());
        entity.setExecutorRole(req.getExecutorRole());
        entity.setNeedReview(req.getNeedReview() == null ? 0 : req.getNeedReview());
        entity.setReviewerRole(req.getReviewerRole());
        entity.setPredecessorsJson(SopJsonHelper.toJson(req.getPredecessors()));
        entity.setParallelGroup(req.getParallelGroup());
        entity.setSlaHours(req.getSlaHours() == null ? 24 : req.getSlaHours());
        entity.setCreator(TenantContextHolder.getUsername());
        entity.setUpdater(TenantContextHolder.getUsername());
        entity.setCreateTime(LocalDateTime.now());
        entity.setUpdateTime(LocalDateTime.now());
        sopNodeMapper.insert(entity);
        return entity.getId();
    }

    @Override
    @Transactional
    @AuditLog(module = "M2-sop", action = "update-node")
    public void update(SopNodeUpdateReq req) {
        SopNodeDO existing = requireNode(req.getId());
        if (StrUtil.isNotBlank(req.getNodeName())) {
            existing.setNodeName(req.getNodeName().trim());
        }
        if (req.getNodeOrder() != null) {
            existing.setNodeOrder(req.getNodeOrder());
        }
        String executorRole = req.getExecutorRole() != null ? req.getExecutorRole() : existing.getExecutorRole();
        Integer needReview = req.getNeedReview() != null ? req.getNeedReview() : existing.getNeedReview();
        String reviewerRole = req.getReviewerRole() != null ? req.getReviewerRole() : existing.getReviewerRole();
        validateNodeRoles(executorRole, needReview, reviewerRole);
        if (req.getExecutorRole() != null) {
            existing.setExecutorRole(req.getExecutorRole());
        }
        if (req.getNeedReview() != null) {
            existing.setNeedReview(req.getNeedReview());
        }
        if (req.getReviewerRole() != null) {
            existing.setReviewerRole(req.getReviewerRole());
        }
        if (req.getPredecessors() != null) {
            validatePredecessors(existing.getTemplateId(), req.getPredecessors(), existing.getId());
            existing.setPredecessorsJson(SopJsonHelper.toJson(req.getPredecessors()));
        }
        if (req.getParallelGroup() != null) {
            existing.setParallelGroup(req.getParallelGroup());
        }
        if (req.getSlaHours() != null) {
            existing.setSlaHours(req.getSlaHours());
        }
        existing.setUpdater(TenantContextHolder.getUsername());
        existing.setUpdateTime(LocalDateTime.now());
        sopNodeMapper.updateById(existing);
    }

    @Override
    public DagValidateResp validateDag(DagValidateReq req) {
        sopTemplateService.requireTemplate(req.getTemplateId());
        Map<Long, List<Long>> graph = new HashMap<>();
        for (DagValidateReq.DagNodeItem item : req.getNodes()) {
            graph.put(item.getId(), item.getPredecessors() == null ? List.of() : item.getPredecessors());
        }
        DagValidator.DagValidateResult result = DagValidator.validate(graph);
        if (result.isValid()) {
            return DagValidateResp.ok();
        }
        throw new ServiceException(OaErrorCodes.SOP_DAG_CYCLE);
    }

    private void validateNodeRoles(String executorRole, Integer needReview, String reviewerRole) {
        if (StrUtil.isBlank(executorRole)) {
            throw new ServiceException(OaErrorCodes.SOP_EXECUTOR_ROLE_MISSING);
        }
        if (needReview != null && needReview == 1 && StrUtil.isBlank(reviewerRole)) {
            throw new ServiceException(OaErrorCodes.SOP_REVIEWER_ROLE_MISSING);
        }
    }

    private void validatePredecessors(Long templateId, List<Long> predecessors, Long excludeNodeId) {
        if (predecessors == null || predecessors.isEmpty()) {
            return;
        }
        for (Long predId : predecessors) {
            SopNodeDO pred = sopNodeMapper.selectById(predId);
            if (pred == null || !templateId.equals(pred.getTemplateId())) {
                throw new ServiceException(OaErrorCodes.ENTITY_NOT_EXISTS);
            }
        }
        if (excludeNodeId != null) {
            Map<Long, List<Long>> graph = new HashMap<>();
            List<SopNodeDO> nodes = sopNodeMapper.selectList(new LambdaQueryWrapper<SopNodeDO>()
                    .eq(SopNodeDO::getTemplateId, templateId));
            for (SopNodeDO node : nodes) {
                List<Long> preds = node.getId().equals(excludeNodeId)
                        ? predecessors
                        : SopJsonHelper.fromJson(node.getPredecessorsJson());
                graph.put(node.getId(), preds);
            }
            if (!graph.containsKey(excludeNodeId)) {
                graph.put(excludeNodeId, predecessors);
            }
            DagValidator.DagValidateResult result = DagValidator.validate(graph);
            if (!result.isValid()) {
                throw new ServiceException(OaErrorCodes.SOP_DAG_CYCLE);
            }
        }
    }

    private SopNodeVO toVO(SopNodeDO entity) {
        SopNodeVO vo = new SopNodeVO();
        vo.setId(entity.getId());
        vo.setTemplateId(entity.getTemplateId());
        vo.setNodeName(entity.getNodeName());
        vo.setNodeOrder(entity.getNodeOrder());
        vo.setExecutorRole(entity.getExecutorRole());
        vo.setNeedReview(entity.getNeedReview());
        vo.setReviewerRole(entity.getReviewerRole());
        vo.setPredecessors(SopJsonHelper.fromJson(entity.getPredecessorsJson()));
        vo.setParallelGroup(entity.getParallelGroup());
        vo.setSlaHours(entity.getSlaHours());
        return vo;
    }

    private SopNodeDO requireNode(Long id) {
        SopNodeDO entity = sopNodeMapper.selectById(id);
        if (entity == null) {
            throw new ServiceException(OaErrorCodes.ENTITY_NOT_EXISTS);
        }
        sopTemplateService.requireTemplate(entity.getTemplateId());
        return entity;
    }
}
