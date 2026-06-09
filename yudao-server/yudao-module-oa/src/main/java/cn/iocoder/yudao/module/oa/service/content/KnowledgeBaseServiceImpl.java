package cn.iocoder.yudao.module.oa.service.content;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.exception.OaErrorCodes;
import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.oa.api.dto.content.KnowledgeCreateReq;
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
