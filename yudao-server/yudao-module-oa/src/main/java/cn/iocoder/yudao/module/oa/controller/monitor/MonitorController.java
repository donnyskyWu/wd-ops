package cn.iocoder.yudao.module.oa.controller.monitor;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.oa.api.dto.monitor.ExternalWorkVO;
import cn.iocoder.yudao.module.oa.service.monitor.MonitorService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.Map;

@RestController
@RequestMapping("/admin-api/oa/monitor")
@Validated
@RequiredArgsConstructor
public class MonitorController {

    private final MonitorService monitorService;

    @GetMapping("/external/list")
    public CommonResult<PageResult<ExternalWorkVO>> externalList(
            @RequestParam(required = false) String platformType,
            @RequestParam(required = false) Long ipGroupId,
            @RequestParam(required = false) String industry,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "20") Integer pageSize) {
        return CommonResult.success(monitorService.externalList(platformType, ipGroupId, industry, startDate, endDate, pageNum, pageSize));
    }

    @GetMapping("/hit/list")
    public CommonResult<PageResult<ExternalWorkVO>> hitList(
            @RequestParam(required = false) String platformType,
            @RequestParam(required = false) Long ipGroupId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "20") Integer pageSize) {
        return CommonResult.success(monitorService.hitList(platformType, ipGroupId, startDate, endDate, pageNum, pageSize));
    }

    @GetMapping("/low-score/list")
    public CommonResult<PageResult<ExternalWorkVO>> lowScoreList(
            @RequestParam(required = false) String platformType,
            @RequestParam(required = false) Long ipGroupId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "20") Integer pageSize) {
        return CommonResult.success(monitorService.lowScoreList(platformType, ipGroupId, startDate, endDate, pageNum, pageSize));
    }

    @GetMapping("/high-follower/list")
    public CommonResult<PageResult<ExternalWorkVO>> highFollowerList(
            @RequestParam(required = false) Long ipGroupId,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "20") Integer pageSize) {
        return CommonResult.success(monitorService.highFollowerList(ipGroupId, pageNum, pageSize));
    }

    @GetMapping("/low-follower/list")
    public CommonResult<PageResult<ExternalWorkVO>> lowFollowerList(
            @RequestParam(required = false) Long ipGroupId,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "20") Integer pageSize) {
        return CommonResult.success(monitorService.lowFollowerList(ipGroupId, pageNum, pageSize));
    }

    @GetMapping("/ip-theme/{id}")
    public CommonResult<Map<String, Object>> ipTheme(@PathVariable Long id) {
        return CommonResult.success(monitorService.ipTheme(id));
    }

    @GetMapping("/industry/{id}")
    public CommonResult<Map<String, Object>> industry(@PathVariable Long id) {
        return CommonResult.success(monitorService.industryStats(id));
    }
}
