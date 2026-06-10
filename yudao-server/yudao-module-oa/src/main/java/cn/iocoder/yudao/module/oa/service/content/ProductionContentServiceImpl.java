package cn.iocoder.yudao.module.oa.service.content;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.exception.OaErrorCodes;
import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.oa.api.dto.content.ContentReviewReq;
import cn.iocoder.yudao.module.oa.api.dto.content.ProductionContentCreateReq;
import cn.iocoder.yudao.module.oa.api.dto.content.ProductionContentUpdateReq;
import cn.iocoder.yudao.module.oa.api.dto.content.ProductionContentVO;
import cn.iocoder.yudao.module.oa.dal.dataobject.account.AccountDO;
import cn.iocoder.yudao.module.oa.dal.dataobject.auth.SysUserDO;
import cn.iocoder.yudao.module.oa.dal.dataobject.content.ProductionContentDO;
import cn.iocoder.yudao.module.oa.dal.dataobject.content.ReviewRecordDO;
import cn.iocoder.yudao.module.oa.dal.mysql.account.AccountMapper;
import cn.iocoder.yudao.module.oa.dal.mysql.auth.SysUserMapper;
import cn.iocoder.yudao.module.oa.dal.mysql.content.ProductionContentMapper;
import cn.iocoder.yudao.module.oa.dal.mysql.content.ReviewRecordMapper;
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
public class ProductionContentServiceImpl implements ProductionContentService {

    private final ProductionContentMapper productionContentMapper;
    private final ReviewRecordMapper reviewRecordMapper;
    private final AccountMapper accountMapper;
    private final SysUserMapper sysUserMapper;

    @Override
    public PageResult<ProductionContentVO> list(String title, String platformType, String contentType,
                                                Long accountId, String status, Integer aiGenerated,
                                                Integer pageNum, Integer pageSize) {
        Long tenantId = requireTenantId();
        LambdaQueryWrapper<ProductionContentDO> wrapper = new LambdaQueryWrapper<ProductionContentDO>()
                .eq(ProductionContentDO::getTenantId, tenantId)
                .like(StrUtil.isNotBlank(title), ProductionContentDO::getTitle, title)
                .eq(StrUtil.isNotBlank(platformType), ProductionContentDO::getPlatformType, platformType)
                .eq(StrUtil.isNotBlank(contentType), ProductionContentDO::getContentType, contentType)
                .eq(accountId != null, ProductionContentDO::getAccountId, accountId)
                .eq(StrUtil.isNotBlank(status), ProductionContentDO::getStatus, status)
                .eq(aiGenerated != null, ProductionContentDO::getAiGenerated, aiGenerated)
                .orderByDesc(ProductionContentDO::getId);
        Page<ProductionContentDO> page = productionContentMapper.selectPage(
                new Page<>(pageNum == null ? 1 : pageNum, pageSize == null ? 20 : pageSize), wrapper);
        return new PageResult<>(page.getRecords().stream().map(this::toVO).collect(Collectors.toList()), page.getTotal());
    }

    @Override
    @Transactional
    @AuditLog(module = "M2-content", action = "create")
    public Long create(ProductionContentCreateReq req) {
        Long tenantId = requireTenantId();
        validateAccount(tenantId, req.getAccountId(), req.getPlatformType());
        ProductionContentDO entity = new ProductionContentDO();
        entity.setTenantId(tenantId);
        entity.setTitle(req.getTitle().trim());
        entity.setBody(req.getBody());
        entity.setCoverImage(req.getCoverImage());
        entity.setCreatorUserId(req.getCreatorUserId());
        entity.setAccountId(req.getAccountId());
        entity.setPlatformType(req.getPlatformType());
        entity.setContentType(req.getContentType());
        entity.setStatus("DRAFT");
        entity.setAiGenerated(req.getAiGenerated() == null ? 0 : req.getAiGenerated());
        entity.setCreator(TenantContextHolder.getUsername());
        entity.setUpdater(TenantContextHolder.getUsername());
        entity.setCreateTime(LocalDateTime.now());
        entity.setUpdateTime(LocalDateTime.now());
        productionContentMapper.insert(entity);
        return entity.getId();
    }

