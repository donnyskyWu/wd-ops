package cn.iocoder.yudao.module.oa.controller.operations;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.oa.api.dto.operations.FollowerAnalysisVO;
import cn.iocoder.yudao.module.oa.api.dto.operations.FollowerTrendVO;
import cn.iocoder.yudao.module.oa.service.operations.FollowerAnalysisService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/admin-api/oa/follower-analysis")
@Validated
@RequiredArgsConstructor
public class FollowerAnalysisController {

    private final FollowerAnalysisService followerAnalysisService;

    @GetMapping("/list")
    public CommonResult<PageResult<FollowerAnalysisVO>> list(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) Long ipGroupId,
            @RequestParam(required = false) Long accountId,
            @RequestParam(required = false) String platformType,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer size) {
        return CommonResult.success(followerAnalysisService.list(startDate, endDate, ipGroupId, accountId, platformType, page, size));
    }

    @GetMapping("/trend")
    public CommonResult<List<FollowerTrendVO>> trend(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) Long ipGroupId,
            @RequestParam(required = false) Long accountId) {
        return CommonResult.success(followerAnalysisService.trend(startDate, endDate, ipGroupId, accountId));
    }
}
