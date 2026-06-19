package cn.iocoder.yudao.module.oa.controller.content;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
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
import cn.iocoder.yudao.module.oa.service.content.WechatLayoutTemplateService;
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
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/admin-api/oa/layout-template")
@Validated
@RequiredArgsConstructor
public class LayoutTemplateController {

    private final WechatLayoutTemplateService layoutTemplateService;

    /** 分页列表；须排在 /{id} 之前，避免 "list"/"page" 等被当作 id */
    @GetMapping("/list")
    @PreAuthorize("hasAuthority('oa:layout-template:list')")
    public CommonResult<PageResult<LayoutTemplateVO>> list(
            @RequestParam(required = false) String templateName,
            @RequestParam(required = false) String documentType,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String sourceType,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "20") Integer pageSize) {
        return CommonResult.success(layoutTemplateService.list(templateName, documentType, status, sourceType,
                pageNum, pageSize));
    }

    @GetMapping("/select-list")
    @PreAuthorize("hasAuthority('oa:layout-template:list')")
    public CommonResult<List<LayoutTemplateSelectVO>> selectList(
            @RequestParam String contentType,
            @RequestParam(required = false) String documentType) {
        return CommonResult.success(layoutTemplateService.selectList(contentType, documentType));
    }

    @GetMapping("/import-job/{jobId}")
    @PreAuthorize("hasAuthority('oa:layout-template:import')")
    public CommonResult<LayoutImportJobVO> getImportJob(@PathVariable Long jobId) {
        return CommonResult.success(layoutTemplateService.getImportJob(jobId));
    }

    @PostMapping("/{id}/publish")
    @PreAuthorize("hasAuthority('oa:layout-template:update')")
    public CommonResult<Boolean> publish(@PathVariable Long id) {
        layoutTemplateService.publish(id);
        return CommonResult.success(true);
    }

    @PostMapping("/{id}/disable")
    @PreAuthorize("hasAuthority('oa:layout-template:update')")
    public CommonResult<Boolean> disable(@PathVariable Long id) {
        layoutTemplateService.disable(id);
        return CommonResult.success(true);
    }

    @PostMapping("/{id}/enable")
    @PreAuthorize("hasAuthority('oa:layout-template:update')")
    public CommonResult<Boolean> enable(@PathVariable Long id) {
        layoutTemplateService.enable(id);
        return CommonResult.success(true);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('oa:layout-template:list')")
    public CommonResult<LayoutTemplateDetailVO> get(@PathVariable Long id) {
        return CommonResult.success(layoutTemplateService.getById(id));
    }

    @PostMapping("/create")
    @PreAuthorize("hasAuthority('oa:layout-template:create')")
    public CommonResult<Long> create(@Valid @RequestBody LayoutTemplateCreateReq req) {
        return CommonResult.success(layoutTemplateService.create(req));
    }

    @PutMapping("/update")
    @PreAuthorize("hasAuthority('oa:layout-template:update')")
    public CommonResult<Boolean> update(@Valid @RequestBody LayoutTemplateUpdateReq req) {
        layoutTemplateService.update(req);
        return CommonResult.success(true);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('oa:layout-template:delete')")
    public CommonResult<Boolean> delete(@PathVariable Long id) {
        layoutTemplateService.delete(id);
        return CommonResult.success(true);
    }

    @PostMapping("/import-url")
    @PreAuthorize("hasAuthority('oa:layout-template:import')")
    public CommonResult<LayoutImportJobCreateResp> importUrl(@Valid @RequestBody LayoutImportUrlReq req) {
        return CommonResult.success(layoutTemplateService.importUrl(req));
    }

    @PostMapping("/import-docx")
    @PreAuthorize("hasAuthority('oa:layout-template:import')")
    public CommonResult<LayoutImportJobCreateResp> importDocx(
            @RequestParam("file") MultipartFile file,
            @RequestParam(required = false) String templateName,
            @RequestParam(required = false) String documentType) {
        return CommonResult.success(layoutTemplateService.importDocx(file, templateName, documentType));
    }

    @PostMapping("/import-mhtml")
    @PreAuthorize("hasAuthority('oa:layout-template:import')")
    public CommonResult<LayoutImportJobCreateResp> importMhtml(
            @RequestParam("file") MultipartFile file,
            @RequestParam(required = false) String templateName,
            @RequestParam(required = false) String documentType) {
        return CommonResult.success(layoutTemplateService.importMhtml(file, templateName, documentType));
    }

    @PostMapping("/{id}/preview-merge")
    @PreAuthorize("hasAuthority('oa:layout-template:list')")
    public CommonResult<LayoutMergePreviewVO> previewMerge(
            @PathVariable Long id,
            @Valid @RequestBody LayoutMergePreviewReq req) {
        return CommonResult.success(layoutTemplateService.previewMerge(id, req));
    }

    @PostMapping("/{id}/partial-apply")
    @PreAuthorize("hasAuthority('oa:layout-template:list')")
    public CommonResult<LayoutMergePreviewVO> partialApply(
            @PathVariable Long id,
            @Valid @RequestBody LayoutMergePreviewReq req) {
        return CommonResult.success(layoutTemplateService.partialApply(id, req));
    }

    @PostMapping("/{id}/apply-background")
    @PreAuthorize("hasAuthority('oa:layout-template:list')")
    public CommonResult<LayoutMergePreviewVO> applyBackground(
            @PathVariable Long id,
            @Valid @RequestBody LayoutMergePreviewReq req) {
        return CommonResult.success(layoutTemplateService.applyBackground(id, req));
    }

    @PostMapping("/{id}/copy")
    @PreAuthorize("hasAuthority('oa:layout-template:create')")
    public CommonResult<Long> copy(@PathVariable Long id) {
        return CommonResult.success(layoutTemplateService.copyTemplate(id));
    }

    @PostMapping("/validate-extract-fidelity")
    @PreAuthorize("hasAuthority('oa:layout-template:import')")
    public CommonResult<LayoutMergePreviewVO> validateExtractFidelity(
            @RequestBody java.util.Map<String, Object> body) {
        Object schema = body.get("layoutSchema");
        String sampleBody = body.get("sampleBody") != null ? String.valueOf(body.get("sampleBody")) : null;
        return CommonResult.success(layoutTemplateService.validateExtractFidelity(schema, sampleBody));
    }

    @PostMapping("/import-paste-preview")
    @PreAuthorize("hasAuthority('oa:layout-template:import')")
    public CommonResult<LayoutImportJobCreateResp> importPastePreview(@Valid @RequestBody LayoutImportPasteReq req) {
        return CommonResult.success(layoutTemplateService.importPastePreview(req));
    }

    @PostMapping("/import-paste")
    @PreAuthorize("hasAuthority('oa:layout-template:import')")
    public CommonResult<LayoutTemplateDetailVO> importPaste(@Valid @RequestBody LayoutImportPasteReq req) {
        return CommonResult.success(layoutTemplateService.importPaste(req));
    }
}
