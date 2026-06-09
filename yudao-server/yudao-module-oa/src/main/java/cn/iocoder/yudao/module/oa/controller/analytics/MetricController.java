package cn.iocoder.yudao.module.oa.controller.analytics;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.oa.api.dto.analytics.MetricCreateReq;
import cn.iocoder.yudao.module.oa.api.dto.analytics.MetricUpdateReq;
import cn.iocoder.yudao.module.oa.api.dto.analytics.MetricVO;
import cn.iocoder.yudao.module.oa.service.analytics.AnalyticsMetricService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin-api/oa/metric")
@Validated
@RequiredArgsConstructor
public class MetricController {

    private final AnalyticsMetricService analyticsMetricService;

    @GetMapping("/list")
    public CommonResult<PageResult<MetricVO>> list(
            @RequestParam(required = false) String metricType,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "20") Integer pageSize) {
        return CommonResult.success(analyticsMetricService.list(metricType, pageNum, pageSize));
    }

    @PostMapping("/create")
    public CommonResult<Long> create(@Valid @RequestBody MetricCreateReq req) {
        return CommonResult.success(analyticsMetricService.create(req));
    }

    @PutMapping("/update")
    public CommonResult<Boolean> update(@Valid @RequestBody MetricUpdateReq req) {
        analyticsMetricService.update(req);
        return CommonResult.success(true);
    }

    @DeleteMapping("/{id}")
    public CommonResult<Boolean> delete(@PathVariable Long id) {
        analyticsMetricService.delete(id);
        return CommonResult.success(true);
    }
}
