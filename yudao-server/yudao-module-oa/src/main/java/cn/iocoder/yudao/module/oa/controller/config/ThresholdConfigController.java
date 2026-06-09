package cn.iocoder.yudao.module.oa.controller.config;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.oa.api.dto.config.ThresholdConfigCreateReq;
import cn.iocoder.yudao.module.oa.api.dto.config.ThresholdConfigRespVO;
import cn.iocoder.yudao.module.oa.api.dto.config.ThresholdConfigUpdateReq;
import cn.iocoder.yudao.module.oa.service.config.ThresholdConfigService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin-api/oa/config/threshold")
@Validated
@RequiredArgsConstructor
public class ThresholdConfigController {

    private final ThresholdConfigService thresholdConfigService;

    @GetMapping("/list")
    public CommonResult<PageResult<ThresholdConfigRespVO>> list(
            @RequestParam(required = false) String metricName,
            @RequestParam(required = false) String metricType,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") Integer pageNo,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        return CommonResult.success(thresholdConfigService.list(metricName, metricType, status, pageNo, pageSize));
    }

    @PostMapping("/create")
    public CommonResult<Long> create(@Valid @RequestBody ThresholdConfigCreateReq req) {
        return CommonResult.success(thresholdConfigService.create(req));
    }

    @PutMapping("/update")
    public CommonResult<Boolean> update(@Valid @RequestBody ThresholdConfigUpdateReq req) {
        thresholdConfigService.update(req);
        return CommonResult.success(true);
    }

    @DeleteMapping("/delete")
    public CommonResult<Boolean> delete(@RequestParam Long id) {
        thresholdConfigService.delete(id);
        return CommonResult.success(true);
    }
}
