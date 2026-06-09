package cn.iocoder.yudao.module.oa.controller.operations;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.oa.api.dto.operations.ProductivityReviewVO;
import cn.iocoder.yudao.module.oa.service.operations.ProductivityReviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin-api/oa/productivity-review")
@Validated
@RequiredArgsConstructor
public class ProductivityReviewController {

    private final ProductivityReviewService productivityReviewService;

    @GetMapping("/list")
    public CommonResult<PageResult<ProductivityReviewVO>> list(
            @RequestParam(required = false) Long ipGroupId,
            @RequestParam(required = false) Long userId,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer size) {
        return CommonResult.success(productivityReviewService.list(ipGroupId, userId, page, size));
    }
}
