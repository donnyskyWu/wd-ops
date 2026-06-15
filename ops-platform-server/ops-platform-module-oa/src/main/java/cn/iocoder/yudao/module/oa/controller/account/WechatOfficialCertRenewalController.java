package cn.iocoder.yudao.module.oa.controller.account;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.module.oa.api.dto.account.WechatCertRenewalCreateReq;
import cn.iocoder.yudao.module.oa.api.dto.account.WechatCertRenewalRespVO;
import cn.iocoder.yudao.module.oa.service.account.WechatOfficialCertRenewalService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/admin-api/oa/account/wechat-cert-renewal")
@Validated
@RequiredArgsConstructor
public class WechatOfficialCertRenewalController {

    private final WechatOfficialCertRenewalService renewalService;

    @GetMapping("/list")
    @PreAuthorize("hasAuthority('oa:account:list')")
    public CommonResult<List<WechatCertRenewalRespVO>> list(@RequestParam Long accountId) {
        return CommonResult.success(renewalService.listByAccount(accountId));
    }

    @PostMapping("/create")
    @PreAuthorize("hasAuthority('oa:account:list')")
    public CommonResult<Long> create(@Valid @RequestBody WechatCertRenewalCreateReq req) {
        return CommonResult.success(renewalService.create(req));
    }

    @DeleteMapping("/delete")
    @PreAuthorize("hasAuthority('oa:account:list')")
    public CommonResult<Boolean> delete(@RequestParam Long id) {
        renewalService.delete(id);
        return CommonResult.success(true);
    }
}
