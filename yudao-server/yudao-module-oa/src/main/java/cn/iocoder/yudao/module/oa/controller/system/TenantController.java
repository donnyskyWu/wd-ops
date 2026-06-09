package cn.iocoder.yudao.module.oa.controller.system;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.oa.api.dto.system.TenantCreateReq;
import cn.iocoder.yudao.module.oa.api.dto.system.TenantRespVO;
import cn.iocoder.yudao.module.oa.api.dto.system.TenantUpdateReq;
import cn.iocoder.yudao.module.oa.service.system.TenantService;
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

@RestController
@RequestMapping("/admin-api/system/tenant")
@Validated
@RequiredArgsConstructor
public class TenantController {

    private final TenantService tenantService;

    @GetMapping("/list")
    @PreAuthorize("hasAuthority('oa:tenant:list')")
    public CommonResult<PageResult<TenantRespVO>> list(
            @RequestParam(required = false) String tenantName,
            @RequestParam(required = false) String contactName,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") Integer pageNo,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        return CommonResult.success(tenantService.list(tenantName, contactName, status, pageNo, pageSize));
    }

    @PostMapping("/create")
    @PreAuthorize("hasAuthority('oa:tenant:create')")
    public CommonResult<Long> create(@Valid @RequestBody TenantCreateReq req) {
        return CommonResult.success(tenantService.create(req));
    }

    @PutMapping("/update")
    @PreAuthorize("hasAuthority('oa:tenant:update')")
    public CommonResult<Boolean> update(@Valid @RequestBody TenantUpdateReq req) {
        tenantService.update(req);
        return CommonResult.success(true);
    }

    @DeleteMapping("/delete")
    @PreAuthorize("hasAuthority('oa:tenant:delete')")
    public CommonResult<Boolean> delete(@RequestParam Long id) {
        tenantService.delete(id);
        return CommonResult.success(true);
    }
}
