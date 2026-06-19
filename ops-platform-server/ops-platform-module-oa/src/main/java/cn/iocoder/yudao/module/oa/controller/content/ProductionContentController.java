package cn.iocoder.yudao.module.oa.controller.content;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.oa.api.dto.content.ContentPublishOptionsVO;
import cn.iocoder.yudao.module.oa.api.dto.content.ContentPublishReq;
import cn.iocoder.yudao.module.oa.api.dto.content.ContentPublishResultVO;
import cn.iocoder.yudao.module.oa.api.dto.content.ContentTransferKnowledgeResultVO;
import cn.iocoder.yudao.module.oa.api.dto.content.ContentTypesetReq;
import cn.iocoder.yudao.module.oa.api.dto.content.ContentTypesetVO;
import cn.iocoder.yudao.module.oa.api.dto.content.ContentApplyLayoutTemplateReq;
import cn.iocoder.yudao.module.oa.api.dto.content.LayoutMergePreviewReq;
import cn.iocoder.yudao.module.oa.api.dto.content.LayoutMergePreviewVO;
import cn.iocoder.yudao.module.oa.api.dto.content.ContentAiGenerateReq;
import cn.iocoder.yudao.module.oa.api.dto.content.ContentAiGenerateResultVO;
import cn.iocoder.yudao.module.oa.api.dto.content.ContentAiPromptOptionVO;
import cn.iocoder.yudao.module.oa.api.dto.content.ContentGenerateResultVO;
import cn.iocoder.yudao.module.oa.api.dto.content.ContentReviewConfigVO;
import cn.iocoder.yudao.module.oa.api.dto.content.ContentReviewReq;
import cn.iocoder.yudao.module.oa.api.dto.content.ContentScriptRefVO;
import cn.iocoder.yudao.module.oa.api.dto.content.ProductionContentCreateReq;
import cn.iocoder.yudao.module.oa.api.dto.content.ProductionContentUpdateReq;
import cn.iocoder.yudao.module.oa.api.dto.content.ProductionContentVO;
import cn.iocoder.yudao.module.oa.service.content.ProductionContentService;
import cn.iocoder.yudao.module.oa.service.content.WechatLayoutTemplateService;
import cn.iocoder.yudao.module.oa.service.content.ContentPublishService;
import cn.iocoder.yudao.module.oa.service.content.TypesettingService;
import cn.hutool.core.util.StrUtil;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin-api/oa/content")
@Validated
@RequiredArgsConstructor
public class ProductionContentController {

    private final ProductionContentService productionContentService;
    private final WechatLayoutTemplateService layoutTemplateService;
    private final ContentPublishService contentPublishService;
    private final TypesettingService typesettingService;

