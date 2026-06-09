package cn.iocoder.yudao.module.oa.controller.operations;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.oa.api.dto.operations.ContentImportVO;
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
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer size) {
        return CommonResult.success(internalContentService.list(platformType, dataSource, page, size));
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
}
