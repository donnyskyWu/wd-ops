package cn.iocoder.yudao.module.oa.controller.account;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.oa.api.dto.account.AccountCreateReq;
import cn.iocoder.yudao.module.oa.api.dto.account.AccountReplaceReq;
import cn.iocoder.yudao.module.oa.api.dto.account.AccountRespVO;
import cn.iocoder.yudao.module.oa.api.dto.account.AccountUpdateReq;
import cn.iocoder.yudao.module.oa.service.account.PlatformAccountService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin-api/oa/account")
@Validated
@RequiredArgsConstructor
public class PlatformAccountController {

    private final PlatformAccountService platformAccountService;

    @GetMapping("/list")
    @PreAuthorize("hasAuthority('oa:account:list')")
    public CommonResult<PageResult<AccountRespVO>> list(
            @RequestParam(required = false) String platformType,
            @RequestParam(required = false) String accountName,
            @RequestParam(required = false) Long companyId,
            @RequestParam(required = false) Long realnameId,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") Integer pageNo,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        return CommonResult.success(platformAccountService.list(
                platformType, accountName, companyId, realnameId, status, pageNo, pageSize));
    }

    @GetMapping("/get")
    @PreAuthorize("hasAuthority('oa:account:list')")
    public CommonResult<AccountRespVO> get(@RequestParam Long id) {
        return CommonResult.success(platformAccountService.get(id));
    }

    @PostMapping("/create")
    public CommonResult<Long> create(@Valid @RequestBody AccountCreateReq req) {
        return CommonResult.success(platformAccountService.create(req));
    }

    @PutMapping("/update")
    public CommonResult<Boolean> update(@Valid @RequestBody AccountUpdateReq req) {
        platformAccountService.update(req);
        return CommonResult.success(true);
    }

    @DeleteMapping("/delete")
    public CommonResult<Boolean> delete(@RequestParam Long id) {
        platformAccountService.delete(id);
        return CommonResult.success(true);
    }

    @PostMapping("/{id}/replace")
    public CommonResult<Boolean> replace(@PathVariable Long id, @Valid @RequestBody AccountReplaceReq req) {
        platformAccountService.replace(id, req);
        return CommonResult.success(true);
    }
}