    @GetMapping("/list")
    public CommonResult<PageResult<ProductionContentVO>> list(
            @RequestParam(required = false) String title,
            @RequestParam(required = false) String platformType,
            @RequestParam(required = false) String contentType,
            @RequestParam(required = false) Long accountId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Integer aiGenerated,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "20") Integer pageSize) {
        return CommonResult.success(productionContentService.list(title, platformType, contentType,
                accountId, status, aiGenerated, pageNum, pageSize));
    }

    @PostMapping("/create")
    public CommonResult<Long> create(@Valid @RequestBody ProductionContentCreateReq req) {
        return CommonResult.success(productionContentService.create(req));
    }

    @GetMapping("/by-task")
    public CommonResult<ProductionContentVO> getByTask(@RequestParam Long taskId) {
        return CommonResult.success(productionContentService.getByTaskId(taskId));
    }

    @GetMapping("/{id}")
    public CommonResult<ProductionContentVO> get(@PathVariable Long id) {
        return CommonResult.success(productionContentService.getById(id));
    }

    @GetMapping("/script-ref")
    public CommonResult<ContentScriptRefVO> getScriptRef(
            @RequestParam String competitionId,
            @RequestParam(required = false, defaultValue = "SHORT_VIDEO_SCRIPT") String documentType) {
        return CommonResult.success(productionContentService.getScriptRef(competitionId, documentType));
    }

    @PostMapping("/{id}/confirm")
    public CommonResult<Boolean> confirm(@PathVariable Long id) {
        productionContentService.confirm(id);
        return CommonResult.success(true);
    }

    @PostMapping("/{id}/generate")
    public CommonResult<ContentGenerateResultVO> generate(@PathVariable Long id) {
        return CommonResult.success(productionContentService.generate(id));
    }

    @GetMapping("/ai-prompt-options")
    public CommonResult<java.util.List<ContentAiPromptOptionVO>> listAiPromptOptions(
            @RequestParam String contentType,
            @RequestParam(required = false) String documentType) {
        return CommonResult.success(productionContentService.listAiPromptOptions(contentType, documentType));
    }

    @PostMapping("/ai-generate")
    public CommonResult<ContentAiGenerateResultVO> aiGenerate(@Valid @RequestBody ContentAiGenerateReq req) {
        return CommonResult.success(productionContentService.aiGenerate(req));
    }

    @PutMapping("/update")
    public CommonResult<Boolean> update(@Valid @RequestBody ProductionContentUpdateReq req) {
        productionContentService.update(req);
        return CommonResult.success(true);
    }

    @DeleteMapping("/{id}")
    public CommonResult<Boolean> delete(@PathVariable Long id) {
        productionContentService.delete(id);
        return CommonResult.success(true);
    }

    @PostMapping("/{id}/submit-review")
    public CommonResult<Boolean> submitReview(@PathVariable Long id) {
        productionContentService.submitReview(id);
        return CommonResult.success(true);
    }

    @PostMapping("/{id}/review")
    public CommonResult<Boolean> review(@PathVariable Long id, @Valid @RequestBody ContentReviewReq req) {
        productionContentService.review(id, req);
        return CommonResult.success(true);
    }

    @GetMapping("/review-config")
    public CommonResult<ContentReviewConfigVO> getReviewConfig() {
        return CommonResult.success(productionContentService.getReviewConfig());
    }

    @GetMapping("/{id}/publish-options")
    @PreAuthorize("hasAuthority('oa:content:publish')")
    public CommonResult<ContentPublishOptionsVO> getPublishOptions(@PathVariable Long id) {
        return CommonResult.success(contentPublishService.getPublishOptions(id));
    }

    @PostMapping("/{id}/publish")
    @PreAuthorize("hasAuthority('oa:content:publish')")
    public CommonResult<ContentPublishResultVO> publish(
            @PathVariable Long id,
            @Valid @RequestBody ContentPublishReq req) {
        return CommonResult.success(contentPublishService.publish(id, req));
    }

    @PostMapping("/{id}/transfer-to-knowledge")
    @PreAuthorize("hasAuthority('oa:content:transfer-knowledge')")
    public CommonResult<ContentTransferKnowledgeResultVO> transferToKnowledge(@PathVariable Long id) {
        return CommonResult.success(productionContentService.transferToKnowledge(id));
    }

    @PostMapping("/{id}/apply-layout-template")
    public CommonResult<ProductionContentVO> applyLayoutTemplate(
            @PathVariable Long id,
            @Valid @RequestBody ContentApplyLayoutTemplateReq req) {
        return CommonResult.success(layoutTemplateService.applyLayoutTemplate(id, req));
    }

    @PostMapping("/{id}/apply-layout-template/preview")
    public CommonResult<LayoutMergePreviewVO> previewApplyLayoutTemplate(
            @PathVariable Long id,
            @Valid @RequestBody ContentApplyLayoutTemplateReq req) {
        ProductionContentVO content = productionContentService.getById(id);
        LayoutMergePreviewReq previewReq = new LayoutMergePreviewReq();
        previewReq.setBody(content.getBody());
        if (content.getLayoutJson() != null) {
            previewReq.setExistingLayoutJson(content.getLayoutJson());
        }
        return CommonResult.success(layoutTemplateService.previewMerge(req.getLayoutTemplateId(), previewReq));
    }

    @PostMapping("/typeset")
    @PreAuthorize("hasAuthority('oa:content:typeset')")
    public CommonResult<ContentTypesetVO> typeset(@Valid @RequestBody ContentTypesetReq req) {
        return CommonResult.success(typesettingService.typeset(req));
    }

    @PostMapping("/{id}/typeset")
    @PreAuthorize("hasAuthority('oa:content:typeset')")
    public CommonResult<ContentTypesetVO> typesetContent(
            @PathVariable Long id,
            @RequestBody(required = false) ContentTypesetReq req) {
        ProductionContentVO content = productionContentService.getById(id);
        ContentTypesetReq effective = req != null ? req : new ContentTypesetReq();
        if (StrUtil.isBlank(effective.getHtml())) {
            effective.setHtml(content.getBody() != null ? content.getBody() : "");
        }
        return CommonResult.success(typesettingService.typeset(effective));
    }
}
