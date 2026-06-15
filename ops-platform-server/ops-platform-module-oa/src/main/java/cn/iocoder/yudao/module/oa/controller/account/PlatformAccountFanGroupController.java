package cn.iocoder.yudao.module.oa.controller.account;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.module.oa.api.dto.account.FanGroupCreateReq;
import cn.iocoder.yudao.module.oa.api.dto.account.FanGroupRespVO;
import cn.iocoder.yudao.module.oa.api.dto.account.FanGroupUpdateReq;
import cn.iocoder.yudao.module.oa.service.account.PlatformAccountFanGroupService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/admin-api/oa/account/fan-group")
@Validated
@RequiredArgsConstructor
public class PlatformAccountFanGroupController {

    private final PlatformAccountFanGroupService fanGroupService;

    @GetMapping("/list")
    @PreAuthorize("hasAuthority('oa:account:list')")
    public CommonResult<List<FanGroupRespVO>> list(@RequestParam Long accountId) {
        return CommonResult.success(fanGroupService.listByAccount(accountId));
    }

    @PostMapping("/create")
    @PreAuthorize("hasAuthority('oa:account:list')")
    public CommonResult<Long> create(@Valid @RequestBody FanGroupCreateReq req) {
        return CommonResult.success(fanGroupService.create(req));
    }

    @PutMapping("/update")
    @PreAuthorize("hasAuthority('oa:account:list')")
    public CommonResult<Boolean> update(@Valid @RequestBody FanGroupUpdateReq req) {
        fanGroupService.update(req);
        return CommonResult.success(true);
    }

    @DeleteMapping("/delete")
    @PreAuthorize("hasAuthority('oa:account:list')")
    public CommonResult<Boolean> delete(@RequestParam Long id) {
        fanGroupService.delete(id);
        return CommonResult.success(true);
    }
}
