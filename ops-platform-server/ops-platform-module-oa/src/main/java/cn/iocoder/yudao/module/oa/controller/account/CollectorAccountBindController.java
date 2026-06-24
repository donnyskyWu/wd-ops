package cn.iocoder.yudao.module.oa.controller.account;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.module.oa.api.dto.collect.CollectorAccountBindRespVO;
import cn.iocoder.yudao.module.oa.api.dto.collect.CollectorAccountBindTestConnectionRespVO;
import cn.iocoder.yudao.module.oa.service.collect.unified.UnifiedCollectorAdapter;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin-api/oa/account")
@Validated
@RequiredArgsConstructor
public class CollectorAccountBindController {

    private final UnifiedCollectorAdapter unifiedCollectorAdapter;

    @GetMapping("/{oaAccountId}/collector-bind")
    @PreAuthorize("hasAuthority('oa:account:list')")
    public CommonResult<CollectorAccountBindRespVO> getBind(@PathVariable Long oaAccountId) {
        return CommonResult.success(unifiedCollectorAdapter.getBind(oaAccountId));
    }

    @PostMapping("/{oaAccountId}/collector-bind")
    @PreAuthorize("hasAuthority('oa:account:list')")
    public CommonResult<CollectorAccountBindRespVO> bind(@PathVariable Long oaAccountId) {
        return CommonResult.success(unifiedCollectorAdapter.bindAccount(oaAccountId));
    }

    @PostMapping("/{oaAccountId}/collector-bind/sync")
    @PreAuthorize("hasAuthority('oa:account:list')")
    public CommonResult<CollectorAccountBindRespVO> sync(@PathVariable Long oaAccountId) {
        return CommonResult.success(unifiedCollectorAdapter.syncCredentials(oaAccountId));
    }

    @PostMapping("/{oaAccountId}/collector-bind/test-connection")
    @PreAuthorize("hasAuthority('oa:account:list')")
    public CommonResult<CollectorAccountBindTestConnectionRespVO> testConnection(@PathVariable Long oaAccountId) {
        return CommonResult.success(unifiedCollectorAdapter.testConnection(oaAccountId));
    }
}
