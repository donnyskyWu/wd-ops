package cn.iocoder.yudao.module.oa.service.content;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.exception.OaErrorCodes;
import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.oa.api.dto.content.LayoutStyleCreateReq;
import cn.iocoder.yudao.module.oa.api.dto.content.LayoutStyleUpdateReq;
import cn.iocoder.yudao.module.oa.api.dto.content.LayoutStyleVO;
import cn.iocoder.yudao.module.oa.dal.dataobject.content.LayoutStyleDO;
import cn.iocoder.yudao.module.oa.dal.mysql.content.LayoutStyleMapper;
import cn.iocoder.yudao.module.oa.framework.audit.AuditLog;
import cn.iocoder.yudao.module.oa.util.LayoutJsonHelper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class LayoutStyleServiceImpl implements LayoutStyleService {

    private final LayoutStyleMapper layoutStyleMapper;

    @Override
    public PageResult<LayoutStyleVO> list(String name, String category, String tags, String status,
                                          Integer pageNum, Integer pageSize) {
        Long tenantId = requireTenantId();
        LambdaQueryWrapper<LayoutStyleDO> wrapper = new LambdaQueryWrapper<LayoutStyleDO>()
                .eq(LayoutStyleDO::getTenantId, tenantId)
                .like(StrUtil.isNotBlank(name), LayoutStyleDO::getName, name)
                .eq(StrUtil.isNotBlank(category), LayoutStyleDO::getCategory, category)
                .eq(StrUtil.isNotBlank(status), LayoutStyleDO::getStatus, status)
                .and(StrUtil.isNotBlank(tags), w -> w.like(LayoutStyleDO::getTags, tags)
                        .or().like(LayoutStyleDO::getName, tags))
                .orderByAsc(LayoutStyleDO::getSort)
                .orderByDesc(LayoutStyleDO::getId);
        Page<LayoutStyleDO> page = layoutStyleMapper.selectPage(
                new Page<>(pageNum == null ? 1 : pageNum, pageSize == null ? 50 : pageSize), wrapper);
        return new PageResult<>(page.getRecords().stream().map(this::toVO).collect(Collectors.toList()), page.getTotal());
    }

    @Override
    public List<LayoutStyleVO> listEnabled(String category, String keyword) {
        Long tenantId = requireTenantId();
        LambdaQueryWrapper<LayoutStyleDO> wrapper = new LambdaQueryWrapper<LayoutStyleDO>()
                .eq(LayoutStyleDO::getTenantId, tenantId)
                .eq(LayoutStyleDO::getStatus, "ENABLED")
                .eq(StrUtil.isNotBlank(category), LayoutStyleDO::getCategory, category)
                .and(StrUtil.isNotBlank(keyword), w -> w.like(LayoutStyleDO::getName, keyword)
                        .or().like(LayoutStyleDO::getTags, keyword)
                        .or().like(LayoutStyleDO::getStyleCode, keyword))
                .orderByAsc(LayoutStyleDO::getSort)
                .orderByDesc(LayoutStyleDO::getId);
        return layoutStyleMapper.selectList(wrapper).stream().map(this::toVO).collect(Collectors.toList());
    }

    @Override
    public LayoutStyleVO getById(Long id) {
        return toVO(requireEntity(id));
    }

    @Override
    @Transactional
    @AuditLog(module = "M2-layout-style", action = "create")
    public Long create(LayoutStyleCreateReq req) {
        Long tenantId = requireTenantId();
        assertUniqueCode(tenantId, req.getStyleCode().trim(), null);
        LayoutStyleDO entity = new LayoutStyleDO();
        entity.setTenantId(tenantId);
        entity.setStyleCode(req.getStyleCode().trim());
        entity.setName(req.getName().trim());
        entity.setCategory(req.getCategory());
        entity.setTags(req.getTags());
        entity.setHtmlSnippet(sanitizeSnippet(req.getHtmlSnippet()));
        entity.setThumbnailFileId(req.getThumbnailFileId());
        entity.setSort(req.getSort() != null ? req.getSort() : 0);
        entity.setStatus(StrUtil.blankToDefault(req.getStatus(), "ENABLED"));
        entity.setCreator(TenantContextHolder.getUsername());
        entity.setUpdater(TenantContextHolder.getUsername());
        entity.setCreateTime(LocalDateTime.now());
        entity.setUpdateTime(LocalDateTime.now());
        layoutStyleMapper.insert(entity);
        return entity.getId();
    }

    @Override
    @Transactional
    @AuditLog(module = "M2-layout-style", action = "update")
    public void update(LayoutStyleUpdateReq req) {
        LayoutStyleDO existing = requireEntity(req.getId());
        if (StrUtil.isNotBlank(req.getName())) {
            existing.setName(req.getName().trim());
        }
        if (req.getCategory() != null) {
            existing.setCategory(req.getCategory());
        }
        if (req.getTags() != null) {
            existing.setTags(req.getTags());
        }
        if (req.getHtmlSnippet() != null) {
            existing.setHtmlSnippet(sanitizeSnippet(req.getHtmlSnippet()));
        }
        if (req.getThumbnailFileId() != null) {
            existing.setThumbnailFileId(req.getThumbnailFileId());
        }
        if (req.getSort() != null) {
            existing.setSort(req.getSort());
        }
        if (StrUtil.isNotBlank(req.getStatus())) {
            existing.setStatus(req.getStatus());
        }
        existing.setUpdater(TenantContextHolder.getUsername());
        existing.setUpdateTime(LocalDateTime.now());
        layoutStyleMapper.updateById(existing);
    }

    @Override
    @Transactional
    @AuditLog(module = "M2-layout-style", action = "delete")
    public void delete(Long id) {
        layoutStyleMapper.deleteById(requireEntity(id).getId());
    }

    static String sanitizeSnippet(String html) {
        if (StrUtil.isBlank(html)) {
            throw new ServiceException(OaErrorCodes.BAD_REQUEST.getCode(), "html_snippet 不能为空");
        }
        String cleaned = LayoutJsonHelper.sanitizeHtml(html);
        cleaned = cleaned.replaceAll("(?is)<iframe[^>]*>.*?</iframe>", "");
        cleaned = cleaned.replaceAll("(?i)<iframe[^>]*/?>", "");
        return cleaned;
    }

    private void assertUniqueCode(Long tenantId, String code, Long excludeId) {
        LambdaQueryWrapper<LayoutStyleDO> wrapper = new LambdaQueryWrapper<LayoutStyleDO>()
                .eq(LayoutStyleDO::getTenantId, tenantId)
                .eq(LayoutStyleDO::getStyleCode, code);
        if (excludeId != null) {
            wrapper.ne(LayoutStyleDO::getId, excludeId);
        }
        if (layoutStyleMapper.selectCount(wrapper) > 0) {
            throw new ServiceException(OaErrorCodes.BAD_REQUEST.getCode(), "style_code 已存在");
        }
    }

    private LayoutStyleDO requireEntity(Long id) {
        LayoutStyleDO entity = layoutStyleMapper.selectById(id);
        if (entity == null || !requireTenantId().equals(entity.getTenantId())) {
            throw new ServiceException(OaErrorCodes.ENTITY_NOT_EXISTS);
        }
        return entity;
    }

    private LayoutStyleVO toVO(LayoutStyleDO entity) {
        LayoutStyleVO vo = new LayoutStyleVO();
        vo.setId(entity.getId());
        vo.setStyleCode(entity.getStyleCode());
        vo.setName(entity.getName());
        vo.setCategory(entity.getCategory());
        vo.setTags(entity.getTags());
        vo.setHtmlSnippet(entity.getHtmlSnippet());
        vo.setThumbnailFileId(entity.getThumbnailFileId());
        vo.setSort(entity.getSort());
        vo.setStatus(entity.getStatus());
        vo.setUpdateTime(entity.getUpdateTime());
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