    @Override
    @Transactional
    @AuditLog(module = "M2-content", action = "update")
    public void update(ProductionContentUpdateReq req) {
        ProductionContentDO existing = requireContent(req.getId());
        if (!"DRAFT".equals(existing.getStatus()) && !"REJECTED".equals(existing.getStatus())) {
            throw new ServiceException(OaErrorCodes.CONTENT_STATUS_INVALID);
        }
        if (StrUtil.isNotBlank(req.getTitle())) {
            existing.setTitle(req.getTitle().trim());
        }
        if (req.getBody() != null) {
            existing.setBody(req.getBody());
        }
        if (req.getCoverImage() != null) {
            existing.setCoverImage(req.getCoverImage());
        }
        if (req.getCreatorUserId() != null) {
            existing.setCreatorUserId(req.getCreatorUserId());
        }
        if (req.getAccountId() != null) {
            String platform = req.getPlatformType() != null ? req.getPlatformType() : existing.getPlatformType();
            validateAccount(existing.getTenantId(), req.getAccountId(), platform);
            existing.setAccountId(req.getAccountId());
        }
        if (req.getPlatformType() != null) {
            validateAccount(existing.getTenantId(), existing.getAccountId(), req.getPlatformType());
            existing.setPlatformType(req.getPlatformType());
        }
        if (req.getContentType() != null) {
            existing.setContentType(req.getContentType());
        }
        if (req.getAiGenerated() != null) {
            existing.setAiGenerated(req.getAiGenerated());
        }
        if ("REJECTED".equals(existing.getStatus())) {
            existing.setStatus("DRAFT");
        }
        existing.setUpdater(TenantContextHolder.getUsername());
        existing.setUpdateTime(LocalDateTime.now());
        productionContentMapper.updateById(existing);
    }

    @Override
    @Transactional
    @AuditLog(module = "M2-content", action = "submit-review")
    public void submitReview(Long id) {
        ProductionContentDO content = requireContent(id);
        if (!"DRAFT".equals(content.getStatus())) {
            throw new ServiceException(OaErrorCodes.CONTENT_STATUS_INVALID);
        }
        content.setStatus("PENDING_FIRST_REVIEW");
        content.setUpdater(TenantContextHolder.getUsername());
        content.setUpdateTime(LocalDateTime.now());
        productionContentMapper.updateById(content);
        saveReviewRecord(content, "FIRST_REVIEW", "SUBMIT", TenantContextHolder.getUserId(), null);
    }

    @Override
    @Transactional
    @AuditLog(module = "M2-content", action = "delete")
    public void delete(Long id) {
        ProductionContentDO content = requireContent(id);
        if (!"DRAFT".equals(content.getStatus()) && !"REJECTED".equals(content.getStatus())) {
            throw new ServiceException(OaErrorCodes.CONTENT_STATUS_INVALID);
        }
        productionContentMapper.deleteById(id);
    }

    @Override
    @Transactional
    @AuditLog(module = "M2-content", action = "review")
    public void review(Long id, ContentReviewReq req) {
        ProductionContentDO content = requireContent(id);
        validateReviewStage(content.getStatus(), req.getStage());
        Long reviewerId = TenantContextHolder.getUserId();
        if (reviewerId == null) {
            throw new ServiceException(OaErrorCodes.UNAUTHORIZED);
        }

        if ("REJECT".equals(req.getAction())) {
            content.setStatus("REJECTED");
            saveReviewRecord(content, req.getStage(), req.getAction(), reviewerId, req.getComment());
        } else if ("APPROVE".equals(req.getAction())) {
            String nextStatus = nextStatusAfterApprove(content.getStatus(), req.getStage());
            content.setStatus(nextStatus);
            saveReviewRecord(content, req.getStage(), req.getAction(), reviewerId, req.getComment());
        } else {
            throw new ServiceException(OaErrorCodes.DICT_VALUE_INVALID);
        }
        content.setUpdater(TenantContextHolder.getUsername());
        content.setUpdateTime(LocalDateTime.now());
        productionContentMapper.updateById(content);
    }

