package cn.iocoder.yudao.module.oa.service.operations;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.exception.OaErrorCodes;
import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.oa.api.dto.operations.ContentImportVO;
import cn.iocoder.yudao.module.oa.api.dto.operations.ImportContentDataReq;
import cn.iocoder.yudao.module.oa.api.dto.operations.ImportReviewReq;
import cn.iocoder.yudao.module.oa.api.dto.operations.InternalContentVO;
import cn.iocoder.yudao.module.oa.dal.dataobject.account.AccountDO;
import cn.iocoder.yudao.module.oa.dal.dataobject.auth.SysUserDO;
import cn.iocoder.yudao.module.oa.dal.dataobject.operations.ContentDO;
import cn.iocoder.yudao.module.oa.dal.dataobject.operations.ContentDataImportDO;
import cn.iocoder.yudao.module.oa.dal.mysql.account.AccountMapper;
import cn.iocoder.yudao.module.oa.dal.mysql.auth.SysUserMapper;
import cn.iocoder.yudao.module.oa.dal.mysql.operations.ContentDataImportMapper;
import cn.iocoder.yudao.module.oa.dal.mysql.operations.ContentMapper;
import cn.iocoder.yudao.module.oa.framework.audit.AuditLog;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class InternalContentServiceImpl implements InternalContentService {

    private static final int REVIEW_PENDING = 0;
    private static final int REVIEW_APPROVED = 1;

    private final ContentMapper contentMapper;
    private final ContentDataImportMapper contentDataImportMapper;
    private final AccountMapper accountMapper;
    private final SysUserMapper sysUserMapper;

    @Override
    public PageResult<InternalContentVO> list(String platformType, String dataSource, Integer pageNo, Integer pageSize) {
        Long tenantId = requireTenantId();
        LambdaQueryWrapper<ContentDO> wrapper = new LambdaQueryWrapper<ContentDO>()
                .eq(ContentDO::getTenantId, tenantId)
                .eq(StrUtil.isNotBlank(platformType), ContentDO::getPlatformType, platformType)
                .eq(StrUtil.isNotBlank(dataSource), ContentDO::getDataSource, dataSource)
                .orderByDesc(ContentDO::getPublishTime);
        Page<ContentDO> page = contentMapper.selectPage(
                new Page<>(pageNo == null ? 1 : pageNo, pageSize == null ? 20 : pageSize), wrapper);
        return new PageResult<>(page.getRecords().stream().map(this::toInternalVO).collect(Collectors.toList()), page.getTotal());
    }

    @Override
    @Transactional
    @AuditLog(module = "M1-internal-content", action = "import-submit")
    public Long submitImport(ImportContentDataReq req) {
        Long tenantId = requireTenantId();
        validateStatDate(req.getStatDate());
        ContentDO content = contentMapper.selectById(req.getContentId());
        if (content == null || !Objects.equals(content.getTenantId(), tenantId)) {
            throw new ServiceException(OaErrorCodes.IMPORT_CONTENT_NOT_FOUND);
        }
        assertNoApprovedImport(tenantId, req.getContentId(), req.getStatDate());

        ContentDataImportDO entity = new ContentDataImportDO();
        entity.setTenantId(tenantId);
        entity.setContentId(req.getContentId());
        entity.setStatDate(req.getStatDate());
        entity.setImportType(req.getImportType());
        entity.setReadCount(req.getReadCount());
        entity.setLikeCount(req.getLikeCount());
        entity.setCommentCount(req.getCommentCount());
        entity.setForwardCount(req.getForwardCount());
        entity.setFollowerChange(req.getFollowerChange());
        entity.setReviewStatus(REVIEW_PENDING);
        entity.setRemark(req.getRemark());
        entity.setSubmitterId(TenantContextHolder.getUserId());
        entity.setCreator(TenantContextHolder.getUsername());
        entity.setUpdater(TenantContextHolder.getUsername());
        entity.setCreateTime(LocalDateTime.now());
        entity.setUpdateTime(LocalDateTime.now());
        contentDataImportMapper.insert(entity);
        return entity.getId();
    }

    @Override
    public PageResult<ContentImportVO> importList(Integer reviewStatus, Integer pageNo, Integer pageSize) {
        Long tenantId = requireTenantId();
        LambdaQueryWrapper<ContentDataImportDO> wrapper = new LambdaQueryWrapper<ContentDataImportDO>()
                .eq(ContentDataImportDO::getTenantId, tenantId)
                .eq(reviewStatus != null, ContentDataImportDO::getReviewStatus, reviewStatus)
                .orderByDesc(ContentDataImportDO::getId);
        Page<ContentDataImportDO> page = contentDataImportMapper.selectPage(
                new Page<>(pageNo == null ? 1 : pageNo, pageSize == null ? 20 : pageSize), wrapper);
        return new PageResult<>(page.getRecords().stream().map(this::toImportVO).collect(Collectors.toList()), page.getTotal());
    }

    @Override
    @Transactional
    @AuditLog(module = "M1-internal-content", action = "import-review")
    public void reviewImport(Long id, ImportReviewReq req) {
        ContentDataImportDO existing = requireImport(id);
        if (existing.getReviewStatus() != null && existing.getReviewStatus() == REVIEW_APPROVED) {
            throw new ServiceException(OaErrorCodes.IMPORT_REVIEW_LOCKED);
        }
        existing.setReviewStatus(req.getReviewStatus());
        existing.setRemark(req.getRemark());
        existing.setReviewerId(TenantContextHolder.getUserId());
        existing.setReviewTime(LocalDateTime.now());
        existing.setUpdater(TenantContextHolder.getUsername());
        existing.setUpdateTime(LocalDateTime.now());
        contentDataImportMapper.updateById(existing);
    }

    private void validateStatDate(LocalDate statDate) {
        LocalDate minDate = LocalDate.now().minusDays(90);
        if (statDate.isBefore(minDate)) {
            throw new ServiceException(OaErrorCodes.IMPORT_DATE_TOO_OLD);
        }
        if (statDate.isAfter(LocalDate.now())) {
            throw new ServiceException(OaErrorCodes.IMPORT_DATE_TOO_OLD.getCode(), "补录日期不能晚于今天");
        }
    }

    private void assertNoApprovedImport(Long tenantId, Long contentId, LocalDate statDate) {
        long count = contentDataImportMapper.selectCount(new LambdaQueryWrapper<ContentDataImportDO>()
                .eq(ContentDataImportDO::getTenantId, tenantId)
                .eq(ContentDataImportDO::getContentId, contentId)
                .eq(ContentDataImportDO::getStatDate, statDate)
                .eq(ContentDataImportDO::getReviewStatus, REVIEW_APPROVED));
        if (count > 0) {
            throw new ServiceException(OaErrorCodes.IMPORT_ALREADY_APPROVED);
        }
    }

    private InternalContentVO toInternalVO(ContentDO content) {
        InternalContentVO vo = new InternalContentVO();
        vo.setId(content.getId());
        vo.setAccountId(content.getAccountId());
        AccountDO account = accountMapper.selectById(content.getAccountId());
        if (account != null) {
            vo.setAccountName(account.getAccountName());
        }
        vo.setTitle(content.getTitle());
        vo.setPlatformType(content.getPlatformType());
        vo.setContentType(content.getContentType());
        vo.setPublishTime(content.getPublishTime());
        vo.setReadCount(content.getReadCount());
        vo.setLikeCount(content.getLikeCount());
        vo.setDataSource(content.getDataSource());
        vo.setIsHit(content.getIsHit() != null && content.getIsHit() == 1);
        return vo;
    }

    private ContentImportVO toImportVO(ContentDataImportDO entity) {
        ContentImportVO vo = new ContentImportVO();
        vo.setId(entity.getId());
        vo.setContentId(entity.getContentId());
        ContentDO content = contentMapper.selectById(entity.getContentId());
        if (content != null) {
            vo.setContentTitle(content.getTitle());
        }
        vo.setStatDate(entity.getStatDate());
        vo.setImportType(entity.getImportType());
        vo.setReadCount(entity.getReadCount());
        vo.setLikeCount(entity.getLikeCount());
        vo.setCommentCount(entity.getCommentCount());
        vo.setForwardCount(entity.getForwardCount());
        vo.setFollowerChange(entity.getFollowerChange());
        vo.setReviewStatus(entity.getReviewStatus());
        vo.setRemark(entity.getRemark());
        vo.setSubmitterId(entity.getSubmitterId());
        if (entity.getSubmitterId() != null) {
            SysUserDO submitter = sysUserMapper.selectById(entity.getSubmitterId());
            if (submitter != null) {
                vo.setSubmitterName(submitter.getNickname() != null ? submitter.getNickname() : submitter.getUsername());
            }
        }
        vo.setReviewerId(entity.getReviewerId());
        vo.setReviewTime(entity.getReviewTime());
        vo.setCreateTime(entity.getCreateTime());
        return vo;
    }

    private ContentDataImportDO requireImport(Long id) {
        ContentDataImportDO entity = contentDataImportMapper.selectById(id);
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
