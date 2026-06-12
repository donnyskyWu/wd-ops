package cn.iocoder.yudao.module.oa.controller.perf;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.oa.api.dto.perf.ExportJobVO;
import cn.iocoder.yudao.module.oa.api.dto.perf.OrderAttributionRoiVO;
import cn.iocoder.yudao.module.oa.api.dto.perf.OrderAttributionVO;
import cn.iocoder.yudao.module.oa.service.perf.OrderAttributionService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/admin-api/oa/order-attribution")
@Validated
@RequiredArgsConstructor
public class OrderAttributionController {

    private final OrderAttributionService orderAttributionService;

    @GetMapping("/list")
    public CommonResult<PageResult<OrderAttributionVO>> list(
            @RequestParam(required = false) Long ipGroupId,
            @RequestParam(required = false) Long accountId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "20") Integer pageSize) {
        return CommonResult.success(orderAttributionService.list(ipGroupId, accountId, startDate, endDate, pageNum, pageSize));
    }

    @GetMapping("/roi")
    public CommonResult<OrderAttributionRoiVO> roi(
            @RequestParam(required = false) Long ipGroupId,
            @RequestParam(required = false) Long accountId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return CommonResult.success(orderAttributionService.roi(ipGroupId, accountId, startDate, endDate));
    }

    @PostMapping("/export")
    public CommonResult<ExportJobVO> export(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return CommonResult.success(orderAttributionService.export(startDate, endDate));
    }
}
