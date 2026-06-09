package cn.iocoder.yudao.module.oa.controller.system;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.module.oa.api.dto.system.PermissionRespVO;
import cn.iocoder.yudao.module.oa.service.system.PermissionService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/admin-api/system/permission")
@RequiredArgsConstructor
public class PermissionController {

    private final PermissionService permissionService;

    @GetMapping("/list")
    @PreAuthorize("hasAuthority('oa:permission:list')")
    public CommonResult<List<PermissionRespVO>> list() {
        return CommonResult.success(permissionService.listAll());
    }
}
