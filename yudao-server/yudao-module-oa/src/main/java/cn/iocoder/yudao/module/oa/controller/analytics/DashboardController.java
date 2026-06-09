package cn.iocoder.yudao.module.oa.controller.analytics;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.module.oa.api.dto.analytics.DashboardCreateReq;
import cn.iocoder.yudao.module.oa.api.dto.analytics.DashboardVO;
import cn.iocoder.yudao.module.oa.service.analytics.DashboardService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin-api/oa/dashboard")
@Validated
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping("/{id}")
    public CommonResult<DashboardVO> get(@PathVariable Long id) {
        return CommonResult.success(dashboardService.get(id));
    }

    @PostMapping("/create")
    public CommonResult<Long> create(@Valid @RequestBody DashboardCreateReq req) {
        return CommonResult.success(dashboardService.create(req));
    }
}
