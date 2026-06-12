package cn.iocoder.yudao.module.oa.controller.operations;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.oa.api.dto.operations.AccountAnalysisVO;
import cn.iocoder.yudao.module.oa.api.dto.operations.ContentAnalysisVO;
import cn.iocoder.yudao.module.oa.api.dto.operations.FollowerAnalysisVO;
import cn.iocoder.yudao.module.oa.service.operations.AccountAnalysisService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/admin-api/oa/account-analysis")
@Validated
@RequiredArgsConstructor
public class AccountAnalysisController {

    private final AccountAnalysisService accountAnalysisService;

    @GetMapping("/list")
    public CommonResult<PageResult<AccountAnalysisVO>> list(
            @RequestParam(required = false) String platform,
            @RequestParam(required = false) Long ipGroupId,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer size) {
        return CommonResult.success(accountAnalysisService.list(platform, ipGroupId, keyword, page, size));
    }

    // P-GATE-UNMOCK-R S-R2-B：账号粉丝详情（按账号 ID）
    @GetMapping("/{id}/followers")
    public CommonResult<List<FollowerAnalysisVO>> followers(
            @PathVariable("id") Long accountId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return CommonResult.success(accountAnalysisService.listAccountFollowers(accountId, startDate, endDate));
    }

    // P-GATE-UNMOCK-R S-R2-B：账号作品详情（分页）
    @GetMapping("/{id}/contents")
    public CommonResult<PageResult<ContentAnalysisVO>> contents(
            @PathVariable("id") Long accountId,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer size) {
        return CommonResult.success(accountAnalysisService.listAccountContents(accountId, page, size));
    }

    @GetMapping("/{id}/follower-trend")
    public CommonResult<List<Map<String, Object>>> followerTrend(
            @PathVariable("id") Long accountId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return CommonResult.success(accountAnalysisService.accountFollowerTrend(accountId, startDate, endDate));
    }

    @GetMapping("/{id}/content-trend")
    public CommonResult<List<Map<String, Object>>> contentTrend(
            @PathVariable("id") Long accountId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return CommonResult.success(accountAnalysisService.accountContentTrend(accountId, startDate, endDate));
    }
}
