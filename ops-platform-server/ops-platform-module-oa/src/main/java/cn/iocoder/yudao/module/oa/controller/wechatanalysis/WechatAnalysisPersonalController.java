package cn.iocoder.yudao.module.oa.controller.wechatanalysis;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.oa.api.dto.wechatanalysis.PersonalAnalysisDetailVO;
import cn.iocoder.yudao.module.oa.api.dto.wechatanalysis.PersonalAnalysisListItemVO;
import cn.iocoder.yudao.module.oa.service.wechatanalysis.WechatAnalysisPersonalService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

/**
 * M1 微信数据分析 — 个微 Tab（M10-AO-S-06 · ADR-045 oa_personal_wechat_daily_stats）。
 * API-M1 正式文档待 Spec 增量；路径对齐企微 {@code /wechat-analysis/wework/*}。
 */
@RestController
@RequestMapping("/admin-api/oa/wechat-analysis/personal")
@Validated
@RequiredArgsConstructor
public class WechatAnalysisPersonalController {

    private final WechatAnalysisPersonalService wechatAnalysisPersonalService;

    @GetMapping("/list")
    public CommonResult<PageResult<PersonalAnalysisListItemVO>> list(
            @RequestParam(required = false) Long accountId,
            @RequestParam(required = false) String accountName,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate statDate,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer size) {
        return CommonResult.success(wechatAnalysisPersonalService.list(
                accountId, accountName, statDate, page, size));
    }

    @GetMapping("/detail")
    public CommonResult<PersonalAnalysisDetailVO> detail(
            @RequestParam Long accountId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return CommonResult.success(wechatAnalysisPersonalService.detail(accountId, startDate, endDate));
    }
}
