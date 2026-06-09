package cn.iocoder.yudao.module.oa.controller.analytics;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.oa.api.dto.analytics.DashboardVO;
import cn.iocoder.yudao.module.oa.service.analytics.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/admin-api/oa/dashboard-config")
@Validated
@RequiredArgsConstructor
public class DashboardConfigController {

    private final DashboardService dashboardService;

    @GetMapping("/list")
    public CommonResult<PageResult<DashboardVO>> list(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "20") Integer pageSize) {
        return CommonResult.success(dashboardService.listConfig(pageNum, pageSize));
    }

    @PostMapping("/update")
    public CommonResult<Boolean> update(@RequestBody Map<String, Object> body) {
        Long id = Long.valueOf(body.get("id").toString());
        String layout = body.get("layout") != null ? body.get("layout").toString() : null;
        dashboardService.updateConfig(id, layout);
        return CommonResult.success(true);
    }
}
