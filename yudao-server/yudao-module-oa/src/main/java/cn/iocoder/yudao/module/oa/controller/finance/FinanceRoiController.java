package cn.iocoder.yudao.module.oa.controller.finance;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.module.oa.api.dto.finance.FinanceRoiAnalysisVO;
import cn.iocoder.yudao.module.oa.api.dto.finance.FinanceRoiBreakdownVO;
import cn.iocoder.yudao.module.oa.api.dto.finance.FinanceRoiTrendVO;
import cn.iocoder.yudao.module.oa.api.dto.perf.ExportJobVO;
import cn.iocoder.yudao.module.oa.service.finance.FinanceRoiService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/admin-api/oa/finance/roi")
@Validated
@RequiredArgsConstructor
public class FinanceRoiController {

    private final FinanceRoiService financeRoiService;

    @GetMapping("/analysis")
    public CommonResult<FinanceRoiAnalysisVO> analysis(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) Long ipGroupId,
            @RequestParam(required = false) Long accountId,
            @RequestParam(required = false) String dimension) {
        return CommonResult.success(financeRoiService.analysis(startDate, endDate, ipGroupId, accountId, dimension));
    }

    @GetMapping("/trend")
    public CommonResult<FinanceRoiTrendVO> trend(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) Long ipGroupId,
            @RequestParam(required = false) Long accountId) {
        return CommonResult.success(financeRoiService.trend(startDate, endDate, ipGroupId, accountId));
    }

    @GetMapping("/breakdown")
    public CommonResult<FinanceRoiBreakdownVO> breakdown(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) Long ipGroupId,
            @RequestParam(required = false) Long accountId) {
        return CommonResult.success(financeRoiService.breakdown(startDate, endDate, ipGroupId, accountId));
    }

    @PostMapping("/export")
    public CommonResult<ExportJobVO> export(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return CommonResult.success(financeRoiService.export(startDate, endDate));
    }
}
