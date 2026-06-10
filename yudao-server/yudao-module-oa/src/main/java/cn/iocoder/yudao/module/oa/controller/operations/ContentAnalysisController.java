package cn.iocoder.yudao.module.oa.controller.operations;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.oa.api.dto.operations.ContentAnalysisVO;
import cn.iocoder.yudao.module.oa.api.dto.operations.ContentStatsVO;
import cn.iocoder.yudao.module.oa.service.operations.ContentAnalysisService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/admin-api/oa/content-analysis")
@Validated
@RequiredArgsConstructor
public class ContentAnalysisController {

    private final ContentAnalysisService contentAnalysisService;

    @GetMapping("/list")
    public CommonResult<PageResult<ContentAnalysisVO>> list(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) Long ipGroupId,
            @RequestParam(required = false) Long accountId,
            @RequestParam(required = false) String platformType,
            @RequestParam(required = false) Boolean isHit,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer size) {
        return CommonResult.success(contentAnalysisService.list(startDate, endDate, ipGroupId, accountId, platformType, isHit, page, size));
    }

    @GetMapping("/stats")
    public CommonResult<ContentStatsVO> stats(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) Long ipGroupId,
            @RequestParam(required = false) Long accountId) {
        return CommonResult.success(contentAnalysisService.stats(startDate, endDate, ipGroupId, accountId));
    }

    // P-GATE-UNMOCK-R S-R2-D：作品数据趋势（按日聚合）
    // S-R8 B4: 收 contentId，详情场景可限定到单条作品
    @GetMapping("/trend")
    public CommonResult<List<Map<String, Object>>> trend(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) Long ipGroupId,
            @RequestParam(required = false) Long accountId,
            @RequestParam(required = false) Long contentId) {
        return CommonResult.success(contentAnalysisService.trend(startDate, endDate, ipGroupId, accountId, contentId));
    }
}
