package cn.iocoder.yudao.module.oa.controller.operations;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.oa.api.dto.operations.AccountAnalysisVO;
import cn.iocoder.yudao.module.oa.service.operations.AccountAnalysisService;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin-api/oa/account-analysis")
@Validated
@RequiredArgsConstructor
public class AccountAnalysisController {

    private final AccountAnalysisService accountAnalysisService;

    @GetMapping("/list")
    public CommonResult<PageResult<AccountAnalysisVO>> list(
            @RequestParam(required = false) String platform,
            @RequestParam(required = false) Long ipGroupId,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer size) {
        return CommonResult.success(accountAnalysisService.list(platform, ipGroupId, keyword, page, size));
    }
}
