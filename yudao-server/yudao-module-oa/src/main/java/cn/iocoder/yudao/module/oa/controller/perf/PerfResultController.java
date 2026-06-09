package cn.iocoder.yudao.module.oa.controller.perf;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.oa.api.dto.perf.ExportJobVO;
import cn.iocoder.yudao.module.oa.api.dto.perf.PerfExportReq;
import cn.iocoder.yudao.module.oa.api.dto.perf.PerfResultVO;
import cn.iocoder.yudao.module.oa.api.dto.perf.PerfTrendVO;
import cn.iocoder.yudao.module.oa.service.perf.PerfResultService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/admin-api/oa/perf/result")
@Validated
@RequiredArgsConstructor
public class PerfResultController {

    private final PerfResultService perfResultService;

    @GetMapping("/list")
    public CommonResult<PageResult<PerfResultVO>> list(
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) String periodType,
            @RequestParam(required = false) String grade,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "20") Integer pageSize) {
        return CommonResult.success(perfResultService.list(userId, periodType, grade, startDate, pageNum, pageSize));
    }

    @GetMapping("/{userId}/trend")
    public CommonResult<PerfTrendVO> trend(@PathVariable Long userId,
                                           @RequestParam(required = false) Integer month) {
        return CommonResult.success(perfResultService.trend(userId, month));
    }

    @PostMapping("/export")
    public CommonResult<ExportJobVO> export(@RequestBody PerfExportReq req) {
        return CommonResult.success(perfResultService.export(req));
    }
}
