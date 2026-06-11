package cn.iocoder.yudao.module.oa.controller.operations;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.oa.api.dto.operations.ContentImportVO;
import cn.iocoder.yudao.module.oa.api.dto.operations.ContentTrendDetailVO;
import cn.iocoder.yudao.module.oa.api.dto.operations.ImportContentDataReq;
import cn.iocoder.yudao.module.oa.api.dto.operations.ImportReviewReq;
import cn.iocoder.yudao.module.oa.api.dto.operations.InternalContentVO;
import cn.iocoder.yudao.module.oa.service.operations.InternalContentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin-api/oa/internal-content")
@Validated
@RequiredArgsConstructor
public class InternalContentController {

    private final InternalContentService internalContentService;

    @GetMapping("/list")
    public CommonResult<PageResult<InternalContentVO>> list(
            @RequestParam(required = false) String platformType,
            @RequestParam(required = false) String dataSource,
            @RequestParam(required = false) String contentType,
            // S-R7-Bug4：补 4 个筛选项（之前 Spring 忽略）
            @RequestParam(required = false) Long ipGroupId,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) java.time.LocalDate startDate,
            @RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) java.time.LocalDate endDate,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer size) {
        return CommonResult.success(internalContentService.list(platformType, dataSource, contentType,
                ipGroupId, keyword, startDate, endDate, page, size));
    }

    @PostMapping("/import")
    public CommonResult<Long> submitImport(@Valid @RequestBody ImportContentDataReq req) {
        return CommonResult.success(internalContentService.submitImport(req));
    }

    @GetMapping("/import/list")
    public CommonResult<PageResult<ContentImportVO>> importList(
            @RequestParam(required = false) Integer reviewStatus,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer size) {
        return CommonResult.success(internalContentService.importList(reviewStatus, page, size));
    }

    @PutMapping("/import/{id}/review")
    public CommonResult<Boolean> reviewImport(@PathVariable Long id, @Valid @RequestBody ImportReviewReq req) {
        internalContentService.reviewImport(id, req);
        return CommonResult.success(true);
    }

    // P-GATE-UNMOCK-R S-R2-Fix-3：内容趋势详情（spec: API-M1-运营管理 §4.5）
    @GetMapping("/{id}/trend")
    public CommonResult<ContentTrendDetailVO> trend(
            @PathVariable Long id,
            @RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) java.time.LocalDate startDate,
            @RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) java.time.LocalDate endDate) {
        return CommonResult.success(internalContentService.trend(id, startDate, endDate));
    }
}
