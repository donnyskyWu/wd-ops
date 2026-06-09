package cn.iocoder.yudao.module.oa.controller.sop;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.module.oa.api.dto.sop.SopReviewActionReq;
import cn.iocoder.yudao.module.oa.api.dto.sop.SopReviewVO;
import cn.iocoder.yudao.module.oa.service.sop.SopReviewService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/admin-api/oa/sop/review")
@Validated
@RequiredArgsConstructor
public class SopReviewController {

    private final SopReviewService sopReviewService;

    @GetMapping("/pending")
    public CommonResult<List<SopReviewVO>> pending(@RequestParam(required = false) Long reviewerId) {
        return CommonResult.success(sopReviewService.pending(reviewerId));
    }

    @PostMapping("/approve")
    public CommonResult<Boolean> approve(@Valid @RequestBody SopReviewActionReq req) {
        sopReviewService.approve(req);
        return CommonResult.success(true);
    }

    @PostMapping("/reject")
    public CommonResult<Boolean> reject(@Valid @RequestBody SopReviewActionReq req) {
        sopReviewService.reject(req);
        return CommonResult.success(true);
    }
}
