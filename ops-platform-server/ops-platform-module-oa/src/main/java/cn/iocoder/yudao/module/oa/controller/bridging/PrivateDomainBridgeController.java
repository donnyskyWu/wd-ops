package cn.iocoder.yudao.module.oa.controller.bridging;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.oa.api.dto.bridging.PrivateDomainBridgeCreateReq;
import cn.iocoder.yudao.module.oa.api.dto.bridging.PrivateDomainBridgeRejectReq;
import cn.iocoder.yudao.module.oa.api.dto.bridging.PrivateDomainBridgeRespVO;
import cn.iocoder.yudao.module.oa.service.bridging.PrivateDomainBridgeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin-api/oa/collect/private-domain-bridge")
@Validated
@RequiredArgsConstructor
public class PrivateDomainBridgeController {

    private final PrivateDomainBridgeService privateDomainBridgeService;

    @GetMapping("/page")
    public CommonResult<PageResult<PrivateDomainBridgeRespVO>> page(
            @RequestParam(required = false) String reviewStatus,
            @RequestParam(required = false) String sourceType,
            @RequestParam(required = false) String targetType,
            @RequestParam(required = false) String matchMethod,
            @RequestParam(defaultValue = "1") Integer pageNo,
            @RequestParam(defaultValue = "20") Integer pageSize) {
        return CommonResult.success(privateDomainBridgeService.page(
                reviewStatus, sourceType, targetType, matchMethod, pageNo, pageSize));
    }

    @GetMapping("/{id}")
    public CommonResult<PrivateDomainBridgeRespVO> get(@PathVariable Long id) {
        return CommonResult.success(privateDomainBridgeService.get(id));
    }

    @PostMapping("/create")
    public CommonResult<Long> create(@Valid @RequestBody PrivateDomainBridgeCreateReq req) {
        return CommonResult.success(privateDomainBridgeService.create(req));
    }

    @PostMapping("/{id}/confirm")
    public CommonResult<Boolean> confirm(@PathVariable Long id) {
        privateDomainBridgeService.confirm(id);
        return CommonResult.success(true);
    }

    @PostMapping("/{id}/reject")
    public CommonResult<Boolean> reject(@PathVariable Long id,
                                        @RequestBody(required = false) PrivateDomainBridgeRejectReq req) {
        privateDomainBridgeService.reject(id, req);
        return CommonResult.success(true);
    }
}
