package cn.iocoder.yudao.module.oa.controller.report;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.oa.api.dto.perf.ExportJobVO;
import cn.iocoder.yudao.module.oa.api.dto.report.ReportStatsVO;
import cn.iocoder.yudao.module.oa.service.report.ReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/admin-api/oa/report")
@Validated
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;

    @GetMapping("/unified-account/list")
    public CommonResult<PageResult<Map<String, Object>>> unifiedAccountList(
            @RequestParam(required = false) Long ipGroupId,
            @RequestParam(required = false) Long accountId,
            @RequestParam(required = false) String platformType,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "20") Integer pageSize) {
        return CommonResult.success(reportService.unifiedAccountList(ipGroupId, accountId, platformType, startDate, endDate, pageNum, pageSize));
    }

    @GetMapping("/unified-account/stats")
    public CommonResult<ReportStatsVO> unifiedAccountStats(
            @RequestParam(required = false) Long ipGroupId,
            @RequestParam(required = false) Long accountId,
            @RequestParam(required = false) String platformType,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return CommonResult.success(reportService.unifiedAccountStats(ipGroupId, accountId, platformType, startDate, endDate));
    }

    @PostMapping("/unified-account/export")
    public CommonResult<ExportJobVO> unifiedAccountExport(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return CommonResult.success(reportService.unifiedAccountExport(startDate, endDate));
    }

    @GetMapping("/account-status/trend")
    public CommonResult<List<Map<String, Object>>> accountStatusTrend(
            @RequestParam(required = false) Long accountId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return CommonResult.success(reportService.accountStatusTrend(accountId, startDate, endDate));
    }

    @GetMapping("/account-status/summary")
    public CommonResult<Map<String, Object>> accountStatusSummary(
            @RequestParam(required = false) Long accountId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return CommonResult.success(reportService.accountStatusSummary(accountId, startDate, endDate));
    }

    @GetMapping("/account-status/log")
    public CommonResult<PageResult<Map<String, Object>>> accountStatusLog(
            @RequestParam(required = false) Long accountId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "20") Integer pageSize) {
        return CommonResult.success(reportService.accountStatusLog(accountId, startDate, endDate, pageNum, pageSize));
    }

    @PostMapping("/account-status/export")
    public CommonResult<ExportJobVO> accountStatusExport(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return CommonResult.success(reportService.accountStatusExport(startDate, endDate));
    }

    @GetMapping("/video-output/list")
    public CommonResult<PageResult<Map<String, Object>>> videoOutputList(
            @RequestParam(required = false) Long ipGroupId,
            @RequestParam(required = false) Long accountId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "20") Integer pageSize) {
        return CommonResult.success(reportService.videoOutputList(ipGroupId, accountId, startDate, endDate, pageNum, pageSize));
    }

    @GetMapping("/video-output/trend")
    public CommonResult<List<Map<String, Object>>> videoOutputTrend(
            @RequestParam(required = false) Long accountId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return CommonResult.success(reportService.videoOutputTrend(accountId, startDate, endDate));
    }

    @GetMapping("/video-output/ranking")
    public CommonResult<List<Map<String, Object>>> videoOutputRanking(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(defaultValue = "10") Integer limit) {
        return CommonResult.success(reportService.videoOutputRanking(startDate, endDate, limit));
    }

    @PostMapping("/video-output/export")
    public CommonResult<ExportJobVO> videoOutputExport(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return CommonResult.success(reportService.videoOutputExport(startDate, endDate));
    }

    @GetMapping("/live-duration/list")
    public CommonResult<PageResult<Map<String, Object>>> liveDurationList(
            @RequestParam(required = false) Long ipGroupId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "20") Integer pageSize) {
        return CommonResult.success(reportService.liveDurationList(ipGroupId, startDate, endDate, pageNum, pageSize));
    }

    @GetMapping("/live-duration/trend")
    public CommonResult<List<Map<String, Object>>> liveDurationTrend(
            @RequestParam(required = false) Long ipGroupId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return CommonResult.success(reportService.liveDurationTrend(ipGroupId, startDate, endDate));
    }

    @PostMapping("/live-duration/export")
    public CommonResult<ExportJobVO> liveDurationExport(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return CommonResult.success(reportService.liveDurationExport(startDate, endDate));
    }

    @GetMapping("/cost-allocation/list")
    public CommonResult<PageResult<Map<String, Object>>> costAllocationList(
            @RequestParam(required = false) Long accountId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "20") Integer pageSize) {
        return CommonResult.success(reportService.costAllocationList(accountId, startDate, endDate, pageNum, pageSize));
    }

    @PostMapping("/cost-allocation/export")
    public CommonResult<ExportJobVO> costAllocationExport(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return CommonResult.success(reportService.costAllocationExport(startDate, endDate));
    }

    @GetMapping("/roi/list")
    public CommonResult<PageResult<Map<String, Object>>> roiList(
            @RequestParam(required = false) Long ipGroupId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "20") Integer pageSize) {
        return CommonResult.success(reportService.roiList(ipGroupId, startDate, endDate, pageNum, pageSize));
    }

    @PostMapping("/roi/export")
    public CommonResult<ExportJobVO> roiExport(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return CommonResult.success(reportService.roiExport(startDate, endDate));
    }

    @GetMapping("/team-config/list")
    public CommonResult<List<Map<String, Object>>> teamConfigList(@RequestParam(required = false) Long ipGroupId) {
        return CommonResult.success(reportService.teamConfigList(ipGroupId));
    }

    @GetMapping("/account-alert/list")
    public CommonResult<PageResult<Map<String, Object>>> accountAlertList(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "20") Integer pageSize) {
        return CommonResult.success(reportService.accountAlertList(startDate, endDate, pageNum, pageSize));
    }

    @PostMapping("/account-alert/export")
    public CommonResult<ExportJobVO> accountAlertExport(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return CommonResult.success(reportService.accountAlertExport(startDate, endDate));
    }
}
