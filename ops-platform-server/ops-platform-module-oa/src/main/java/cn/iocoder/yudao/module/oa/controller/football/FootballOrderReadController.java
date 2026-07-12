package cn.iocoder.yudao.module.oa.controller.football;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.oa.api.dto.football.FootballOrderListVO;
import cn.iocoder.yudao.module.oa.service.football.FootballOrderReadService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

/**
 * P2b: read-only Football order list for 订单归因 / 财务 ROI pages.
 * Data source: {@code pay_all_order} (Football pay module SSOT).
 */
@RestController
@RequestMapping("/admin-api/oa/football-order")
@Validated
@RequiredArgsConstructor
public class FootballOrderReadController {

    private final FootballOrderReadService footballOrderReadService;

    @GetMapping("/list")
    @PreAuthorize("hasAnyAuthority('oa:order-attribution:list','oa:roi:list')")
    public CommonResult<PageResult<FootballOrderListVO>> list(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) Long authorId,
            @RequestParam(required = false) Integer status,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "20") Integer pageSize) {
        return CommonResult.success(
                footballOrderReadService.listPayAllOrders(startDate, endDate, authorId, status, pageNum, pageSize));
    }
}
