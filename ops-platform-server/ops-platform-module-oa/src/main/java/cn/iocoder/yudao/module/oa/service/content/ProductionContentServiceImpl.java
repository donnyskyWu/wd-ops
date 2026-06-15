package cn.iocoder.yudao.module.oa.service.content;

import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import cn.iocoder.yudao.framework.common.exception.OaErrorCodes;
import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.oa.api.dto.content.ContentAiGenerateReq;
import cn.iocoder.yudao.module.oa.api.dto.content.ContentAiGenerateResultVO;
import cn.iocoder.yudao.module.oa.api.dto.content.ContentAiPromptOptionVO;
import cn.iocoder.yudao.module.oa.api.dto.content.ContentGenerateResultVO;
import cn.iocoder.yudao.module.oa.api.dto.content.ContentReviewConfigVO;
import cn.iocoder.yudao.module.oa.api.dto.content.ContentReviewReq;
import cn.iocoder.yudao.module.oa.api.dto.content.ContentReviewStepVO;
import cn.iocoder.yudao.module.oa.api.dto.content.ContentScriptRefVO;
import cn.iocoder.yudao.module.oa.api.dto.content.ContentTransferKnowledgeResultVO;
import cn.iocoder.yudao.module.oa.api.dto.content.ProductionContentCreateReq;
import cn.iocoder.yudao.module.oa.api.dto.content.ProductionContentUpdateReq;
import cn.iocoder.yudao.module.oa.api.dto.content.ProductionContentVO;
import cn.iocoder.yudao.module.oa.dal.dataobject.account.AccountDO;
import cn.iocoder.yudao.module.oa.dal.dataobject.author.AuthorDO;
import cn.iocoder.yudao.module.oa.dal.dataobject.auth.SysUserDO;
import cn.iocoder.yudao.module.oa.dal.dataobject.config.AiModelConfigDO;
import cn.iocoder.yudao.module.oa.dal.dataobject.config.AiPromptConfigDO;
import cn.iocoder.yudao.module.oa.dal.dataobject.content.KnowledgeBaseDO;
import cn.iocoder.yudao.module.oa.dal.dataobject.content.ProductionContentDO;
import cn.iocoder.yudao.module.oa.dal.dataobject.content.ReviewRecordDO;
import cn.iocoder.yudao.module.oa.dal.dataobject.ipgroup.IpGroupDO;
import cn.iocoder.yudao.module.oa.dal.dataobject.ipgroup.IpGroupMemberDO;
import cn.iocoder.yudao.module.oa.dal.dataobject.plan.ContentPlanCompetitionDO;
import cn.iocoder.yudao.module.oa.dal.dataobject.plan.ContentPlanStepDO;
import cn.iocoder.yudao.module.oa.dal.dataobject.sop.TaskDO;
import cn.iocoder.yudao.module.oa.dal.mysql.account.AccountMapper;
import cn.iocoder.yudao.module.oa.dal.mysql.author.AuthorMapper;
import cn.iocoder.yudao.module.oa.dal.mysql.auth.SysUserMapper;
import cn.iocoder.yudao.module.oa.dal.mysql.config.AiModelConfigMapper;
import cn.iocoder.yudao.module.oa.dal.mysql.config.AiPromptConfigMapper;
import cn.iocoder.yudao.module.oa.dal.mysql.content.KnowledgeBaseMapper;
import cn.iocoder.yudao.module.oa.dal.mysql.content.ProductionContentMapper;
import cn.iocoder.yudao.module.oa.dal.mysql.content.ReviewRecordMapper;
import cn.iocoder.yudao.module.oa.dal.mysql.ipgroup.IpGroupMapper;
import cn.iocoder.yudao.module.oa.dal.mysql.ipgroup.IpGroupMemberMapper;
import cn.iocoder.yudao.module.oa.dal.mysql.plan.ContentPlanCompetitionMapper;
import cn.iocoder.yudao.module.oa.dal.mysql.plan.ContentPlanStepMapper;
import cn.iocoder.yudao.module.oa.dal.mysql.sop.TaskMapper;
import cn.iocoder.yudao.module.oa.framework.audit.AuditLog;
import cn.iocoder.yudao.module.oa.service.notification.NotificationService;
import cn.iocoder.yudao.module.oa.util.AesUtil;
import cn.iocoder.yudao.module.oa.util.LayoutJsonHelper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductionContentServiceImpl implements ProductionContentService {

    private final ProductionContentMapper productionContentMapper;
    private final KnowledgeBaseMapper knowledgeBaseMapper;
    private final ReviewRecordMapper reviewRecordMapper;
    private final AccountMapper accountMapper;
    private final SysUserMapper sysUserMapper;
    private final TaskMapper taskMapper;
    private final IpGroupMapper ipGroupMapper;
    private final IpGroupMemberMapper ipGroupMemberMapper;
    private final AuthorMapper authorMapper;
    private final AiPromptConfigMapper aiPromptConfigMapper;
    private final AiModelConfigMapper aiModelConfigMapper;
    private final ContentPlanStepMapper contentPlanStepMapper;
    private final ContentPlanCompetitionMapper contentPlanCompetitionMapper;
    private final ContentReviewConfigService contentReviewConfigService;
    private final NotificationService notificationService;
    private final AesUtil aesUtil;

    private static final String CONTENT_TYPE_ARTICLE = "ARTICLE";
    private static final String CONTENT_TYPE_SHORT_VIDEO = "SHORT_VIDEO";
    private static final String DOC_TYPE_SHORT_VIDEO_SCRIPT = "SHORT_VIDEO_SCRIPT";
    private static final String MOCK_VIDEO_URL = "https://mock.ops-platform.local/ai-video/placeholder.mp4";
    private static final String KNOWLEDGE_CATEGORY_CASE = "CASE_LIB";

    @Override
    @Transactional
    public ProductionContentVO getByTaskId(Long taskId) {
        Long tenantId = requireTenantId();
        requireTaskInTenant(taskId, tenantId);
        ProductionContentDO entity = productionContentMapper.selectOne(
                new LambdaQueryWrapper<ProductionContentDO>()
                        .eq(ProductionContentDO::getTenantId, tenantId)
                        .eq(ProductionContentDO::getTaskId, taskId)
                        .last("LIMIT 1"));
        if (entity == null) {
            return null;
        }
        alignContentWithTask(entity);
        return toVOWithReview(entity);
    }

    private ProductionContentVO toVOWithReview(ProductionContentDO entity) {
        ProductionContentVO vo = toVO(entity);
        vo.setReviewProgress(buildReviewProgress(entity));
        return vo;
    }

    @Override
    public ProductionContentVO getById(Long id) {
        ProductionContentDO entity = requireContent(id);
        return toVOWithReview(entity);
    }

    @Override
    public PageResult<ProductionContentVO> list(String title, String platformType, String contentType,
                                                Long accountId, String status, Integer aiGenerated,
                                                Integer pageNum, Integer pageSize) {
        Long tenantId = requireTenantId();
        Long userId = TenantContextHolder.getUserId();

        if ("PENDING_FIRST_REVIEW".equals(status)) {
            if (!contentReviewConfigService.hasLevel1ListAccess(userId)) {
                return PageResult.empty();
            }
        } else if ("PENDING_SECOND_REVIEW".equals(status)) {
            if (!contentReviewConfigService.hasLevel2ListAccess(userId)) {
                return PageResult.empty();
            }
        }

        LambdaQueryWrapper<ProductionContentDO> wrapper = new LambdaQueryWrapper<ProductionContentDO>()
                .eq(ProductionContentDO::getTenantId, tenantId)
                .like(StrUtil.isNotBlank(title), ProductionContentDO::getTitle, title)
                .eq(StrUtil.isNotBlank(platformType), ProductionContentDO::getPlatformType, platformType)
                .eq(StrUtil.isNotBlank(contentType), ProductionContentDO::getContentType, contentType)
                .eq(accountId != null, ProductionContentDO::getAccountId, accountId)
                .eq(StrUtil.isNotBlank(status), ProductionContentDO::getStatus, status)
                .eq(aiGenerated != null, ProductionContentDO::getAiGenerated, aiGenerated)
                .orderByDesc(ProductionContentDO::getId);

        if ("PENDING_FIRST_REVIEW".equals(status)
                && !contentReviewConfigService.hasLevel1FullAccess(userId)) {
            List<Long> ledGroupIds = contentReviewConfigService.listIpGroupIdsLedByUser(userId);
            if (ledGroupIds.isEmpty()) {
                return PageResult.empty();
            }
            wrapper.in(ProductionContentDO::getIpGroupId, ledGroupIds);
        }

        Page<ProductionContentDO> page = productionContentMapper.selectPage(
                new Page<>(pageNum == null ? 1 : pageNum, pageSize == null ? 20 : pageSize), wrapper);
        return new PageResult<>(page.getRecords().stream().map(this::toVO).collect(Collectors.toList()), page.getTotal());
    }

    @Override
    @Transactional
    @AuditLog(module = "M2-content", action = "create")
    public Long create(ProductionContentCreateReq req) {
        Long tenantId = requireTenantId();
        boolean taskDriven = req.getTaskId() != null;
        if (taskDriven) {
            assertTaskContentCreatable(req.getTaskId(), tenantId);
        }
        ResolvedContentFields resolved = resolveContentFields(req, tenantId, taskDriven);
        ProductionContentDO entity = new ProductionContentDO();
        entity.setTenantId(tenantId);
        entity.setTitle(req.getTitle().trim());
        entity.setBody(StrUtil.blankToDefault(resolved.body(), ""));
        applyLayoutFields(entity, req.getBodyFormat(), req.getLayoutJson(), req.getLayoutHtml(), req.getLayoutTemplateId());
        entity.setCoverImage(req.getCoverImage());
        entity.setCreatorUserId(req.getCreatorUserId());
        entity.setAccountId(resolved.accountId());
        entity.setPlatformType(resolved.platformType());
        entity.setPlatformTypesJson(ContentJsonHelper.toPlatformTypesJson(resolved.platformTypes()));
        entity.setAccountIdsJson(ContentJsonHelper.toAccountIdsJson(resolved.accountIds()));
        entity.setContentType(req.getContentType());
        entity.setStatus("DRAFT");
        entity.setAiGenerated(req.getAiGenerated() == null ? 0 : req.getAiGenerated());
        entity.setDocumentType(resolved.documentType());
        entity.setIpGroupId(resolved.ipGroupId());
        entity.setAuthorId(resolved.authorId());
        entity.setGeneratedVideoUrl(req.getGeneratedVideoUrl());
        entity.setFinalVideoUrl(req.getFinalVideoUrl());
        if (taskDriven) {
            TaskDO task = requireTaskInTenant(req.getTaskId(), tenantId);
            entity.setTaskId(req.getTaskId());
            entity.setCompetitionId(StrUtil.blankToDefault(req.getCompetitionId(), task.getCompetitionId()));
            entity.setCompetitionName(StrUtil.blankToDefault(req.getCompetitionName(), resolveCompetitionName(task)));
        } else {
            assertCompetitionSelection(req.getCompetitionId(), req.getCompetitionName());
            entity.setCompetitionId(req.getCompetitionId());
            entity.setCompetitionName(req.getCompetitionName());
        }
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
        if (!isEditableStatus(existing)) {
            throw new ServiceException(OaErrorCodes.CONTENT_STATUS_INVALID);
        }
        if (StrUtil.isNotBlank(req.getTitle())) {
            existing.setTitle(req.getTitle().trim());
        }
        if (req.getBody() != null) {
            existing.setBody(req.getBody());
        }
        if (req.getBodyFormat() != null || req.getLayoutJson() != null || req.getLayoutHtml() != null
                || req.getLayoutTemplateId() != null) {
            applyLayoutFields(existing, req.getBodyFormat(), req.getLayoutJson(), req.getLayoutHtml(),
                    req.getLayoutTemplateId());
        }
        if (req.getCoverImage() != null) {
            existing.setCoverImage(req.getCoverImage());
        }
        if (req.getCreatorUserId() != null) {
            existing.setCreatorUserId(req.getCreatorUserId());
        }
        if (req.getAccountIds() != null) {
            List<Long> accountIds = normalizeAccountIds(req.getAccountIds());
            List<String> platformTypes = resolvePlatformTypes(req.getPlatformTypes(), req.getPlatformType(), accountIds, existing.getTenantId());
            validateAccounts(existing.getTenantId(), accountIds, platformTypes);
            existing.setAccountIdsJson(ContentJsonHelper.toAccountIdsJson(accountIds));
            existing.setAccountId(accountIds.isEmpty() ? null : accountIds.get(0));
            existing.setPlatformTypesJson(ContentJsonHelper.toPlatformTypesJson(platformTypes));
            existing.setPlatformType(platformTypes.isEmpty() ? null : platformTypes.get(0));
        } else if (req.getAccountId() != null) {
            List<String> platformTypes = resolvePlatformTypes(req.getPlatformTypes(), req.getPlatformType(), List.of(req.getAccountId()), existing.getTenantId());
            validateAccounts(existing.getTenantId(), List.of(req.getAccountId()), platformTypes);
            existing.setAccountId(req.getAccountId());
            existing.setAccountIdsJson(ContentJsonHelper.toAccountIdsJson(List.of(req.getAccountId())));
            existing.setPlatformTypesJson(ContentJsonHelper.toPlatformTypesJson(platformTypes));
            existing.setPlatformType(platformTypes.isEmpty() ? null : platformTypes.get(0));
        } else if (req.getPlatformTypes() != null || req.getPlatformType() != null) {
            List<String> platformTypes = resolvePlatformTypes(req.getPlatformTypes(), req.getPlatformType(), Collections.emptyList(), existing.getTenantId());
            existing.setPlatformTypesJson(ContentJsonHelper.toPlatformTypesJson(platformTypes));
            existing.setPlatformType(platformTypes.isEmpty() ? null : platformTypes.get(0));
        }
        if (req.getContentType() != null) {
            existing.setContentType(req.getContentType());
        }
        if (req.getAiGenerated() != null) {
            existing.setAiGenerated(req.getAiGenerated());
        }
        if (req.getDocumentType() != null) {
            existing.setDocumentType(req.getDocumentType());
        }
        if (existing.getTaskId() != null) {
            TaskDO task = requireTaskInTenant(existing.getTaskId(), existing.getTenantId());
            if (task.getIpGroupId() != null) {
                assertIpGroupInTenant(task.getIpGroupId(), existing.getTenantId());
                existing.setIpGroupId(task.getIpGroupId());
            }
        } else if (req.getIpGroupId() != null) {
            assertIpGroupInTenant(req.getIpGroupId(), existing.getTenantId());
            assertUserMemberOfIpGroup(TenantContextHolder.getUserId(), req.getIpGroupId(), existing.getTenantId());
            existing.setIpGroupId(req.getIpGroupId());
        }
        if (req.getAuthorId() != null) {
            assertAuthorInIpGroup(req.getAuthorId(), existing.getIpGroupId(), existing.getTenantId());
            existing.setAuthorId(req.getAuthorId());
        }
        if (req.getGeneratedVideoUrl() != null) {
            existing.setGeneratedVideoUrl(req.getGeneratedVideoUrl());
        }
        if (req.getFinalVideoUrl() != null) {
            existing.setFinalVideoUrl(req.getFinalVideoUrl());
        }
        if (req.getCompetitionId() != null) {
            assertCompetitionSelection(req.getCompetitionId(), req.getCompetitionName());
            existing.setCompetitionId(req.getCompetitionId());
            existing.setCompetitionName(req.getCompetitionName());
        }
        if (shouldResetToDraftOnEdit(existing)) {
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
        if (!isSubmittableStatus(content.getStatus())) {
            throw new ServiceException(OaErrorCodes.CONTENT_STATUS_INVALID);
        }
        if (content.getIpGroupId() == null) {
            throw new ServiceException(OaErrorCodes.BAD_REQUEST.getCode(), "提交审核前须指定 IP 组");
        }
        String nextStatus = contentReviewConfigService.resolveInitialReviewStatus();
        content.setStatus(nextStatus);
        content.setUpdater(TenantContextHolder.getUsername());
        content.setUpdateTime(LocalDateTime.now());
        productionContentMapper.updateById(content);
        saveReviewRecord(content, contentReviewConfigService.resolveSubmitStage(),
                "SUBMIT", TenantContextHolder.getUserId(), null);
        notifyAfterSubmitReview(content);
    }

    private void notifyAfterSubmitReview(ProductionContentDO content) {
        try {
            String stage = contentReviewConfigService.resolveSubmitStage();
            notificationService.notifyContentReviewSubmit(content, stage);
        } catch (Exception ignored) {
            // 通知失败不影响提交流程
        }
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
        if (!contentReviewConfigService.canReview(reviewerId, content, req.getStage())) {
            throw new ServiceException(OaErrorCodes.FORBIDDEN.getCode(), "无当前审核级别权限");
        }

        if ("REJECT".equals(req.getAction())) {
            content.setStatus("REJECTED");
            saveReviewRecord(content, req.getStage(), req.getAction(), reviewerId, req.getComment());
        } else if ("APPROVE".equals(req.getAction())) {
            String nextStatus = nextStatusAfterApprove(content.getStatus(), req.getStage());
            content.setStatus(nextStatus);
            saveReviewRecord(content, req.getStage(), req.getAction(), reviewerId, req.getComment());
            notifyAfterReviewApprove(content, nextStatus);
        } else {
            throw new ServiceException(OaErrorCodes.DICT_VALUE_INVALID);
        }
        content.setUpdater(TenantContextHolder.getUsername());
        content.setUpdateTime(LocalDateTime.now());
        productionContentMapper.updateById(content);
    }

    private void notifyAfterReviewApprove(ProductionContentDO content, String nextStatus) {
        try {
            if ("PENDING_SECOND_REVIEW".equals(nextStatus)) {
                notificationService.notifyContentReviewSubmit(content, "SECOND_REVIEW");
            } else if ("PENDING_PUBLISH".equals(nextStatus)) {
                notificationService.notifyContentReviewApproved(content);
            } else if ("PENDING_FINAL_REVIEW".equals(nextStatus)) {
                notificationService.notifyContentReviewSubmit(content, "FINAL_REVIEW");
            }
        } catch (Exception ignored) {
            // 通知失败不影响审核流程
        }
    }

    private String nextStatusAfterApprove(String currentStatus, String stage) {
        String next = contentReviewConfigService.resolveNextStatusAfterApprove(currentStatus, stage);
        if (next == null) {
            throw new ServiceException(OaErrorCodes.CONTENT_STATUS_INVALID);
        }
        return next;
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

    @Override
    public ContentScriptRefVO getScriptRef(String competitionId, String documentType) {
        Long tenantId = requireTenantId();
        if (StrUtil.isBlank(competitionId)) {
            throw new ServiceException(OaErrorCodes.BAD_REQUEST);
        }
        String docType = StrUtil.blankToDefault(documentType, DOC_TYPE_SHORT_VIDEO_SCRIPT);
        ProductionContentDO ref = productionContentMapper.selectOne(
                new LambdaQueryWrapper<ProductionContentDO>()
                        .eq(ProductionContentDO::getTenantId, tenantId)
                        .eq(ProductionContentDO::getCompetitionId, competitionId)
                        .eq(ProductionContentDO::getContentType, CONTENT_TYPE_ARTICLE)
                        .eq(ProductionContentDO::getDocumentType, docType)
                        .eq(ProductionContentDO::getStatus, "COMPLETED")
                        .orderByDesc(ProductionContentDO::getId)
                        .last("LIMIT 1"));
        if (ref == null) {
            return null;
        }
        ContentScriptRefVO vo = new ContentScriptRefVO();
        vo.setContentId(ref.getId());
        vo.setTitle(ref.getTitle());
        vo.setBody(ref.getBody());
        vo.setDocumentType(ref.getDocumentType());
        vo.setCompetitionId(ref.getCompetitionId());
        return vo;
    }

    @Override
    @Transactional
    @AuditLog(module = "M2-content", action = "confirm")
    public void confirm(Long id) {
        ProductionContentDO content = requireContent(id);
        if (!"DRAFT".equals(content.getStatus())) {
            throw new ServiceException(OaErrorCodes.CONTENT_STATUS_INVALID);
        }
        validateTaskContentCompleteness(content);
        if (CONTENT_TYPE_SHORT_VIDEO.equals(content.getContentType())) {
            if (StrUtil.isBlank(content.getFinalVideoUrl())) {
                content.setFinalVideoUrl(StrUtil.blankToDefault(content.getGeneratedVideoUrl(), MOCK_VIDEO_URL));
            }
        }
        content.setStatus("COMPLETED");
        content.setUpdater(TenantContextHolder.getUsername());
        content.setUpdateTime(LocalDateTime.now());
        productionContentMapper.updateById(content);
    }

    @Override
    @Transactional
    @AuditLog(module = "M2-content", action = "generate")
    public ContentGenerateResultVO generate(Long id) {
        ProductionContentDO content = requireContent(id);
        if (!"DRAFT".equals(content.getStatus()) && !"REJECTED".equals(content.getStatus())) {
            throw new ServiceException(OaErrorCodes.CONTENT_STATUS_INVALID);
        }
        ContentGenerateResultVO result = new ContentGenerateResultVO();
        String promptScene = resolvePromptScene(content);
        AiPromptConfigDO prompt = findPromptConfig(content.getTenantId(), promptScene, content);
        boolean promptMatched = prompt != null;
        result.setPromptMatched(promptMatched);
        if (CONTENT_TYPE_SHORT_VIDEO.equals(content.getContentType())) {
            String videoUrl = MOCK_VIDEO_URL + "?contentId=" + content.getId();
            content.setGeneratedVideoUrl(videoUrl);
            content.setAiGenerated(1);
            result.setGeneratedVideoUrl(videoUrl);
            result.setMessage(promptMatched
                    ? "AI 短视频生成占位（已匹配 M8 提示词 scene=" + promptScene + "，BLK-M2-010）"
                    : "AI 短视频生成占位（未匹配提示词，返回 mock URL，BLK-M2-010）");
        } else {
            String generated = buildMockArticleBody(content, prompt);
            content.setBody(generated);
            content.setAiGenerated(1);
            result.setBody(generated);
            result.setMessage(promptMatched
                    ? "AI 文案生成占位（已匹配 M8 提示词 scene=" + promptScene + "，BLK-M2-005）"
                    : "AI 文案生成占位（未匹配提示词，返回示例文案，BLK-M2-005）");
        }
        content.setUpdater(TenantContextHolder.getUsername());
        content.setUpdateTime(LocalDateTime.now());
        productionContentMapper.updateById(content);
        return result;
    }

    private List<ContentReviewStepVO> buildReviewProgress(ProductionContentDO entity) {
        ContentReviewConfigVO config = contentReviewConfigService.getConfig();
        List<ReviewRecordDO> records = reviewRecordMapper.selectList(
                new LambdaQueryWrapper<ReviewRecordDO>()
                        .eq(ReviewRecordDO::getTenantId, entity.getTenantId())
                        .eq(ReviewRecordDO::getContentId, entity.getId())
                        .orderByAsc(ReviewRecordDO::getCreateTime));

        List<ContentReviewStepVO> steps = new ArrayList<>();
        String contentStatus = entity.getStatus();

        ContentReviewStepVO draft = newStep("DRAFT", "草稿", resolveDraftStepStatus(contentStatus));
        fillFromRecord(draft, findRecordByAction(records, "SUBMIT"));
        if (draft.getCompletedAt() == null && !"DRAFT".equals(contentStatus)) {
            draft.setStepStatus("COMPLETED");
            draft.setCompletedAt(entity.getCreateTime());
        }
        steps.add(draft);

        if (config.isLevel1Enabled()) {
            ContentReviewStepVO first = newStep("FIRST_REVIEW", "一级审核",
                    stepStatusForReview(contentStatus, "PENDING_FIRST_REVIEW", records, "FIRST_REVIEW"));
            fillFromStageRecords(first, records, "FIRST_REVIEW", entity);
            enrichPendingReviewer(first, entity, "FIRST_REVIEW");
            steps.add(first);
        }

        if (config.isLevel2Enabled()) {
            ContentReviewStepVO second = newStep("SECOND_REVIEW", "二级审核",
                    stepStatusForReview(contentStatus, "PENDING_SECOND_REVIEW", records, "SECOND_REVIEW"));
            fillFromStageRecords(second, records, "SECOND_REVIEW", entity);
            enrichPendingReviewer(second, entity, "SECOND_REVIEW");
            steps.add(second);
        }

        ContentReviewStepVO pendingPublish = newStep("PENDING_PUBLISH", "待发布",
                resolvePendingPublishStepStatus(contentStatus));
        steps.add(pendingPublish);

        ContentReviewStepVO published = newStep("PUBLISHED",
                "REJECTED".equals(contentStatus) ? "已驳回" : "已发布",
                resolvePublishedStepStatus(contentStatus));
        if ("PUBLISHED".equals(contentStatus)) {
            ReviewRecordDO lastApprove = findLastAction(records, "APPROVE");
            fillFromRecord(published, lastApprove);
        } else if ("REJECTED".equals(contentStatus)) {
            ReviewRecordDO reject = findLastAction(records, "REJECT");
            fillFromRecord(published, reject);
        }
        steps.add(published);

        return steps;
    }

    private ContentReviewStepVO newStep(String key, String label, String stepStatus) {
        ContentReviewStepVO step = new ContentReviewStepVO();
        step.setStepKey(key);
        step.setLabel(label);
        step.setStepStatus(stepStatus);
        return step;
    }

    private String resolveDraftStepStatus(String contentStatus) {
        if ("DRAFT".equals(contentStatus)) {
            return "IN_PROGRESS";
        }
        return "COMPLETED";
    }

    private String stepStatusForReview(String contentStatus, String pendingStatus,
                                       List<ReviewRecordDO> records, String stage) {
        ReviewRecordDO reject = records.stream()
                .filter(r -> stage.equals(r.getStage()) && "REJECT".equals(r.getAction()))
                .reduce((a, b) -> b)
                .orElse(null);
        if (reject != null && "REJECTED".equals(contentStatus)) {
            return "REJECTED";
        }
        ReviewRecordDO approve = records.stream()
                .filter(r -> stage.equals(r.getStage()) && "APPROVE".equals(r.getAction()))
                .reduce((a, b) -> b)
                .orElse(null);
        if (approve != null) {
            return "COMPLETED";
        }
        if (pendingStatus.equals(contentStatus)) {
            return "IN_PROGRESS";
        }
        if ("DRAFT".equals(contentStatus)) {
            return "WAITING";
        }
        if (reviewStatusOrder(contentStatus) > reviewStatusOrder(pendingStatus)) {
            return "COMPLETED";
        }
        return "WAITING";
    }

    private String resolvePendingPublishStepStatus(String contentStatus) {
        if ("PENDING_PUBLISH".equals(contentStatus)) {
            return "IN_PROGRESS";
        }
        if ("PUBLISHED".equals(contentStatus)) {
            return "COMPLETED";
        }
        if ("REJECTED".equals(contentStatus)) {
            return "WAITING";
        }
        if (reviewStatusOrder(contentStatus) > reviewStatusOrder("PENDING_PUBLISH")) {
            return "COMPLETED";
        }
        return "WAITING";
    }

    private String resolvePublishedStepStatus(String contentStatus) {
        if ("PUBLISHED".equals(contentStatus)) {
            return "COMPLETED";
        }
        if ("REJECTED".equals(contentStatus)) {
            return "REJECTED";
        }
        return "WAITING";
    }

    private int reviewStatusOrder(String status) {
        return switch (status) {
            case "DRAFT" -> 0;
            case "PENDING_FIRST_REVIEW" -> 1;
            case "PENDING_SECOND_REVIEW" -> 2;
            case "PENDING_FINAL_REVIEW" -> 3;
            case "PENDING_PUBLISH" -> 4;
            case "PUBLISHED", "COMPLETED", "REJECTED" -> 5;
            default -> 0;
        };
    }

    private void fillFromStageRecords(ContentReviewStepVO step, List<ReviewRecordDO> records, String stage,
                                      ProductionContentDO entity) {
        ReviewRecordDO approve = records.stream()
                .filter(r -> stage.equals(r.getStage()) && "APPROVE".equals(r.getAction()))
                .reduce((a, b) -> b)
                .orElse(null);
        ReviewRecordDO reject = records.stream()
                .filter(r -> stage.equals(r.getStage()) && "REJECT".equals(r.getAction()))
                .reduce((a, b) -> b)
                .orElse(null);
        if (reject != null && (approve == null || !reject.getCreateTime().isAfter(approve.getCreateTime()))) {
            step.setStepStatus("REJECTED");
            fillFromRecord(step, reject, stage);
        } else if (approve != null) {
            step.setStepStatus("COMPLETED");
            fillFromRecord(step, approve, stage);
        }
    }

    private ReviewRecordDO findRecordByAction(List<ReviewRecordDO> records, String action) {
        return records.stream()
                .filter(r -> action.equals(r.getAction()))
                .reduce((a, b) -> b)
                .orElse(null);
    }

    private ReviewRecordDO findRecord(List<ReviewRecordDO> records, String stage) {
        return records.stream()
                .filter(r -> stage.equals(r.getStage()))
                .reduce((a, b) -> b)
                .orElse(null);
    }

    private ReviewRecordDO findLastAction(List<ReviewRecordDO> records, String action) {
        return records.stream()
                .filter(r -> action.equals(r.getAction()))
                .reduce((a, b) -> b)
                .orElse(null);
    }

    private void fillFromRecord(ContentReviewStepVO step, ReviewRecordDO record, String stage) {
        if (record == null) {
            return;
        }
        step.setCompletedAt(record.getCreateTime());
        step.setComment(record.getComment());
        if (record.getReviewerId() != null) {
            SysUserDO reviewer = sysUserMapper.selectById(record.getReviewerId());
            if (reviewer != null) {
                String userName = reviewer.getNickname() != null ? reviewer.getNickname() : reviewer.getUsername();
                step.setReviewerName(userName);
                String roleCode = contentReviewConfigService.resolveStageRoleCode(stage);
                String roleLabel = contentReviewConfigService.resolveReviewRoleLabel(roleCode);
                step.setReviewerRole(roleLabel);
                step.setReviewerUsers(List.of(userName));
                step.setReviewerDisplay(contentReviewConfigService.formatReviewerDisplay(roleLabel, userName));
            }
        }
    }

    private void fillFromRecord(ContentReviewStepVO step, ReviewRecordDO record) {
        fillFromRecord(step, record, null);
    }

    private void enrichPendingReviewer(ContentReviewStepVO step, ProductionContentDO entity, String stage) {
        if (StrUtil.isNotBlank(step.getReviewerDisplay()) || StrUtil.isNotBlank(step.getReviewerName())) {
            return;
        }
        if (!"IN_PROGRESS".equals(step.getStepStatus()) && !"WAITING".equals(step.getStepStatus())) {
            return;
        }
        String roleCode = contentReviewConfigService.resolveStageRoleCode(stage);
        String roleLabel = contentReviewConfigService.resolveReviewRoleLabel(roleCode);
        List<String> eligibleUsers = contentReviewConfigService.listEligibleReviewerNames(entity, stage);
        step.setReviewerRole(roleLabel);
        step.setReviewerUsers(eligibleUsers);
        step.setReviewerDisplay(contentReviewConfigService.formatReviewerDisplay(roleLabel, eligibleUsers));
        if (eligibleUsers.isEmpty()) {
            step.setReviewerName(roleLabel);
        }
    }

    private ProductionContentVO toVO(ProductionContentDO entity) {
        ProductionContentVO vo = new ProductionContentVO();
        vo.setId(entity.getId());
        vo.setTitle(entity.getTitle());
        vo.setBody(entity.getBody());
        vo.setBodyFormat(entity.getBodyFormat());
        if (StrUtil.isNotBlank(entity.getLayoutJson())) {
            vo.setLayoutJson(JSONUtil.parse(entity.getLayoutJson()));
        }
        vo.setLayoutHtml(entity.getLayoutHtml());
        vo.setLayoutTemplateId(entity.getLayoutTemplateId());
        vo.setCoverImage(entity.getCoverImage());
        vo.setCreatorUserId(entity.getCreatorUserId());
        SysUserDO creator = sysUserMapper.selectById(entity.getCreatorUserId());
        if (creator != null) {
            vo.setCreatorUserName(creator.getNickname() != null ? creator.getNickname() : creator.getUsername());
        }
        vo.setAccountId(entity.getAccountId());
        List<Long> accountIds = ContentJsonHelper.fromAccountIdsJson(entity.getAccountIdsJson());
        if (accountIds.isEmpty() && entity.getAccountId() != null) {
            accountIds = List.of(entity.getAccountId());
        }
        vo.setAccountIds(accountIds);
        List<String> accountNames = new ArrayList<>();
        for (Long accountId : accountIds) {
            AccountDO account = accountMapper.selectById(accountId);
            if (account != null) {
                accountNames.add(account.getAccountName());
            }
        }
        vo.setAccountNames(accountNames);
        if (!accountNames.isEmpty()) {
            vo.setAccountName(accountNames.get(0));
        }
        List<String> platformTypes = ContentJsonHelper.fromPlatformTypesJson(entity.getPlatformTypesJson());
        if (platformTypes.isEmpty() && StrUtil.isNotBlank(entity.getPlatformType())) {
            platformTypes = List.of(entity.getPlatformType());
        }
        vo.setPlatformTypes(platformTypes);
        vo.setPlatformType(platformTypes.isEmpty() ? entity.getPlatformType() : platformTypes.get(0));
        vo.setContentType(entity.getContentType());
        vo.setStatus(entity.getStatus());
        vo.setAiGenerated(entity.getAiGenerated());
        vo.setTaskId(entity.getTaskId());
        vo.setCompetitionId(entity.getCompetitionId());
        vo.setCompetitionName(entity.getCompetitionName());
        vo.setDocumentType(entity.getDocumentType());
        vo.setIpGroupId(entity.getIpGroupId());
        if (entity.getIpGroupId() != null) {
            IpGroupDO ipGroup = ipGroupMapper.selectById(entity.getIpGroupId());
            if (ipGroup != null) {
                vo.setIpGroupName(ipGroup.getGroupName());
            }
        }
        vo.setAuthorId(entity.getAuthorId());
        if (entity.getAuthorId() != null) {
            AuthorDO author = authorMapper.selectById(entity.getAuthorId());
            if (author != null) {
                vo.setAuthorName(author.getAuthorName());
            }
        }
        vo.setGeneratedVideoUrl(entity.getGeneratedVideoUrl());
        vo.setFinalVideoUrl(entity.getFinalVideoUrl());
        vo.setTransferredToKnowledge(entity.getTransferredToKnowledge() == null ? 0 : entity.getTransferredToKnowledge());
        vo.setKnowledgeId(entity.getKnowledgeId());
        vo.setCreateTime(entity.getCreateTime());
        return vo;
    }

    private String resolveKnowledgeContent(ProductionContentDO content) {
        if ("LAYOUT".equals(content.getBodyFormat()) && StrUtil.isNotBlank(content.getLayoutHtml())) {
            return content.getLayoutHtml();
        }
        return content.getBody() != null ? content.getBody() : "";
    }

    private static String truncateKnowledgeTitle(String title) {
        String normalized = StrUtil.blankToDefault(title, "未命名内容").trim();
        return normalized.length() <= 100 ? normalized : normalized.substring(0, 100);
    }

    private static String truncateKnowledgeTags(String tags) {
        if (StrUtil.isBlank(tags)) {
            return tags;
        }
        return tags.length() <= 200 ? tags : tags.substring(0, 200);
    }

    private String buildKnowledgeTags(ProductionContentDO content) {
        Set<String> tags = new LinkedHashSet<>();
        if (StrUtil.isNotBlank(content.getContentType())) {
            tags.add(content.getContentType());
        }
        if (StrUtil.isNotBlank(content.getDocumentType())) {
            tags.add(content.getDocumentType());
        }
        if (StrUtil.isNotBlank(content.getPlatformType())) {
            tags.add(content.getPlatformType());
        }
        for (String platformType : ContentJsonHelper.fromPlatformTypesJson(content.getPlatformTypesJson())) {
            if (StrUtil.isNotBlank(platformType)) {
                tags.add(platformType);
            }
        }
        return String.join(",", tags);
    }

    private record ResolvedContentFields(Long accountId, String platformType, List<String> platformTypes,
                                         List<Long> accountIds, String body,
                                         String documentType, Long ipGroupId, Long authorId) {
    }

    private ResolvedContentFields resolveContentFields(ProductionContentCreateReq req, Long tenantId, boolean taskDriven) {
        Long ipGroupId = req.getIpGroupId();
        Long authorId = req.getAuthorId();
        List<Long> accountIds = normalizeAccountIds(req.getAccountIds());
        if (accountIds.isEmpty() && req.getAccountId() != null) {
            accountIds = List.of(req.getAccountId());
        }
        List<String> platformTypes = resolvePlatformTypes(req.getPlatformTypes(), req.getPlatformType(), accountIds, tenantId);
        String documentType = req.getDocumentType();
        String body = req.getBody();

        if (taskDriven) {
            TaskDO task = requireTaskInTenant(req.getTaskId(), tenantId);
            if (task.getIpGroupId() != null) {
                ipGroupId = task.getIpGroupId();
            }
            if (ipGroupId == null) {
                throw new ServiceException(OaErrorCodes.BAD_REQUEST.getCode(), "任务驱动创作须指定 IP 组");
            }
            assertIpGroupInTenant(ipGroupId, tenantId);
            if (authorId != null) {
                assertAuthorInIpGroup(authorId, ipGroupId, tenantId);
            } else {
                AuthorDO defaultAuthor = findFirstAuthor(ipGroupId, tenantId);
                if (defaultAuthor != null) {
                    authorId = defaultAuthor.getId();
                }
            }
            if (authorId != null && accountIds.isEmpty()) {
                AuthorDO author = authorMapper.selectById(authorId);
                if (author != null && author.getPrimaryAccountId() != null) {
                    accountIds = List.of(author.getPrimaryAccountId());
                    platformTypes = resolvePlatformTypes(platformTypes, null, accountIds, tenantId);
                }
            }
        } else {
            if (ipGroupId == null) {
                throw new ServiceException(OaErrorCodes.BAD_REQUEST.getCode(), "IP 组必填");
            }
            assertIpGroupInTenant(ipGroupId, tenantId);
            assertUserMemberOfIpGroup(TenantContextHolder.getUserId(), ipGroupId, tenantId);
            if (authorId != null) {
                assertAuthorInIpGroup(authorId, ipGroupId, tenantId);
            } else {
                AuthorDO defaultAuthor = findFirstAuthor(ipGroupId, tenantId);
                if (defaultAuthor != null) {
                    authorId = defaultAuthor.getId();
                }
            }
        }

        if (!accountIds.isEmpty()) {
            validateAccounts(tenantId, accountIds, platformTypes);
        }

        if (CONTENT_TYPE_ARTICLE.equals(req.getContentType())) {
            if (StrUtil.isBlank(documentType)) {
                throw new ServiceException(OaErrorCodes.BAD_REQUEST.getCode(), "文档类型必填");
            }
            if (StrUtil.isBlank(body)) {
                throw new ServiceException(OaErrorCodes.BAD_REQUEST.getCode(), "文档正文必填");
            }
        } else if (!CONTENT_TYPE_SHORT_VIDEO.equals(req.getContentType())) {
            if (StrUtil.isBlank(body)) {
                throw new ServiceException(OaErrorCodes.BAD_REQUEST.getCode(), "正文必填");
            }
        }
        Long accountId = accountIds.isEmpty() ? null : accountIds.get(0);
        String platformType = platformTypes.isEmpty() ? null : platformTypes.get(0);
        return new ResolvedContentFields(accountId, platformType, platformTypes, accountIds, body, documentType, ipGroupId, authorId);
    }

    private void validateTaskContentCompleteness(ProductionContentDO content) {
        if (StrUtil.isBlank(content.getTitle())) {
            throw new ServiceException(OaErrorCodes.BAD_REQUEST.getCode(), "标题必填");
        }
        if (content.getIpGroupId() == null) {
            throw new ServiceException(OaErrorCodes.BAD_REQUEST.getCode(), "IP 组必填");
        }
        if (CONTENT_TYPE_ARTICLE.equals(content.getContentType())) {
            if (StrUtil.isBlank(content.getDocumentType())) {
                throw new ServiceException(OaErrorCodes.BAD_REQUEST.getCode(), "文档类型必填");
            }
            if (StrUtil.isBlank(content.getBody())) {
                throw new ServiceException(OaErrorCodes.BAD_REQUEST.getCode(), "文档正文必填");
            }
        }
        if (CONTENT_TYPE_SHORT_VIDEO.equals(content.getContentType())) {
            if (StrUtil.isBlank(content.getFinalVideoUrl()) && StrUtil.isBlank(content.getGeneratedVideoUrl())) {
                throw new ServiceException(OaErrorCodes.BAD_REQUEST.getCode(), "须先生成或上传视频");
            }
        }
    }

    private String resolvePromptScene(ProductionContentDO content) {
        if (CONTENT_TYPE_SHORT_VIDEO.equals(content.getContentType())) {
            return "SHORT_VIDEO";
        }
        if (StrUtil.isNotBlank(content.getDocumentType())) {
            if (DOC_TYPE_SHORT_VIDEO_SCRIPT.equals(content.getDocumentType())) {
                return "SHORT_VIDEO";
            }
            return "CONTENT_GEN";
        }
        return "CONTENT_GEN";
    }

    private AiPromptConfigDO findPromptConfig(Long tenantId, String scene, ProductionContentDO content) {
        LambdaQueryWrapper<AiPromptConfigDO> wrapper = new LambdaQueryWrapper<AiPromptConfigDO>()
                .eq(AiPromptConfigDO::getTenantId, tenantId)
                .eq(AiPromptConfigDO::getScene, scene)
                .eq(AiPromptConfigDO::getStatus, "ENABLED");
        if (StrUtil.isNotBlank(content.getContentType())) {
            wrapper.and(w -> w.eq(AiPromptConfigDO::getContentType, content.getContentType())
                    .or().isNull(AiPromptConfigDO::getContentType));
        }
        if (StrUtil.isNotBlank(content.getDocumentType())) {
            wrapper.and(w -> w.eq(AiPromptConfigDO::getDocumentType, content.getDocumentType())
                    .or().isNull(AiPromptConfigDO::getDocumentType));
        }
        wrapper.orderByDesc(AiPromptConfigDO::getId).last("LIMIT 1");
        return aiPromptConfigMapper.selectOne(wrapper);
    }

    private String buildMockArticleBody(ProductionContentDO content, AiPromptConfigDO prompt) {
        String competition = StrUtil.blankToDefault(content.getCompetitionId(), "未知赛事");
        String docType = StrUtil.blankToDefault(content.getDocumentType(), "文档");
        String template = prompt != null ? prompt.getPromptContent() : "请撰写赛事相关文档。";
        return "【AI 占位文案 · " + docType + "】\n"
                + "赛事：" + competition + "\n"
                + "提示词摘要：" + StrUtil.sub(template, 0, 120) + "...\n"
                + "（BLK-M2-005：完整 AI pipeline 待 M8 对接）";
    }

    private List<Long> normalizeAccountIds(List<Long> accountIds) {
        if (accountIds == null || accountIds.isEmpty()) {
            return Collections.emptyList();
        }
        Set<Long> unique = new LinkedHashSet<>();
        for (Long id : accountIds) {
            if (id != null) {
                unique.add(id);
            }
        }
        return new ArrayList<>(unique);
    }

    private List<String> resolvePlatformTypes(List<String> platformTypes, String platformType,
                                              List<Long> accountIds, Long tenantId) {
        Set<String> resolved = new LinkedHashSet<>();
        if (platformTypes != null) {
            platformTypes.stream().filter(StrUtil::isNotBlank).forEach(resolved::add);
        }
        if (StrUtil.isNotBlank(platformType)) {
            resolved.add(platformType);
        }
        if (resolved.isEmpty() && !accountIds.isEmpty()) {
            for (Long accountId : accountIds) {
                AccountDO account = accountMapper.selectById(accountId);
                if (account != null && StrUtil.isNotBlank(account.getPlatformType())) {
                    resolved.add(account.getPlatformType());
                }
            }
        }
        return new ArrayList<>(resolved);
    }

    private void validateAccounts(Long tenantId, List<Long> accountIds, List<String> platformTypes) {
        for (Long accountId : accountIds) {
            AccountDO account = accountMapper.selectById(accountId);
            if (account == null || !Objects.equals(account.getTenantId(), tenantId)) {
                throw new ServiceException(OaErrorCodes.ENTITY_NOT_EXISTS);
            }
            if (!"NORMAL".equals(account.getStatus())) {
                throw new ServiceException(OaErrorCodes.ENTITY_DISABLED);
            }
            if (!platformTypes.isEmpty() && !platformTypes.contains(account.getPlatformType())) {
                throw new ServiceException(OaErrorCodes.CONTENT_PLATFORM_MISMATCH);
            }
        }
    }

    @Override
    public List<ContentAiPromptOptionVO> listAiPromptOptions(String contentType, String documentType) {
        Long tenantId = requireTenantId();
        if (StrUtil.isBlank(contentType)) {
            throw new ServiceException(OaErrorCodes.BAD_REQUEST);
        }
        LambdaQueryWrapper<AiPromptConfigDO> wrapper = new LambdaQueryWrapper<AiPromptConfigDO>()
                .eq(AiPromptConfigDO::getTenantId, tenantId)
                .eq(AiPromptConfigDO::getStatus, "ENABLED")
                .and(w -> w.eq(AiPromptConfigDO::getContentType, contentType)
                        .or().isNull(AiPromptConfigDO::getContentType));
        if (StrUtil.isNotBlank(documentType)) {
            wrapper.and(w -> w.eq(AiPromptConfigDO::getDocumentType, documentType)
                    .or().isNull(AiPromptConfigDO::getDocumentType));
        }
        wrapper.orderByDesc(AiPromptConfigDO::getId);
        return aiPromptConfigMapper.selectList(wrapper).stream().map(prompt -> {
            ContentAiPromptOptionVO vo = new ContentAiPromptOptionVO();
            vo.setId(prompt.getId());
            vo.setTemplateName(prompt.getTemplateName());
            vo.setScene(prompt.getScene());
            vo.setContentType(prompt.getContentType());
            vo.setDocumentType(prompt.getDocumentType());
            return vo;
        }).collect(Collectors.toList());
    }

    @Override
    public ContentReviewConfigVO getReviewConfig() {
        return contentReviewConfigService.getConfig();
    }

    @Override
    @Transactional
    @AuditLog(module = "M2-content", action = "transfer-knowledge")
    public ContentTransferKnowledgeResultVO transferToKnowledge(Long id) {
        ProductionContentDO content = requireContent(id);
        if (!"PUBLISHED".equals(content.getStatus())) {
            throw new ServiceException(OaErrorCodes.CONTENT_STATUS_INVALID);
        }
        if (Integer.valueOf(1).equals(content.getTransferredToKnowledge())) {
            throw new ServiceException(OaErrorCodes.CONTENT_ALREADY_TRANSFERRED);
        }

        Long tenantId = requireTenantId();
        KnowledgeBaseDO knowledge = new KnowledgeBaseDO();
        knowledge.setTenantId(tenantId);
        knowledge.setTitle(truncateKnowledgeTitle(content.getTitle()));
        knowledge.setContent(resolveKnowledgeContent(content));
        knowledge.setCategory(KNOWLEDGE_CATEGORY_CASE);
        knowledge.setTags(truncateKnowledgeTags(buildKnowledgeTags(content)));
        knowledge.setIsPublic(1);
        knowledge.setStatus(1);
        knowledge.setCreator(TenantContextHolder.getUsername());
        knowledge.setUpdater(TenantContextHolder.getUsername());
        knowledge.setCreateTime(LocalDateTime.now());
        knowledge.setUpdateTime(LocalDateTime.now());
        knowledgeBaseMapper.insert(knowledge);

        content.setTransferredToKnowledge(1);
        content.setKnowledgeId(knowledge.getId());
        content.setUpdater(TenantContextHolder.getUsername());
        content.setUpdateTime(LocalDateTime.now());
        productionContentMapper.updateById(content);

        ContentTransferKnowledgeResultVO result = new ContentTransferKnowledgeResultVO();
        result.setContentId(content.getId());
        result.setKnowledgeId(knowledge.getId());
        return result;
    }

    @Override
    @Transactional
    @AuditLog(module = "M2-content", action = "ai-generate")
    public ContentAiGenerateResultVO aiGenerate(ContentAiGenerateReq req) {
        Long tenantId = requireTenantId();
        AiModelConfigDO model = aiModelConfigMapper.selectById(req.getModelId());
        if (model == null || !Objects.equals(model.getTenantId(), tenantId)) {
            throw new ServiceException(OaErrorCodes.ENTITY_NOT_EXISTS);
        }
        if (!"ENABLED".equals(model.getStatus())) {
            throw new ServiceException(OaErrorCodes.ENTITY_DISABLED);
        }
        AiPromptConfigDO prompt = aiPromptConfigMapper.selectById(req.getPromptId());
        if (prompt == null || !Objects.equals(prompt.getTenantId(), tenantId)) {
            throw new ServiceException(OaErrorCodes.ENTITY_NOT_EXISTS);
        }
        if (!"ENABLED".equals(prompt.getStatus())) {
            throw new ServiceException(OaErrorCodes.ENTITY_DISABLED);
        }
        assertPromptMatches(req.getContentType(), req.getDocumentType(), prompt);
        String eventInfo = resolveEventInfo(req.getCompetitionId(), req.getCompetitionName(), req.getTaskId(), tenantId);
        String filledPrompt = fillPromptPlaceholders(prompt.getPromptContent(), eventInfo);
        ContentAiGenerateResultVO result = new ContentAiGenerateResultVO();
        result.setEventInfo(eventInfo);
        result.setTitle(StrUtil.blankToDefault(req.getContentType(), "AI 内容"));
        if ("CONNECTED".equals(model.getConnStatus()) && StrUtil.isNotBlank(model.getApiEndpoint())) {
            result.setMock(false);
            result.setContent(generateViaModelEndpoint(model, filledPrompt));
            result.setMessage("AI 生成完成（模型 " + model.getModelName() + "）");
        } else {
            result.setMock(true);
            result.setContent(buildMockAiContent(filledPrompt, eventInfo, prompt));
            result.setMessage("AI 占位生成（模型未连通，BLK-M2-005 集成点）");
        }
        return result;
    }

    private void assertPromptMatches(String contentType, String documentType, AiPromptConfigDO prompt) {
        if (StrUtil.isNotBlank(contentType) && StrUtil.isNotBlank(prompt.getContentType())
                && !contentType.equals(prompt.getContentType())) {
            throw new ServiceException(OaErrorCodes.BAD_REQUEST.getCode(), "提示词与内容类型不匹配");
        }
        if (StrUtil.isNotBlank(documentType) && StrUtil.isNotBlank(prompt.getDocumentType())
                && !documentType.equals(prompt.getDocumentType())) {
            throw new ServiceException(OaErrorCodes.BAD_REQUEST.getCode(), "提示词与文档类型不匹配");
        }
    }

    private String resolveEventInfo(String competitionId, String competitionName, Long taskId, Long tenantId) {
        if (StrUtil.isNotBlank(competitionId)) {
            if (StrUtil.isNotBlank(competitionName)) {
                return competitionName;
            }
            return resolveCompetitionDisplayName(competitionId, taskId, tenantId);
        }
        if (taskId != null) {
            TaskDO task = requireTaskInTenant(taskId, tenantId);
            String name = resolveCompetitionName(task);
            if (StrUtil.isNotBlank(name)) {
                return name;
            }
            return StrUtil.blankToDefault(task.getCompetitionId(), "");
        }
        return "";
    }

    private String resolveCompetitionDisplayName(String competitionId, Long taskId, Long tenantId) {
        if (taskId != null) {
            TaskDO task = requireTaskInTenant(taskId, tenantId);
            if (competitionId.equals(task.getCompetitionId())) {
                String name = resolveCompetitionName(task);
                if (StrUtil.isNotBlank(name)) {
                    return name;
                }
            }
        }
        return competitionId;
    }

    private void assertCompetitionSelection(String competitionId, String competitionName) {
        if (StrUtil.isBlank(competitionId)) {
            throw new ServiceException(OaErrorCodes.BAD_REQUEST.getCode(), "请选择赛事");
        }
        if (StrUtil.isBlank(competitionName)) {
            throw new ServiceException(OaErrorCodes.BAD_REQUEST.getCode(), "赛事展示名不能为空");
        }
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

    private String fillPromptPlaceholders(String template, String eventInfo) {
        String text = StrUtil.blankToDefault(template, "");
        return text.replace("{eventinfo}", StrUtil.blankToDefault(eventInfo, ""))
                .replace("{competitionName}", StrUtil.blankToDefault(eventInfo, ""));
    }

    private String buildMockAiContent(String filledPrompt, String eventInfo, AiPromptConfigDO prompt) {
        return "【AI 生成占位 · " + StrUtil.blankToDefault(prompt != null ? prompt.getTemplateName() : null, "提示词") + "】\n"
                + "赛事信息：" + StrUtil.blankToDefault(eventInfo, "（无）") + "\n\n"
                + filledPrompt + "\n\n"
                + "（BLK-M2-005：外部模型连通后将返回真实生成结果）";
    }

    private String generateViaModelEndpoint(AiModelConfigDO model, String filledPrompt) {
        String endpoint = resolveChatCompletionsUrl(model.getApiEndpoint());
        int timeoutMs = (model.getTimeout() == null ? 120 : model.getTimeout()) * 1000;
        String apiKey = StrUtil.isNotBlank(model.getApiKeyEncrypted())
                ? aesUtil.decrypt(model.getApiKeyEncrypted()) : "";

        JSONObject body = new JSONObject();
        body.set("model", StrUtil.blankToDefault(model.getModelId(), model.getModelName()));
        body.set("max_tokens", model.getMaxTokens() == null ? 2048 : model.getMaxTokens());
        if (model.getTemperature() != null) {
            body.set("temperature", model.getTemperature());
        }
        if (model.getTopP() != null) {
            body.set("top_p", model.getTopP());
        }
        JSONArray messages = new JSONArray();
        JSONObject userMsg = new JSONObject();
        userMsg.set("role", "user");
        userMsg.set("content", filledPrompt);
        messages.add(userMsg);
        body.set("messages", messages);

        log.info("AI generate calling model endpoint: model={}, url={}", model.getModelName(), endpoint);
        HttpRequest request = HttpRequest.post(endpoint)
                .header("Content-Type", "application/json")
                .timeout(timeoutMs)
                .body(body.toString());
        if (StrUtil.isNotBlank(apiKey)) {
            request.header("Authorization", "Bearer " + apiKey);
        }

        HttpResponse response = request.execute();
        if (!response.isOk()) {
            log.warn("AI model HTTP {}: {}", response.getStatus(), response.body());
            throw new ServiceException(OaErrorCodes.BAD_REQUEST.getCode(),
                    "AI 模型调用失败（HTTP " + response.getStatus() + "）");
        }

        JSONObject respJson = JSONUtil.parseObj(response.body());
        JSONObject error = respJson.getJSONObject("error");
        if (error != null) {
            throw new ServiceException(OaErrorCodes.BAD_REQUEST.getCode(),
                    "AI 模型返回错误：" + StrUtil.blankToDefault(error.getStr("message"), error.toString()));
        }
        JSONArray choices = respJson.getJSONArray("choices");
        if (choices == null || choices.isEmpty()) {
            throw new ServiceException(OaErrorCodes.BAD_REQUEST.getCode(), "AI 模型未返回有效内容");
        }
        JSONObject message = choices.getJSONObject(0).getJSONObject("message");
        String content = message != null ? message.getStr("content") : null;
        if (StrUtil.isBlank(content)) {
            content = choices.getJSONObject(0).getStr("text");
        }
        if (StrUtil.isBlank(content)) {
            throw new ServiceException(OaErrorCodes.BAD_REQUEST.getCode(), "AI 模型返回空正文");
        }
        return content.trim();
    }

    private String resolveChatCompletionsUrl(String apiEndpoint) {
        String url = StrUtil.removeSuffix(StrUtil.trim(apiEndpoint), "/");
        if (url.endsWith("/chat/completions")) {
            return url;
        }
        if (url.endsWith("/v1")) {
            return url + "/chat/completions";
        }
        return url + "/v1/chat/completions";
    }

    private void alignContentWithTask(ProductionContentDO content) {
        if (content.getTaskId() == null) {
            return;
        }
        TaskDO task = requireTaskInTenant(content.getTaskId(), content.getTenantId());
        if (task.getIpGroupId() == null) {
            return;
        }
        boolean changed = false;
        if (!Objects.equals(content.getIpGroupId(), task.getIpGroupId())) {
            content.setIpGroupId(task.getIpGroupId());
            changed = true;
        }
        if (content.getAuthorId() != null) {
            AuthorDO author = authorMapper.selectById(content.getAuthorId());
            if (author == null || !Objects.equals(author.getIpGroupId(), task.getIpGroupId())) {
                content.setAuthorId(null);
                changed = true;
            }
        }
        if (content.getAuthorId() == null) {
            AuthorDO defaultAuthor = findFirstAuthor(task.getIpGroupId(), content.getTenantId());
            if (defaultAuthor != null) {
                content.setAuthorId(defaultAuthor.getId());
                changed = true;
            }
        }
        if (changed) {
            content.setUpdater(TenantContextHolder.getUsername());
            content.setUpdateTime(LocalDateTime.now());
            productionContentMapper.updateById(content);
        }
    }

    private void assertIpGroupInTenant(Long ipGroupId, Long tenantId) {
        IpGroupDO ipGroup = ipGroupMapper.selectById(ipGroupId);
        if (ipGroup == null || !Objects.equals(ipGroup.getTenantId(), tenantId)) {
            throw new ServiceException(OaErrorCodes.ENTITY_NOT_EXISTS);
        }
        if (ipGroup.getStatus() != 1) {
            throw new ServiceException(OaErrorCodes.ENTITY_DISABLED);
        }
    }

    private void assertUserMemberOfIpGroup(Long userId, Long ipGroupId, Long tenantId) {
        if (userId == null) {
            throw new ServiceException(OaErrorCodes.UNAUTHORIZED);
        }
        Long count = ipGroupMemberMapper.selectCount(new LambdaQueryWrapper<IpGroupMemberDO>()
                .eq(IpGroupMemberDO::getTenantId, tenantId)
                .eq(IpGroupMemberDO::getIpGroupId, ipGroupId)
                .eq(IpGroupMemberDO::getUserId, userId));
        if (count == null || count == 0) {
            throw new ServiceException(OaErrorCodes.FORBIDDEN.getCode(), "当前用户不属于所选 IP 组");
        }
    }

    private void assertAuthorInIpGroup(Long authorId, Long ipGroupId, Long tenantId) {
        AuthorDO author = authorMapper.selectById(authorId);
        if (author == null || !Objects.equals(author.getTenantId(), tenantId)) {
            throw new ServiceException(OaErrorCodes.ENTITY_NOT_EXISTS);
        }
        if (author.getStatus() != 1) {
            throw new ServiceException(OaErrorCodes.ENTITY_DISABLED);
        }
        if (ipGroupId != null && !Objects.equals(author.getIpGroupId(), ipGroupId)) {
            throw new ServiceException(OaErrorCodes.BAD_REQUEST.getCode(), "作者不属于所选 IP 组");
        }
    }

    private AuthorDO findFirstAuthor(Long ipGroupId, Long tenantId) {
        return authorMapper.selectOne(
                new LambdaQueryWrapper<AuthorDO>()
                        .eq(AuthorDO::getTenantId, tenantId)
                        .eq(AuthorDO::getIpGroupId, ipGroupId)
                        .eq(AuthorDO::getStatus, 1)
                        .orderByAsc(AuthorDO::getId)
                        .last("LIMIT 1"));
    }

    private void assertTaskContentCreatable(Long taskId, Long tenantId) {
        requireTaskInTenant(taskId, tenantId);
        Long count = productionContentMapper.selectCount(
                new LambdaQueryWrapper<ProductionContentDO>()
                        .eq(ProductionContentDO::getTenantId, tenantId)
                        .eq(ProductionContentDO::getTaskId, taskId));
        if (count != null && count > 0) {
            throw new ServiceException(OaErrorCodes.ENTITY_ALREADY_BOUND);
        }
    }

    private TaskDO requireTaskInTenant(Long taskId, Long tenantId) {
        TaskDO task = taskMapper.selectById(taskId);
        if (task == null) {
            throw new ServiceException(OaErrorCodes.ENTITY_NOT_EXISTS);
        }
        if (!Objects.equals(task.getTenantId(), tenantId)) {
            throw new ServiceException(OaErrorCodes.TENANT_FORBIDDEN);
        }
        return task;
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

    /** DRAFT/REJECTED always editable; task-linked COMPLETED may re-enter review after edit. */
    private boolean isEditableStatus(ProductionContentDO content) {
        String status = content.getStatus();
        if ("DRAFT".equals(status) || "REJECTED".equals(status)) {
            return true;
        }
        return "COMPLETED".equals(status) && content.getTaskId() != null;
    }

    private boolean shouldResetToDraftOnEdit(ProductionContentDO content) {
        return "REJECTED".equals(content.getStatus())
                || ("COMPLETED".equals(content.getStatus()) && content.getTaskId() != null);
    }

    private boolean isSubmittableStatus(String status) {
        return "DRAFT".equals(status) || "REJECTED".equals(status);
    }

    private void applyLayoutFields(ProductionContentDO entity, String bodyFormat, Object layoutJson,
                                   String layoutHtml, Long layoutTemplateId) {
        if (StrUtil.isNotBlank(bodyFormat)) {
            entity.setBodyFormat(bodyFormat);
        } else if (layoutJson != null) {
            entity.setBodyFormat("LAYOUT");
        } else if (entity.getBodyFormat() == null) {
            entity.setBodyFormat("PLAIN");
        }
        if (layoutJson != null) {
            String json = LayoutJsonHelper.toJsonString(layoutJson);
            entity.setLayoutJson(json);
            entity.setLayoutHtml(StrUtil.blankToDefault(layoutHtml, LayoutJsonHelper.renderHtml(layoutJson)));
            entity.setBodyFormat("LAYOUT");
        } else if (layoutHtml != null) {
            entity.setLayoutHtml(LayoutJsonHelper.sanitizeHtml(layoutHtml));
        }
        if (layoutTemplateId != null) {
            entity.setLayoutTemplateId(layoutTemplateId);
        }
        if ("PLAIN".equals(entity.getBodyFormat()) && layoutJson == null) {
            entity.setLayoutJson(null);
            entity.setLayoutHtml(null);
        }
    }
}
