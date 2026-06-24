package cn.iocoder.yudao.module.oa.controller.wechatanalysis;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.oa.api.dto.wechatanalysis.WeworkAnalysisDetailVO;
import cn.iocoder.yudao.module.oa.api.dto.wechatanalysis.WeworkAnalysisListItemVO;
import cn.iocoder.yudao.module.oa.service.wechatanalysis.WechatAnalysisWeworkService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

/** M1 微信数据分析 — 企微 Tab（M10-WECOM-S-04 · ADR-048）。API-M1 正式文档待 Spec 增量。 */
@RestController
@RequestMapping("/admin-api/oa/wechat-analysis/wework")
@Validated
@RequiredArgsConstructor
public class WechatAnalysisWeworkController {

    private final WechatAnalysisWeworkService wechatAnalysisWeworkService;

    @GetMapping("/list")
    public CommonResult<PageResult<WeworkAnalysisListItemVO>> list(
            @RequestParam(required = false) Long accountId,
            @RequestParam(required = false) String accountName,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate statDate,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer size) {
        return CommonResult.success(wechatAnalysisWeworkService.list(
                accountId, accountName, statDate, page, size));
    }

    @GetMapping("/detail")
    public CommonResult<WeworkAnalysisDetailVO> detail(
            @RequestParam Long accountId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return CommonResult.success(wechatAnalysisWeworkService.detail(accountId, startDate, endDate));
    }
}
