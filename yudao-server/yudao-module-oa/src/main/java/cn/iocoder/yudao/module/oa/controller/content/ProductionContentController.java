package cn.iocoder.yudao.module.oa.controller.content;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.oa.api.dto.content.ContentReviewReq;
import cn.iocoder.yudao.module.oa.api.dto.content.ProductionContentCreateReq;
import cn.iocoder.yudao.module.oa.api.dto.content.ProductionContentUpdateReq;
import cn.iocoder.yudao.module.oa.api.dto.content.ProductionContentVO;
import cn.iocoder.yudao.module.oa.service.content.ProductionContentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
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
}
