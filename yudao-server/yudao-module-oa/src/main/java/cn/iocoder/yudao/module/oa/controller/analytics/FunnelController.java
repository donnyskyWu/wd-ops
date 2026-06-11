package cn.iocoder.yudao.module.oa.controller.analytics;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.oa.api.dto.analytics.FunnelCreateReq;
import cn.iocoder.yudao.module.oa.api.dto.analytics.FunnelDataVO;
import cn.iocoder.yudao.module.oa.api.dto.analytics.FunnelVO;
import cn.iocoder.yudao.module.oa.api.dto.perf.ExportJobVO;
import cn.iocoder.yudao.module.oa.service.analytics.FunnelService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/admin-api/oa/funnel")
@Validated
@RequiredArgsConstructor
public class FunnelController {

    private final FunnelService funnelService;

    @GetMapping("/list")
    public CommonResult<PageResult<FunnelVO>> list(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "20") Integer pageSize) {
        return CommonResult.success(funnelService.list(pageNum, pageSize));
    }

    @PostMapping("/create")
    public CommonResult<Long> create(@Valid @RequestBody FunnelCreateReq req) {
        return CommonResult.success(funnelService.create(req));
    }

    @GetMapping("/{id}/data")
    public CommonResult<FunnelDataVO> data(
            @PathVariable Long id,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) Long ipGroupId,
            @RequestParam(required = false) String platformType) {
        return CommonResult.success(funnelService.getData(id, startDate, endDate, ipGroupId, platformType));
    }

    @PostMapping("/export")
    public CommonResult<ExportJobVO> export(@RequestParam Long id) {
        return CommonResult.success(funnelService.export(id));
    }
}
