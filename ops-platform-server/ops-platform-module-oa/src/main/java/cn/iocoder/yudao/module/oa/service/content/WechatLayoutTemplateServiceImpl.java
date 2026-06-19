package cn.iocoder.yudao.module.oa.service.content;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import cn.iocoder.yudao.framework.common.exception.OaErrorCodes;
import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.oa.api.dto.content.ContentApplyLayoutTemplateReq;
import cn.iocoder.yudao.module.oa.api.dto.content.LayoutImportJobCreateResp;
import cn.iocoder.yudao.module.oa.api.dto.content.LayoutImportJobVO;
import cn.iocoder.yudao.module.oa.api.dto.content.LayoutMergePreviewReq;
import cn.iocoder.yudao.module.oa.api.dto.content.LayoutMergePreviewVO;
import cn.iocoder.yudao.module.oa.api.dto.content.LayoutImportPasteReq;
import cn.iocoder.yudao.module.oa.api.dto.content.LayoutImportUrlReq;
import cn.iocoder.yudao.module.oa.api.dto.content.LayoutTemplateCreateReq;
import cn.iocoder.yudao.module.oa.api.dto.content.LayoutTemplateDetailVO;
import cn.iocoder.yudao.module.oa.api.dto.content.LayoutTemplateSelectVO;
import cn.iocoder.yudao.module.oa.api.dto.content.LayoutTemplateUpdateReq;
import cn.iocoder.yudao.module.oa.api.dto.content.LayoutTemplateVO;
import cn.iocoder.yudao.module.oa.api.dto.content.ProductionContentVO;
import cn.iocoder.yudao.module.oa.dal.dataobject.auth.SysUserDO;
import cn.iocoder.yudao.module.oa.dal.dataobject.content.LayoutImportJobDO;
import cn.iocoder.yudao.module.oa.dal.dataobject.content.ProductionContentDO;
import cn.iocoder.yudao.module.oa.dal.dataobject.content.WechatLayoutTemplateDO;
import cn.iocoder.yudao.module.oa.dal.mysql.auth.SysUserMapper;
import cn.iocoder.yudao.module.oa.dal.mysql.content.LayoutImportJobMapper;
import cn.iocoder.yudao.module.oa.dal.mysql.content.ProductionContentMapper;
import cn.iocoder.yudao.module.oa.dal.mysql.content.WechatLayoutTemplateMapper;
import cn.iocoder.yudao.module.oa.framework.audit.AuditLog;
import cn.iocoder.yudao.module.oa.util.DocxToHtmlConverter;
import cn.iocoder.yudao.module.oa.util.LayoutImportExtractor;
import cn.iocoder.yudao.module.oa.util.LayoutJsonHelper;
import cn.iocoder.yudao.module.oa.util.LayoutSchemaHelper;
import cn.iocoder.yudao.module.oa.util.WechatArticleHtmlFetcher;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class WechatLayoutTemplateServiceImpl implements WechatLayoutTemplateService {

    private static final String CONTENT_TYPE_ARTICLE = "ARTICLE";
    private static final String BODY_FORMAT_LAYOUT = "LAYOUT";
    private static final String GENERAL_FILTER = "__GENERAL__";
    private static final String SOURCE_PRESET = "PRESET";
    private static final long MAX_DOCX_BYTES = 10L * 1024 * 1024;
    private static final long MAX_MHTML_BYTES = 20L * 1024 * 1024;

    private final WechatLayoutTemplateMapper templateMapper;
    private final LayoutImportJobMapper importJobMapper;
    private final ProductionContentMapper productionContentMapper;
    private final ProductionContentService productionContentService;
    private final SysUserMapper sysUserMapper;
    private final LayoutMergeService layoutMergeService;

    @Override
    public PageResult<LayoutTemplateVO> list(String templateName, String documentType, String status,
                                             String sourceType, Integer pageNum, Integer pageSize) {
        Long tenantId = requireTenantId();
        LambdaQueryWrapper<WechatLayoutTemplateDO> wrapper = new LambdaQueryWrapper<WechatLayoutTemplateDO>()
                .eq(WechatLayoutTemplateDO::getTenantId, tenantId)
                .like(StrUtil.isNotBlank(templateName), WechatLayoutTemplateDO::getTemplateName, templateName)
                .eq(StrUtil.isNotBlank(status), WechatLayoutTemplateDO::getStatus, status)
                .eq(StrUtil.isNotBlank(sourceType), WechatLayoutTemplateDO::getSourceType, sourceType)
                .orderByDesc(WechatLayoutTemplateDO::getId);
        if (GENERAL_FILTER.equals(documentType)) {
            wrapper.isNull(WechatLayoutTemplateDO::getDocumentType);
        } else if (StrUtil.isNotBlank(documentType)) {
            wrapper.eq(WechatLayoutTemplateDO::getDocumentType, documentType);
        }
        Page<WechatLayoutTemplateDO> page = templateMapper.selectPage(
                new Page<>(pageNum == null ? 1 : pageNum, pageSize == null ? 20 : pageSize), wrapper);
        return new PageResult<>(page.getRecords().stream().map(this::toVO).collect(Collectors.toList()), page.getTotal());
    }

    @Override
    public LayoutTemplateDetailVO getById(Long id) {
        return toDetailVO(requireTemplate(id));
    }

    @Override
    public List<LayoutTemplateSelectVO> selectList(String contentType, String documentType) {
        if (!CONTENT_TYPE_ARTICLE.equals(contentType)) {
            return List.of();
        }
        Long tenantId = requireTenantId();
        List<WechatLayoutTemplateDO> list = templateMapper.selectList(
                new LambdaQueryWrapper<WechatLayoutTemplateDO>()
                        .eq(WechatLayoutTemplateDO::getTenantId, tenantId)
                        .eq(WechatLayoutTemplateDO::getContentType, CONTENT_TYPE_ARTICLE)
                        .eq(WechatLayoutTemplateDO::getStatus, "ENABLED")
                        .orderByDesc(WechatLayoutTemplateDO::getId));
        return list.stream()
                .filter(t -> matchesDocumentType(t.getDocumentType(), documentType))
                .map(t -> {
                    LayoutTemplateSelectVO vo = new LayoutTemplateSelectVO();
                    vo.setId(t.getId());
                    vo.setTemplateName(t.getTemplateName());
                    vo.setDocumentType(t.getDocumentType());
                    vo.setThumbnailUrl(t.getThumbnailUrl());
                    return vo;
                })
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    @AuditLog(module = "M2-layout-template", action = "create")
    public Long create(LayoutTemplateCreateReq req) {
        Long tenantId = requireTenantId();
        JSONObject schema = resolveRequestSchema(req.getLayoutSchema(), req.getLayoutJson());
        String schemaJson = LayoutSchemaHelper.toJsonString(schema);
        String previewHtml = resolveVisualPreviewHtml(req.getPreviewHtml(), schema);
        String layoutHtml = StrUtil.isNotBlank(req.getLayoutHtml())
                ? LayoutJsonHelper.sanitizeHtml(req.getLayoutHtml())
                : previewHtml;
        WechatLayoutTemplateDO entity = new WechatLayoutTemplateDO();
        entity.setTenantId(tenantId);
        entity.setTemplateName(req.getTemplateName().trim());
        entity.setDescription(req.getDescription());
        entity.setContentType(CONTENT_TYPE_ARTICLE);
        entity.setDocumentType(req.getDocumentType());
        entity.setLayoutSchema(schemaJson);
        entity.setSchemaVersion(2);
        entity.setLayoutJson(LayoutJsonHelper.toJsonString(LayoutJsonHelper.emptyDocument()));
        entity.setLayoutHtml(layoutHtml);
        entity.setPreviewHtml(previewHtml);
        if (StrUtil.isNotBlank(req.getStyleCss())) {
            entity.setStyleCss(req.getStyleCss().trim());
        }
        entity.setSourceType(StrUtil.blankToDefault(req.getSourceType(), "MANUAL"));
        entity.setSourceUrl(req.getSourceUrl());
        entity.setStatus(req.getStatus());
        entity.setCreatorUserId(TenantContextHolder.getUserId());
        entity.setCreator(TenantContextHolder.getUsername());
        entity.setUpdater(TenantContextHolder.getUsername());
        entity.setCreateTime(LocalDateTime.now());
        entity.setUpdateTime(LocalDateTime.now());
        templateMapper.insert(entity);
        return entity.getId();
    }

    @Override
    @Transactional
    @AuditLog(module = "M2-layout-template", action = "update")
    public void update(LayoutTemplateUpdateReq req) {
        WechatLayoutTemplateDO existing = requireTemplate(req.getId());
        assertNotPreset(existing);
        assertCanEditTemplate(existing);
        if (StrUtil.isNotBlank(req.getTemplateName())) {
            existing.setTemplateName(req.getTemplateName().trim());
        }
        if (req.getDescription() != null) {
            existing.setDescription(req.getDescription());
        }
        existing.setDocumentType(req.getDocumentType());
        if (req.getLayoutSchema() != null || req.getLayoutJson() != null) {
            JSONObject schema = resolveRequestSchema(req.getLayoutSchema(), req.getLayoutJson());
            String previewHtml = LayoutSchemaHelper.renderSchemaPreview(schema);
            existing.setLayoutSchema(LayoutSchemaHelper.toJsonString(schema));
            existing.setSchemaVersion(2);
            existing.setPreviewHtml(previewHtml);
            existing.setLayoutHtml(previewHtml);
        }
        if (StrUtil.isNotBlank(req.getStatus())) {
            existing.setStatus(req.getStatus());
        }
        existing.setUpdater(TenantContextHolder.getUsername());
        existing.setUpdateTime(LocalDateTime.now());
        templateMapper.updateById(existing);
    }

    @Override
    @Transactional
    @AuditLog(module = "M2-layout-template", action = "delete")
    public void delete(Long id) {
        WechatLayoutTemplateDO existing = requireTemplate(id);
        assertCanEditTemplate(existing);
        assertNotPreset(existing);
        templateMapper.deleteById(id);
    }

    @Override
    @Transactional
    @AuditLog(module = "M2-layout-template", action = "publish")
    public void publish(Long id) {
        WechatLayoutTemplateDO existing = requireTemplate(id);
        assertCanEditTemplate(existing);
        if (!"DRAFT".equals(existing.getStatus())) {
            throw new ServiceException(OaErrorCodes.TASK_STATUS_INVALID);
        }
        existing.setStatus("ENABLED");
        existing.setUpdater(TenantContextHolder.getUsername());
        existing.setUpdateTime(LocalDateTime.now());
        templateMapper.updateById(existing);
    }

    @Override
    @Transactional
    @AuditLog(module = "M2-layout-template", action = "disable")
    public void disable(Long id) {
        WechatLayoutTemplateDO existing = requireTemplate(id);
        assertCanEditTemplate(existing);
        if (!"ENABLED".equals(existing.getStatus())) {
            throw new ServiceException(OaErrorCodes.TASK_STATUS_INVALID);
        }
        existing.setStatus("DISABLED");
        existing.setUpdater(TenantContextHolder.getUsername());
        existing.setUpdateTime(LocalDateTime.now());
        templateMapper.updateById(existing);
    }

    @Override
    @Transactional
    @AuditLog(module = "M2-layout-template", action = "enable")
    public void enable(Long id) {
        WechatLayoutTemplateDO existing = requireTemplate(id);
        assertCanEditTemplate(existing);
        if (!"DISABLED".equals(existing.getStatus())) {
            throw new ServiceException(OaErrorCodes.TASK_STATUS_INVALID);
        }
        existing.setStatus("ENABLED");
        existing.setUpdater(TenantContextHolder.getUsername());
        existing.setUpdateTime(LocalDateTime.now());
        templateMapper.updateById(existing);
    }

    @Override
    @Transactional
    public LayoutImportJobCreateResp importUrl(LayoutImportUrlReq req) {
        Long tenantId = requireTenantId();
        LayoutImportJobDO job = newJob(tenantId, "URL", req.getSourceUrl());
        importJobMapper.insert(job);
        try {
            job.setStatus("RUNNING");
            importJobMapper.updateById(job);
            String html = WechatArticleHtmlFetcher.fetchFromUrl(req.getSourceUrl());
            applyExtractResultToJob(job, LayoutImportExtractor.extract(html));
            job.setSuggestedName(StrUtil.blankToDefault(req.getTemplateName(), suggestNameFromUrl(req.getSourceUrl())));
            job.setStatus("SUCCESS");
            job.setUpdateTime(LocalDateTime.now());
            importJobMapper.updateById(job);
        } catch (Exception ex) {
            log.warn("layout import-url failed: {}", ex.getMessage());
            job.setStatus("FAILED");
            job.setErrorMessage(StrUtil.sub(ex.getMessage(), 0, 480));
            job.setUpdateTime(LocalDateTime.now());
            importJobMapper.updateById(job);
        }
        LayoutImportJobCreateResp resp = new LayoutImportJobCreateResp();
        resp.setJobId(job.getId());
        resp.setStatus(job.getStatus());
        return resp;
    }

    @Override
    @Transactional
    public LayoutImportJobCreateResp importDocx(MultipartFile file, String templateName, String documentType) {
        if (file == null || file.isEmpty()) {
            throw new ServiceException(OaErrorCodes.BAD_REQUEST.getCode(), "上传文件不能为空");
        }
        if (file.getSize() > MAX_DOCX_BYTES) {
            throw new ServiceException(OaErrorCodes.BAD_REQUEST.getCode(), "Word 文件不能超过 10MB");
        }
        String original = file.getOriginalFilename();
        if (original == null || !original.toLowerCase().endsWith(".docx")) {
            throw new ServiceException(OaErrorCodes.BAD_REQUEST.getCode(), "仅支持 .docx 文件");
        }
        Long tenantId = requireTenantId();
        LayoutImportJobDO job = newJob(tenantId, "DOCX", null);
        importJobMapper.insert(job);
        try {
            job.setStatus("RUNNING");
            importJobMapper.updateById(job);
            String html = docxToHtml(file);
            applyExtractResultToJob(job, LayoutImportExtractor.extract(html));
            job.setSuggestedName(StrUtil.blankToDefault(templateName,
                    StrUtil.removeSuffix(original, ".docx")));
            job.setStatus("SUCCESS");
            job.setUpdateTime(LocalDateTime.now());
            importJobMapper.updateById(job);
        } catch (Exception ex) {
            log.warn("layout import-docx failed: {}", ex.getMessage());
            job.setStatus("FAILED");
            job.setErrorMessage(StrUtil.sub(ex.getMessage(), 0, 480));
            job.setUpdateTime(LocalDateTime.now());
            importJobMapper.updateById(job);
        }
        LayoutImportJobCreateResp resp = new LayoutImportJobCreateResp();
        resp.setJobId(job.getId());
        resp.setStatus(job.getStatus());
        return resp;
    }

    @Override
    @Transactional
    public LayoutImportJobCreateResp importMhtml(MultipartFile file, String templateName, String documentType) {
        if (file == null || file.isEmpty()) {
            throw new ServiceException(OaErrorCodes.BAD_REQUEST.getCode(), "上传文件不能为空");
        }
        if (file.getSize() > MAX_MHTML_BYTES) {
            throw new ServiceException(OaErrorCodes.BAD_REQUEST.getCode(), "MHTML 文件不能超过 20MB");
        }
        String original = file.getOriginalFilename();
        if (original == null || !original.toLowerCase().endsWith(".mhtml")) {
            throw new ServiceException(OaErrorCodes.BAD_REQUEST.getCode(), "仅支持 .mhtml 文件");
        }
        Long tenantId = requireTenantId();
        LayoutImportJobDO job = newJob(tenantId, "MHTML", null);
        importJobMapper.insert(job);
        try {
            job.setStatus("RUNNING");
            importJobMapper.updateById(job);
            byte[] bytes = file.getBytes();
            if (bytes.length < 50_000) {
                throw new ServiceException(OaErrorCodes.BAD_REQUEST.getCode(),
                        "MHTML 文件过小（" + bytes.length + " 字节），可能上传不完整，请重试");
            }
            String html = WechatArticleHtmlFetcher.parseMhtml(bytes);
            applyExtractResultToJob(job, LayoutImportExtractor.extract(html));
            job.setSuggestedName(StrUtil.blankToDefault(templateName,
                    StrUtil.removeSuffix(original, ".mhtml")));
            job.setStatus("SUCCESS");
            job.setUpdateTime(LocalDateTime.now());
            importJobMapper.updateById(job);
        } catch (Exception ex) {
            log.warn("layout import-mhtml failed: {}", ex.getMessage());
            job.setStatus("FAILED");
            job.setErrorMessage(StrUtil.sub(ex.getMessage(), 0, 480));
            job.setUpdateTime(LocalDateTime.now());
            importJobMapper.updateById(job);
        }
        LayoutImportJobCreateResp resp = new LayoutImportJobCreateResp();
        resp.setJobId(job.getId());
        resp.setStatus(job.getStatus());
        return resp;
    }

    @Override
    @Transactional
    @AuditLog(module = "M2-layout-template", action = "import-paste")
    public LayoutTemplateDetailVO importPaste(LayoutImportPasteReq req) {
        LayoutImportExtractor.ExtractResult extracted = LayoutImportExtractor.extract(req.getHtml());
        LayoutTemplateCreateReq createReq = new LayoutTemplateCreateReq();
        createReq.setTemplateName(req.getTemplateName().trim());
        createReq.setDocumentType(req.getDocumentType());
        createReq.setLayoutSchema(extracted.getLayoutSchema());
        createReq.setPreviewHtml(extracted.getPreviewHtml());
        createReq.setLayoutHtml(extracted.getLayoutHtml());
        createReq.setStyleCss(extracted.getStyleCss());
        createReq.setStatus("DRAFT");
        createReq.setDescription("粘贴 HTML 导入");
        createReq.setSourceType("PASTE");
        Long id = create(createReq);
        WechatLayoutTemplateDO saved = requireTemplate(id);
        saved.setSourceType("PASTE");
        templateMapper.updateById(saved);
        return toDetailVO(saved);
    }

    @Override
    @Transactional
    public LayoutImportJobCreateResp importPastePreview(LayoutImportPasteReq req) {
        if (StrUtil.isBlank(req.getHtml())) {
            throw new ServiceException(OaErrorCodes.BAD_REQUEST.getCode(), "HTML 内容不能为空");
        }
        Long tenantId = requireTenantId();
        LayoutImportJobDO job = newJob(tenantId, "PASTE", null);
        importJobMapper.insert(job);
        try {
            job.setStatus("RUNNING");
            importJobMapper.updateById(job);
            applyExtractResultToJob(job, LayoutImportExtractor.extract(req.getHtml()));
            job.setSuggestedName(StrUtil.blankToDefault(req.getTemplateName(), "粘贴导入"));
            job.setStatus("SUCCESS");
            job.setUpdateTime(LocalDateTime.now());
            importJobMapper.updateById(job);
        } catch (Exception ex) {
            log.warn("layout import-paste-preview failed: {}", ex.getMessage());
            job.setStatus("FAILED");
            job.setErrorMessage(StrUtil.sub(ex.getMessage(), 0, 480));
            job.setUpdateTime(LocalDateTime.now());
            importJobMapper.updateById(job);
        }
        LayoutImportJobCreateResp resp = new LayoutImportJobCreateResp();
        resp.setJobId(job.getId());
        resp.setStatus(job.getStatus());
        return resp;
    }

    @Override
    public LayoutImportJobVO getImportJob(Long jobId) {
        LayoutImportJobDO job = requireImportJob(jobId);
        LayoutImportJobVO vo = new LayoutImportJobVO();
        vo.setId(job.getId());
        vo.setStatus(job.getStatus());
        vo.setSourceType(job.getSourceType());
        vo.setSourceUrl(job.getSourceUrl());
        vo.setSuggestedName(job.getSuggestedName());
        vo.setErrorMessage(job.getErrorMessage());
        if (StrUtil.isNotBlank(job.getPreviewLayoutSchema())) {
            vo.setPreviewLayoutSchema(JSONUtil.parse(job.getPreviewLayoutSchema()));
        }
        if (StrUtil.isNotBlank(job.getExtractionReport())) {
            JSONObject report = JSONUtil.parseObj(job.getExtractionReport());
            vo.setExtractionReport(report);
            vo.setPreviewHtml(report.getStr("previewHtml"));
            vo.setStyleCss(report.getStr("styleCss"));
        }
        if (StrUtil.isNotBlank(job.getPreviewLayoutJson())) {
            vo.setPreviewLayoutJson(JSONUtil.parse(job.getPreviewLayoutJson()));
        }
        return vo;
    }

    @Override
    public LayoutMergePreviewVO previewMerge(Long templateId, LayoutMergePreviewReq req) {
        return doMergePreview(templateId, req, req.getParamOverrides(), false);
    }

    @Override
    public LayoutMergePreviewVO partialApply(Long templateId, LayoutMergePreviewReq req) {
        List<String> types = req.getIncludeBlockTypes();
        if (types == null || types.isEmpty()) {
            types = List.of("heading", "slot", "divider", "fixed", "section");
        }
        req.setIncludeBlockTypes(types);
        return doMergePreview(templateId, req, req.getParamOverrides(), false);
    }

    @Override
    public LayoutMergePreviewVO applyBackground(Long templateId, LayoutMergePreviewReq req) {
        return doMergePreview(templateId, req, req.getParamOverrides(), true);
    }

    private LayoutMergePreviewVO doMergePreview(Long templateId, LayoutMergePreviewReq req,
                                                Object paramOverrides, boolean backgroundOnly) {
        WechatLayoutTemplateDO template = requireTemplate(templateId);
        JSONObject schema = resolveTemplateSchema(template);
        mergeDefaultParams(schema, template, paramOverrides != null ? paramOverrides : req.getParamOverrides());
        if (StrUtil.isBlank(req.getBody())) {
            throw new ServiceException(OaErrorCodes.LAYOUT_APPLY_BODY_EMPTY);
        }
        LayoutMergeService.MergeResult result = layoutMergeService.merge(
                req.getBody(), req.getExistingLayoutJson(), schema,
                paramOverrides != null ? paramOverrides : req.getParamOverrides(),
                req.getIncludeBlockTypes(), backgroundOnly);
        LayoutMergePreviewVO vo = new LayoutMergePreviewVO();
        vo.setLayoutJson(result.getLayoutJson());
        vo.setLayoutHtml(result.getLayoutHtml());
        vo.setOverflowSegmentCount(result.getOverflowSegmentCount());
        return vo;
    }

    private void mergeDefaultParams(JSONObject schema, WechatLayoutTemplateDO template, Object reqOverrides) {
        JSONObject effective = null;
        if (StrUtil.isNotBlank(template.getDefaultParams())) {
            JSONObject defaults = JSONUtil.parseObj(template.getDefaultParams());
            if (reqOverrides == null) {
                effective = defaults;
            } else {
                effective = JSONUtil.parseObj(JSONUtil.toJsonStr(reqOverrides));
                for (String key : defaults.keySet()) {
                    if (!effective.containsKey(key)) {
                        effective.set(key, defaults.get(key));
                    }
                }
            }
        } else if (reqOverrides != null) {
            effective = JSONUtil.parseObj(JSONUtil.toJsonStr(reqOverrides));
        }
        if (effective != null) {
            applyParamOverridesToSchema(schema, effective);
        }
    }

    private void applyParamOverridesToSchema(JSONObject schema, JSONObject overrides) {
        JSONObject globalStyles = schema.getJSONObject("globalStyles");
        if (globalStyles == null) {
            return;
        }
        for (String key : overrides.keySet()) {
            Object val = overrides.get(key);
            if (val instanceof JSONObject styleOverride) {
                JSONObject existing = globalStyles.getJSONObject(key);
                if (existing == null) {
                    globalStyles.set(key, styleOverride);
                } else {
                    existing.putAll(styleOverride);
                }
            }
        }
    }

    @Override
    public LayoutMergePreviewVO validateExtractFidelity(Object layoutSchema, String sampleBody) {
        JSONObject schema = JSONUtil.parseObj(JSONUtil.toJsonStr(layoutSchema));
        LayoutSchemaHelper.validateLayoutSchema(schema);
        String body = StrUtil.blankToDefault(sampleBody,
                "示例标题\n\n这是第一段正文，用于验证模板槽位与段落映射。\n\n这是第二段正文。");
        LayoutMergeService.MergeResult result = layoutMergeService.merge(body, null, schema);
        LayoutMergePreviewVO vo = new LayoutMergePreviewVO();
        vo.setLayoutJson(result.getLayoutJson());
        vo.setLayoutHtml(result.getLayoutHtml());
        vo.setOverflowSegmentCount(result.getOverflowSegmentCount());
        JSONObject fidelity = new JSONObject();
        fidelity.set("sampleBodySegmentCount", LayoutSchemaHelper.splitBody(body).size());
        fidelity.set("outputBlockCount", result.getLayoutJson().getJSONArray("blocks").size());
        fidelity.set("structurePreserved", result.getOverflowSegmentCount() == 0);
        List<String> notes = new ArrayList<>();
        if (result.getOverflowSegmentCount() > 0) {
            notes.add("样本正文有 " + result.getOverflowSegmentCount() + " 段落入默认段落样式（槽位不足）");
        } else {
            notes.add("样本正文段数与模板槽位匹配，结构还原良好");
        }
        notes.add("装饰区块与复杂嵌套 HTML 可能无法 100% 还原");
        fidelity.set("fidelityNotes", notes);
        vo.setExtractionReport(fidelity);
        return vo;
    }

    @Override
    @Transactional
    @AuditLog(module = "M2-layout-template", action = "copy")
    public Long copyTemplate(Long id) {
        WechatLayoutTemplateDO source = requireTemplate(id);
        JSONObject schema = resolveTemplateSchema(source);
        LayoutTemplateCreateReq req = new LayoutTemplateCreateReq();
        req.setTemplateName(source.getTemplateName() + "（副本）");
        req.setDescription(source.getDescription());
        req.setDocumentType(source.getDocumentType());
        req.setLayoutSchema(schema);
        req.setStatus("DRAFT");
        return create(req);
    }

    @Override
    @Transactional
    @AuditLog(module = "M2-content", action = "apply-layout-template")
    public ProductionContentVO applyLayoutTemplate(Long contentId, ContentApplyLayoutTemplateReq req) {
        ProductionContentDO content = requireContent(contentId);
        if (!CONTENT_TYPE_ARTICLE.equals(content.getContentType())) {
            throw new ServiceException(OaErrorCodes.LAYOUT_TEMPLATE_MISMATCH);
        }
        WechatLayoutTemplateDO template = requireTemplate(req.getLayoutTemplateId());
        if (!"ENABLED".equals(template.getStatus())) {
            throw new ServiceException(OaErrorCodes.ENTITY_DISABLED);
        }
        if (!matchesDocumentType(template.getDocumentType(), content.getDocumentType())) {
            throw new ServiceException(OaErrorCodes.LAYOUT_TEMPLATE_MISMATCH);
        }
        boolean overwrite = Boolean.TRUE.equals(req.getOverwrite());
        if (BODY_FORMAT_LAYOUT.equals(content.getBodyFormat()) && !overwrite) {
            throw new ServiceException(OaErrorCodes.LAYOUT_OVERWRITE_REQUIRED);
        }
        String body = resolveContentBody(content);
        if (StrUtil.isBlank(body)) {
            throw new ServiceException(OaErrorCodes.LAYOUT_APPLY_BODY_EMPTY);
        }
        JSONObject schema = resolveTemplateSchema(template);
        Object existingLayout = overwrite && BODY_FORMAT_LAYOUT.equals(content.getBodyFormat())
                ? (StrUtil.isNotBlank(content.getLayoutJson()) ? JSONUtil.parse(content.getLayoutJson()) : null)
                : null;
        LayoutMergeService.MergeResult merged = layoutMergeService.merge(body, existingLayout, schema);
        content.setBodyFormat(BODY_FORMAT_LAYOUT);
        content.setLayoutJson(JSONUtil.toJsonStr(merged.getLayoutJson()));
        content.setLayoutHtml(merged.getLayoutHtml());
        content.setLayoutTemplateId(template.getId());
        content.setUpdater(TenantContextHolder.getUsername());
        content.setUpdateTime(LocalDateTime.now());
        productionContentMapper.updateById(content);
        return productionContentService.getById(contentId);
    }

    @Override
    public boolean matchesDocumentType(String templateDocumentType, String contentDocumentType) {
        if (StrUtil.isBlank(templateDocumentType)) {
            return true;
        }
        return StrUtil.equals(templateDocumentType, contentDocumentType);
    }

    private void applyExtractResultToJob(LayoutImportJobDO job, LayoutImportExtractor.ExtractResult extracted) {
        JSONObject report = extracted.getExtractionReport();
        report.set("previewHtml", extracted.getPreviewHtml());
        if (StrUtil.isNotBlank(extracted.getStyleCss())) {
            report.set("styleCss", extracted.getStyleCss());
        }
        job.setPreviewLayoutSchema(LayoutSchemaHelper.toJsonString(extracted.getLayoutSchema()));
        job.setExtractionReport(JSONUtil.toJsonStr(report));
        job.setPreviewLayoutJson(null);
    }

    private String resolveVisualPreviewHtml(String requestPreviewHtml, JSONObject schema) {
        if (StrUtil.isNotBlank(requestPreviewHtml)) {
            return LayoutImportExtractor.sanitizeImportHtml(requestPreviewHtml);
        }
        return LayoutSchemaHelper.renderSchemaPreview(schema);
    }

    private LayoutImportJobDO newJob(Long tenantId, String sourceType, String sourceUrl) {
        LayoutImportJobDO job = new LayoutImportJobDO();
        job.setTenantId(tenantId);
        job.setSourceType(sourceType);
        job.setSourceUrl(sourceUrl);
        job.setStatus("PENDING");
        job.setCreatorUserId(TenantContextHolder.getUserId());
        job.setCreator(TenantContextHolder.getUsername());
        job.setUpdater(TenantContextHolder.getUsername());
        job.setCreateTime(LocalDateTime.now());
        job.setUpdateTime(LocalDateTime.now());
        return job;
    }

    private String docxToHtml(MultipartFile file) throws IOException {
        byte[] bytes = file.getBytes();
        String html = DocxToHtmlConverter.convert(bytes);
        if (!DocxToHtmlConverter.hasExtractableContent(html)) {
            throw new ServiceException(OaErrorCodes.LAYOUT_DOCX_IMPORT_FAILED.getCode(),
                    "Word 文档未包含可解析的正文内容");
        }
        return html;
    }

    private String suggestNameFromUrl(String url) {
        String slug = StrUtil.subAfter(url, "/", true);
        if (StrUtil.isBlank(slug)) {
            return "导入-公众号文章";
        }
        return "导入-" + StrUtil.sub(slug, 0, 40);
    }

    private WechatLayoutTemplateDO requireTemplate(Long id) {
        WechatLayoutTemplateDO entity = templateMapper.selectById(id);
        if (entity == null) {
            throw new ServiceException(OaErrorCodes.ENTITY_NOT_EXISTS);
        }
        Long tenantId = requireTenantId();
        if (!tenantId.equals(entity.getTenantId())) {
            throw new ServiceException(OaErrorCodes.TENANT_FORBIDDEN);
        }
        return entity;
    }

    private ProductionContentDO requireContent(Long id) {
        ProductionContentDO entity = productionContentMapper.selectById(id);
        if (entity == null) {
            throw new ServiceException(OaErrorCodes.ENTITY_NOT_EXISTS);
        }
        if (!requireTenantId().equals(entity.getTenantId())) {
            throw new ServiceException(OaErrorCodes.TENANT_FORBIDDEN);
        }
        return entity;
    }

    private LayoutImportJobDO requireImportJob(Long jobId) {
        LayoutImportJobDO job = importJobMapper.selectById(jobId);
        if (job == null) {
            throw new ServiceException(OaErrorCodes.LAYOUT_IMPORT_JOB_NOT_FOUND);
        }
        if (!requireTenantId().equals(job.getTenantId())) {
            throw new ServiceException(OaErrorCodes.TENANT_FORBIDDEN);
        }
        return job;
    }

    private void assertCanEditTemplate(WechatLayoutTemplateDO template) {
        Long userId = TenantContextHolder.getUserId();
        if (userId == null) {
            throw new ServiceException(OaErrorCodes.UNAUTHORIZED);
        }
        if (userId.equals(template.getCreatorUserId())) {
            return;
        }
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getAuthorities().stream()
                .anyMatch(a -> "oa:layout-template:update".equals(a.getAuthority())
                        || "oa:layout-template:delete".equals(a.getAuthority()))) {
            return;
        }
        throw new ServiceException(OaErrorCodes.FORBIDDEN);
    }

    private LayoutTemplateVO toVO(WechatLayoutTemplateDO entity) {
        LayoutTemplateVO vo = new LayoutTemplateVO();
        vo.setId(entity.getId());
        vo.setTemplateName(entity.getTemplateName());
        vo.setContentType(entity.getContentType());
        vo.setDocumentType(entity.getDocumentType());
        vo.setDescription(entity.getDescription());
        vo.setSourceType(entity.getSourceType());
        vo.setSourceUrl(entity.getSourceUrl());
        vo.setStatus(entity.getStatus());
        vo.setThumbnailUrl(entity.getThumbnailUrl());
        vo.setPreviewHtml(entity.getPreviewHtml());
        vo.setCreatorUserId(entity.getCreatorUserId());
        vo.setUpdateTime(entity.getUpdateTime());
        vo.setTags(entity.getTags());
        if (StrUtil.isNotBlank(entity.getDefaultParams())) {
            vo.setDefaultParams(JSONUtil.parse(entity.getDefaultParams()));
        }
        SysUserDO creator = sysUserMapper.selectById(entity.getCreatorUserId());
        if (creator != null) {
            vo.setCreatorName(creator.getNickname() != null ? creator.getNickname() : creator.getUsername());
        }
        return vo;
    }

    private LayoutTemplateDetailVO toDetailVO(WechatLayoutTemplateDO entity) {
        LayoutTemplateDetailVO vo = new LayoutTemplateDetailVO();
        LayoutTemplateVO base = toVO(entity);
        vo.setId(base.getId());
        vo.setTemplateName(base.getTemplateName());
        vo.setContentType(base.getContentType());
        vo.setDocumentType(base.getDocumentType());
        vo.setDescription(base.getDescription());
        vo.setSourceType(base.getSourceType());
        vo.setSourceUrl(base.getSourceUrl());
        vo.setStatus(base.getStatus());
        vo.setThumbnailUrl(base.getThumbnailUrl());
        vo.setCreatorUserId(base.getCreatorUserId());
        vo.setCreatorName(base.getCreatorName());
        vo.setUpdateTime(base.getUpdateTime());
        if (StrUtil.isNotBlank(entity.getLayoutJson())) {
            vo.setLayoutJson(JSONUtil.parse(entity.getLayoutJson()));
        }
        JSONObject schema = resolveTemplateSchema(entity);
        vo.setLayoutSchema(schema);
        vo.setSchemaVersion(entity.getSchemaVersion() != null ? entity.getSchemaVersion() : 2);
        vo.setLayoutHtml(StrUtil.blankToDefault(entity.getPreviewHtml(), entity.getLayoutHtml()));
        vo.setPreviewHtml(StrUtil.blankToDefault(entity.getPreviewHtml(),
                LayoutSchemaHelper.renderSchemaPreview(schema)));
        return vo;
    }

    private JSONObject resolveRequestSchema(Object layoutSchema, Object layoutJson) {
        if (layoutSchema != null) {
            JSONObject schema = JSONUtil.parseObj(JSONUtil.toJsonStr(layoutSchema));
            LayoutSchemaHelper.validateLayoutSchema(schema);
            return schema;
        }
        if (layoutJson != null) {
            return LayoutSchemaHelper.extractLayoutSchemaFromLayoutJson(layoutJson);
        }
        throw new ServiceException(OaErrorCodes.LAYOUT_SCHEMA_INVALID);
    }

    private JSONObject resolveTemplateSchema(WechatLayoutTemplateDO entity) {
        if (entity.getSchemaVersion() != null && entity.getSchemaVersion() >= 2
                && StrUtil.isNotBlank(entity.getLayoutSchema())) {
            return JSONUtil.parseObj(entity.getLayoutSchema());
        }
        if (StrUtil.isNotBlank(entity.getLayoutJson())) {
            JSONObject migrated = LayoutSchemaHelper.extractLayoutSchemaFromLayoutJson(
                    JSONUtil.parseObj(entity.getLayoutJson()));
            entity.setLayoutSchema(LayoutSchemaHelper.toJsonString(migrated));
            entity.setSchemaVersion(2);
            entity.setPreviewHtml(LayoutSchemaHelper.renderSchemaPreview(migrated));
            templateMapper.updateById(entity);
            return migrated;
        }
        return LayoutSchemaHelper.emptySchema();
    }

    private String resolveContentBody(ProductionContentDO content) {
        if (StrUtil.isNotBlank(content.getBody())) {
            return content.getBody();
        }
        if (BODY_FORMAT_LAYOUT.equals(content.getBodyFormat()) && StrUtil.isNotBlank(content.getLayoutJson())) {
            return LayoutSchemaHelper.extractTextFromLayout(JSONUtil.parse(content.getLayoutJson()));
        }
        return "";
    }

    private void assertNotPreset(WechatLayoutTemplateDO template) {
        if (SOURCE_PRESET.equals(template.getSourceType())) {
            throw new ServiceException(OaErrorCodes.LAYOUT_PRESET_READONLY);
        }
    }

    private Long requireTenantId() {
        Long tenantId = TenantContextHolder.getTenantId();
        if (tenantId == null) {
            throw new ServiceException(OaErrorCodes.UNAUTHORIZED.getCode(), "缺少租户上下文");
        }
        return tenantId;
    }
}
