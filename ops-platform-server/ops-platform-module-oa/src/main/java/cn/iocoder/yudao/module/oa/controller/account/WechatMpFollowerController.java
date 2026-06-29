package cn.iocoder.yudao.module.oa.controller.account;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.oa.api.dto.account.MpFollowerRespVO;
import cn.iocoder.yudao.module.oa.service.account.WechatMpFollowerQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin-api/oa/account")
@Validated
@RequiredArgsConstructor
public class WechatMpFollowerController {

    private final WechatMpFollowerQueryService wechatMpFollowerQueryService;

    @GetMapping("/{accountId}/mp-followers")
    public CommonResult<PageResult<MpFollowerRespVO>> page(
            @PathVariable Long accountId,
            @RequestParam(defaultValue = "1") Integer pageNo,
            @RequestParam(defaultValue = "20") Integer pageSize) {
        return CommonResult.success(wechatMpFollowerQueryService.pageByAccount(accountId, pageNo, pageSize));
    }
}
