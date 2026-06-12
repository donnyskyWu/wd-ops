package cn.iocoder.yudao.module.oa.controller.analytics;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.module.oa.api.dto.analytics.DashboardCreateReq;
import cn.iocoder.yudao.module.oa.api.dto.analytics.DashboardDataVO;
import cn.iocoder.yudao.module.oa.api.dto.analytics.DashboardUpdateReq;
import cn.iocoder.yudao.module.oa.api.dto.analytics.DashboardVO;
import cn.iocoder.yudao.module.oa.service.analytics.DashboardDataService;
import cn.iocoder.yudao.module.oa.service.analytics.DashboardService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/admin-api/oa/dashboard")
@Validated
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;
    private final DashboardDataService dashboardDataService;

    @GetMapping("/{id}")
    public CommonResult<DashboardVO> get(@PathVariable Long id) {
        return CommonResult.success(dashboardService.get(id));
    }

    @GetMapping("/{id}/data")
    public CommonResult<DashboardDataVO> getData(
            @PathVariable Long id,
            @RequestParam(required = false) Long ipGroupId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) String platformType) {
        return CommonResult.success(dashboardDataService.loadData(id, ipGroupId, startDate, endDate, platformType));
    }

    @PostMapping("/create")
    public CommonResult<Long> create(@Valid @RequestBody DashboardCreateReq req) {
        return CommonResult.success(dashboardService.create(req));
    }

    @PutMapping("/full-update")
    public CommonResult<Boolean> fullUpdate(@Valid @RequestBody DashboardUpdateReq req) {
        dashboardService.updateFull(req);
        return CommonResult.success(true);
    }
}
