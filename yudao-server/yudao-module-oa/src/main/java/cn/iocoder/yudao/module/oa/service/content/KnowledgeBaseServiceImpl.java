package cn.iocoder.yudao.module.oa.service.content;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.exception.OaErrorCodes;
import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.oa.api.dto.content.KnowledgeCreateReq;
import cn.iocoder.yudao.module.oa.api.dto.content.KnowledgeLikeReq;
import cn.iocoder.yudao.module.oa.api.dto.content.KnowledgeUpdateReq;
import cn.iocoder.yudao.module.oa.api.dto.content.KnowledgeVO;
import cn.iocoder.yudao.module.oa.dal.dataobject.content.KnowledgeBaseDO;
import cn.iocoder.yudao.module.oa.dal.mysql.content.KnowledgeBaseMapper;
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
public class KnowledgeBaseServiceImpl implements KnowledgeBaseService {

    private final KnowledgeBaseMapper knowledgeBaseMapper;

    @Override
    public PageResult<KnowledgeVO> list(String title, String category, Integer pageNum, Integer pageSize) {
        Long tenantId = requireTenantId();
        LambdaQueryWrapper<KnowledgeBaseDO> wrapper = new LambdaQueryWrapper<KnowledgeBaseDO>()
                .eq(KnowledgeBaseDO::getTenantId, tenantId)
                .like(StrUtil.isNotBlank(title), KnowledgeBaseDO::getTitle, title)
                .eq(StrUtil.isNotBlank(category), KnowledgeBaseDO::getCategory, category)
                .orderByDesc(KnowledgeBaseDO::getId);
        Page<KnowledgeBaseDO> page = knowledgeBaseMapper.selectPage(
                new Page<>(pageNum == null ? 1 : pageNum, pageSize == null ? 20 : pageSize), wrapper);
        return new PageResult<>(page.getRecords().stream().map(this::toVO).collect(Collectors.toList()), page.getTotal());
    }

    @Override
    public KnowledgeVO getById(Long id) {
        KnowledgeBaseDO entity = requireEntity(id);
        return toVO(entity);
    }

    @Override
    @Transactional
    @AuditLog(module = "M2-knowledge", action = "create")
    public Long create(KnowledgeCreateReq req) {
        Long tenantId = requireTenantId();
        KnowledgeBaseDO entity = new KnowledgeBaseDO();
        entity.setTenantId(tenantId);
        entity.setTitle(req.getTitle().trim());
        entity.setContent(req.getContent());
        entity.setCategory(req.getCategory());
        entity.setTags(req.getTags());
        entity.setIsPublic(req.getIsPublic() == null ? 1 : req.getIsPublic());
        entity.setStatus(1);
        entity.setCreator(TenantContextHolder.getUsername());
        entity.setUpdater(TenantContextHolder.getUsername());
        entity.setCreateTime(LocalDateTime.now());
        entity.setUpdateTime(LocalDateTime.now());
        knowledgeBaseMapper.insert(entity);
        return entity.getId();
    }

    @Override
    @Transactional
    @AuditLog(module = "M2-knowledge", action = "update")
    public void update(KnowledgeUpdateReq req) {
        KnowledgeBaseDO existing = requireEntity(req.getId());
        existing.setTitle(req.getTitle().trim());
        existing.setContent(req.getContent());
        existing.setCategory(req.getCategory());
        existing.setTags(req.getTags());
        if (req.getIsPublic() != null) {
            existing.setIsPublic(req.getIsPublic());
        }
        existing.setUpdater(TenantContextHolder.getUsername());
        existing.setUpdateTime(LocalDateTime.now());
        knowledgeBaseMapper.updateById(existing);
    }

    @Override
    @Transactional
    @AuditLog(module = "M2-knowledge", action = "delete")
    public void delete(Long id) {
        KnowledgeBaseDO existing = requireEntity(id);
        knowledgeBaseMapper.deleteById(existing.getId());
    }

    @Override
    @Transactional
    @AuditLog(module = "M2-knowledge", action = "toggle-like")
    public void toggleLike(KnowledgeLikeReq req) {
        // P-GATE-UNMOCK-R S-R1 P0-1：S-R1 仅触发 updateTime 占位；S-R2 补 like 表
        if (!"like".equals(req.getAction()) && !"unlike".equals(req.getAction())) {
            throw new ServiceException(OaErrorCodes.BAD_REQUEST.getCode(), "action 必须为 like/unlike");
        }
        KnowledgeBaseDO existing = requireEntity(req.getId());
        existing.setUpdater(TenantContextHolder.getUsername());
        existing.setUpdateTime(LocalDateTime.now());
        knowledgeBaseMapper.updateById(existing);
    }

    private KnowledgeBaseDO requireEntity(Long id) {
        KnowledgeBaseDO entity = knowledgeBaseMapper.selectById(id);
        if (entity == null || !Objects.equals(entity.getTenantId(), requireTenantId())) {
            throw new ServiceException(OaErrorCodes.ENTITY_NOT_EXISTS);
        }
        return entity;
    }

    private KnowledgeVO toVO(KnowledgeBaseDO entity) {
        KnowledgeVO vo = new KnowledgeVO();
        vo.setId(entity.getId());
        vo.setTitle(entity.getTitle());
        vo.setContent(entity.getContent());
        vo.setCategory(entity.getCategory());
        vo.setTags(entity.getTags());
        vo.setIsPublic(entity.getIsPublic());
        vo.setStatus(entity.getStatus());
        vo.setCreateTime(entity.getCreateTime());
        vo.setUpdateTime(entity.getUpdateTime());
        vo.setCreatorName(entity.getCreator());
        return vo;
    }

    private Long requireTenantId() {
        Long tenantId = TenantContextHolder.getTenantId();
        if (tenantId == null) {
            throw new ServiceException(OaErrorCodes.UNAUTHORIZED.getCode(), "缺少租户上下文");
        }
        return tenantId;
    }
}
