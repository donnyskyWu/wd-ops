package cn.iocoder.yudao.module.oa.controller.home;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.module.oa.api.dto.home.DashboardAccountOverviewVO;
import cn.iocoder.yudao.module.oa.api.dto.home.DashboardAlertItemVO;
import cn.iocoder.yudao.module.oa.api.dto.home.DashboardContentOverviewVO;
import cn.iocoder.yudao.module.oa.api.dto.home.DashboardHomeKpiVO;
import cn.iocoder.yudao.module.oa.api.dto.home.DashboardTodoItemVO;
import cn.iocoder.yudao.module.oa.api.dto.home.HomeMetricsVO;
import cn.iocoder.yudao.module.oa.api.dto.home.PlatformDistVO;
import cn.iocoder.yudao.module.oa.api.dto.home.QuickActionVO;
import cn.iocoder.yudao.module.oa.api.dto.home.TodoVO;
import cn.iocoder.yudao.module.oa.api.dto.home.TrendPointVO;
import cn.iocoder.yudao.module.oa.service.home.HomeDashboardService;
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

@RestController
@RequestMapping("/admin-api/oa/dashboard/home")
@Validated
@RequiredArgsConstructor
public class HomeDashboardController {

    private final HomeDashboardService homeDashboardService;

    @GetMapping("/kpi")
    public CommonResult<DashboardHomeKpiVO> kpi(
            @RequestParam(required = false) Long ipGroupId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return CommonResult.success(homeDashboardService.getKpi(ipGroupId, startDate, endDate));
    }

    @GetMapping("/account-overview")
    public CommonResult<List<DashboardAccountOverviewVO>> accountOverview(
            @RequestParam(required = false) Long ipGroupId) {
        return CommonResult.success(homeDashboardService.getAccountOverview(ipGroupId));
    }

    @GetMapping("/content-overview")
    public CommonResult<List<DashboardContentOverviewVO>> contentOverview(
            @RequestParam(required = false) Long ipGroupId) {
        return CommonResult.success(homeDashboardService.getContentOverview(ipGroupId));
    }

    @GetMapping("/alert-list")
    public CommonResult<List<DashboardAlertItemVO>> alertList(
            @RequestParam(required = false) Long ipGroupId,
            @RequestParam(required = false) Integer limit) {
        return CommonResult.success(homeDashboardService.getAlertList(ipGroupId, limit));
    }

    @GetMapping("/todo-list")
    public CommonResult<List<DashboardTodoItemVO>> todoList(
            @RequestParam(required = false) Long ipGroupId) {
        return CommonResult.success(homeDashboardService.getTodoList(ipGroupId));
    }

    @GetMapping("/metrics")
    public CommonResult<HomeMetricsVO> metrics(
            @RequestParam(required = false) Long ipGroupId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return CommonResult.success(homeDashboardService.getMetrics(ipGroupId, startDate, endDate));
    }

    @GetMapping("/trend")
    public CommonResult<List<TrendPointVO>> trend(
            @RequestParam(required = false) Long ipGroupId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) String platformType,
            @RequestParam(required = false) String type) {
        return CommonResult.success(homeDashboardService.getTrend(ipGroupId, startDate, endDate, platformType, type));
    }

    @GetMapping("/platform-dist")
    public CommonResult<List<PlatformDistVO>> platformDist(
            @RequestParam(required = false) Long ipGroupId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return CommonResult.success(homeDashboardService.getPlatformDist(ipGroupId, startDate, endDate));
    }

    @GetMapping("/todos")
    public CommonResult<List<TodoVO>> todos(
            @RequestParam(required = false) Long ipGroupId,
            @RequestParam(required = false) Integer limit) {
        return CommonResult.success(homeDashboardService.getTodos(ipGroupId, limit));
    }

    @GetMapping("/quick-actions")
    public CommonResult<List<QuickActionVO>> quickActions() {
        return CommonResult.success(homeDashboardService.getQuickActions());
    }

    @PostMapping("/refresh")
    public CommonResult<Boolean> refresh() {
        homeDashboardService.refresh();
        return CommonResult.success(true);
    }
}
