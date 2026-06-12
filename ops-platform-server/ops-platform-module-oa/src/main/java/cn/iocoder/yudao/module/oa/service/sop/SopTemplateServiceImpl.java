package cn.iocoder.yudao.module.oa.service.sop;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.exception.OaErrorCodes;
import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.oa.api.dto.sop.SopTemplateCreateReq;
import cn.iocoder.yudao.module.oa.api.dto.sop.SopTemplateUpdateReq;
import cn.iocoder.yudao.module.oa.api.dto.sop.SopTemplateVO;
import cn.iocoder.yudao.module.oa.dal.dataobject.sop.SopNodeDO;
import cn.iocoder.yudao.module.oa.dal.dataobject.sop.SopTemplateDO;
import cn.iocoder.yudao.module.oa.dal.dataobject.sop.TaskDO;
import cn.iocoder.yudao.module.oa.dal.mysql.sop.SopNodeMapper;
import cn.iocoder.yudao.module.oa.dal.mysql.sop.SopTemplateMapper;
import cn.iocoder.yudao.module.oa.dal.mysql.sop.TaskMapper;
import cn.iocoder.yudao.module.oa.framework.audit.AuditLog;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SopTemplateServiceImpl implements SopTemplateService {

    private final SopTemplateMapper sopTemplateMapper;
    private final SopNodeMapper sopNodeMapper;
    private final TaskMapper taskMapper;

    @Override
    public PageResult<SopTemplateVO> list(String templateName, String contentType, String platformType,
                                          Integer status, Integer pageNum, Integer pageSize) {
        Long tenantId = requireTenantId();
        LambdaQueryWrapper<SopTemplateDO> wrapper = new LambdaQueryWrapper<SopTemplateDO>()
                .eq(SopTemplateDO::getTenantId, tenantId)
                .like(StrUtil.isNotBlank(templateName), SopTemplateDO::getTemplateName, templateName)
                .eq(StrUtil.isNotBlank(contentType), SopTemplateDO::getContentType, contentType)
                .eq(StrUtil.isNotBlank(platformType), SopTemplateDO::getPlatformType, platformType)
                .eq(status != null, SopTemplateDO::getStatus, status)
                .orderByDesc(SopTemplateDO::getId);
        Page<SopTemplateDO> page = sopTemplateMapper.selectPage(
                new Page<>(pageNum == null ? 1 : pageNum, pageSize == null ? 20 : pageSize), wrapper);
        return new PageResult<>(page.getRecords().stream().map(this::toVO).collect(Collectors.toList()), page.getTotal());
    }

    @Override
    @Transactional
    @AuditLog(module = "M2-sop", action = "create-template")
    public Long create(SopTemplateCreateReq req) {
        Long tenantId = requireTenantId();
        SopTemplateDO entity = new SopTemplateDO();
        entity.setTenantId(tenantId);
        entity.setTemplateName(req.getTemplateName().trim());
        entity.setContentType(req.getContentType());
        entity.setPlatformType(req.getPlatformType());
        entity.setDescription(req.getDescription());
        entity.setStatus(req.getStatus() == null ? 1 : req.getStatus());
        entity.setCreator(TenantContextHolder.getUsername());
        entity.setUpdater(TenantContextHolder.getUsername());
        entity.setCreateTime(LocalDateTime.now());
        entity.setUpdateTime(LocalDateTime.now());
        sopTemplateMapper.insert(entity);
        return entity.getId();
    }

    @Override
    @Transactional
    @AuditLog(module = "M2-sop", action = "update-template")
    public void update(SopTemplateUpdateReq req) {
        SopTemplateDO existing = requireTemplate(req.getId());
        if (StrUtil.isNotBlank(req.getTemplateName())) {
            existing.setTemplateName(req.getTemplateName().trim());
        }
        if (req.getContentType() != null) {
            existing.setContentType(req.getContentType());
        }
        if (req.getPlatformType() != null) {
            existing.setPlatformType(req.getPlatformType());
        }
        if (req.getDescription() != null) {
            existing.setDescription(req.getDescription());
        }
        if (req.getStatus() != null) {
            if (req.getStatus() == 1) {
                long nodeCount = sopNodeMapper.selectCount(new LambdaQueryWrapper<SopNodeDO>()
                        .eq(SopNodeDO::getTemplateId, existing.getId()));
                if (nodeCount == 0) {
                    throw new ServiceException(OaErrorCodes.SOP_TEMPLATE_NO_NODES);
                }
            }
            existing.setStatus(req.getStatus());
        }
        existing.setUpdater(TenantContextHolder.getUsername());
        existing.setUpdateTime(LocalDateTime.now());
        sopTemplateMapper.updateById(existing);
    }

    @Override
    @Transactional
    @AuditLog(module = "M2-sop", action = "delete-template")
    public void delete(Long id) {
        SopTemplateDO existing = requireTemplate(id);
        long taskCount = taskMapper.selectCount(new LambdaQueryWrapper<TaskDO>()
                .eq(TaskDO::getTenantId, existing.getTenantId())
                .eq(TaskDO::getTemplateId, id));
        if (taskCount > 0) {
            throw new ServiceException(OaErrorCodes.ENTITY_ALREADY_BOUND);
        }
        existing.setStatus(0);
        existing.setUpdater(TenantContextHolder.getUsername());
        existing.setUpdateTime(LocalDateTime.now());
        sopTemplateMapper.updateById(existing);
    }

    private SopTemplateVO toVO(SopTemplateDO entity) {
        SopTemplateVO vo = new SopTemplateVO();
        vo.setId(entity.getId());
        vo.setTemplateName(entity.getTemplateName());
        vo.setContentType(entity.getContentType());
        vo.setPlatformType(entity.getPlatformType());
        vo.setDescription(entity.getDescription());
        vo.setStatus(entity.getStatus());
        vo.setCreateTime(entity.getCreateTime());
        Long nodeCount = sopNodeMapper.selectCount(new LambdaQueryWrapper<SopNodeDO>()
                .eq(SopNodeDO::getTemplateId, entity.getId()));
        vo.setNodeCount(nodeCount == null ? 0 : nodeCount.intValue());
        return vo;
    }

    SopTemplateDO requireTemplate(Long id) {
        SopTemplateDO entity = sopTemplateMapper.selectById(id);
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