    private String nextStatusAfterApprove(String currentStatus, String stage) {
        if ("FIRST_REVIEW".equals(stage) && "PENDING_FIRST_REVIEW".equals(currentStatus)) {
            return "PENDING_SECOND_REVIEW";
        }
        if ("SECOND_REVIEW".equals(stage) && "PENDING_SECOND_REVIEW".equals(currentStatus)) {
            return "PENDING_FINAL_REVIEW";
        }
        if ("FINAL_REVIEW".equals(stage) && "PENDING_FINAL_REVIEW".equals(currentStatus)) {
            return "PUBLISHED";
        }
        throw new ServiceException(OaErrorCodes.CONTENT_STATUS_INVALID);
    }

    private void validateReviewStage(String contentStatus, String stage) {
        if ("FIRST_REVIEW".equals(stage) && !"PENDING_FIRST_REVIEW".equals(contentStatus)) {
            throw new ServiceException(OaErrorCodes.CONTENT_STATUS_INVALID);
        }
        if ("SECOND_REVIEW".equals(stage) && !"PENDING_SECOND_REVIEW".equals(contentStatus)) {
            throw new ServiceException(OaErrorCodes.CONTENT_STATUS_INVALID);
        }
        if ("FINAL_REVIEW".equals(stage) && !"PENDING_FINAL_REVIEW".equals(contentStatus)) {
            throw new ServiceException(OaErrorCodes.CONTENT_STATUS_INVALID);
        }
    }

    private void saveReviewRecord(ProductionContentDO content, String stage, String action,
                                  Long reviewerId, String comment) {
        ReviewRecordDO record = new ReviewRecordDO();
        record.setTenantId(content.getTenantId());
        record.setContentId(content.getId());
        record.setStage(stage);
        record.setAction(action);
        record.setReviewerId(reviewerId);
        record.setComment(comment);
        record.setCreator(TenantContextHolder.getUsername());
        record.setUpdater(TenantContextHolder.getUsername());
        record.setCreateTime(LocalDateTime.now());
        record.setUpdateTime(LocalDateTime.now());
        reviewRecordMapper.insert(record);
    }

    private void validateAccount(Long tenantId, Long accountId, String platformType) {
        AccountDO account = accountMapper.selectById(accountId);
        if (account == null || !Objects.equals(account.getTenantId(), tenantId)) {
            throw new ServiceException(OaErrorCodes.ENTITY_NOT_EXISTS);
        }
        if (!"NORMAL".equals(account.getStatus())) {
            throw new ServiceException(OaErrorCodes.ENTITY_DISABLED);
        }
        if (StrUtil.isNotBlank(platformType) && !platformType.equals(account.getPlatformType())) {
            throw new ServiceException(OaErrorCodes.CONTENT_PLATFORM_MISMATCH);
        }
    }

    private ProductionContentVO toVO(ProductionContentDO entity) {
        ProductionContentVO vo = new ProductionContentVO();
        vo.setId(entity.getId());
        vo.setTitle(entity.getTitle());
        vo.setBody(entity.getBody());
        vo.setCoverImage(entity.getCoverImage());
        vo.setCreatorUserId(entity.getCreatorUserId());
        SysUserDO creator = sysUserMapper.selectById(entity.getCreatorUserId());
        if (creator != null) {
            vo.setCreatorUserName(creator.getNickname() != null ? creator.getNickname() : creator.getUsername());
        }
        vo.setAccountId(entity.getAccountId());
        AccountDO account = accountMapper.selectById(entity.getAccountId());
        if (account != null) {
            vo.setAccountName(account.getAccountName());
        }
        vo.setPlatformType(entity.getPlatformType());
        vo.setContentType(entity.getContentType());
        vo.setStatus(entity.getStatus());
        vo.setAiGenerated(entity.getAiGenerated());
        vo.setCreateTime(entity.getCreateTime());
        return vo;
    }

    private ProductionContentDO requireContent(Long id) {
        ProductionContentDO entity = productionContentMapper.selectById(id);
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
