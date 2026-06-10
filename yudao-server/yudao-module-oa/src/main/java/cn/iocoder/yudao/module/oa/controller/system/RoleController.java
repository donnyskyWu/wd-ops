package cn.iocoder.yudao.module.oa.controller.system;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.oa.api.dto.system.PermissionRespVO;
import cn.iocoder.yudao.module.oa.api.dto.system.RoleAssignPermissionReq;
import cn.iocoder.yudao.module.oa.api.dto.system.RoleCreateReq;
import cn.iocoder.yudao.module.oa.api.dto.system.RoleRespVO;
import cn.iocoder.yudao.module.oa.api.dto.system.RoleUpdateReq;
import cn.iocoder.yudao.module.oa.service.system.RoleService;
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
@RequestMapping({"/admin-api/oa/system/role", "/admin-api/system/role"})
@Validated
@RequiredArgsConstructor
public class RoleController {

    private final RoleService roleService;

    @GetMapping("/list")
    @PreAuthorize("hasAuthority('oa:role:list')")
    public CommonResult<PageResult<RoleRespVO>> list(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String code,
            @RequestParam(defaultValue = "1") Integer pageNo,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        return CommonResult.success(roleService.list(name, code, pageNo, pageSize));
    }

    @PostMapping("/create")
    @PreAuthorize("hasAuthority('oa:role:create')")
    public CommonResult<Long> create(@Valid @RequestBody RoleCreateReq req) {
        return CommonResult.success(roleService.create(req));
    }

    @PutMapping("/update")
    @PreAuthorize("hasAuthority('oa:role:update')")
    public CommonResult<Boolean> update(@Valid @RequestBody RoleUpdateReq req) {
        roleService.update(req);
        return CommonResult.success(true);
    }

    @DeleteMapping("/delete")
    @PreAuthorize("hasAuthority('oa:role:delete')")
    public CommonResult<Boolean> delete(@RequestParam Long id) {
        roleService.delete(id);
        return CommonResult.success(true);
    }

    @PostMapping("/assign-permission")
    @PreAuthorize("hasAuthority('oa:role:assign-permission')")
    public CommonResult<Boolean> assignPermission(@Valid @RequestBody RoleAssignPermissionReq req) {
        roleService.assignPermission(req);
        return CommonResult.success(true);
    }

    @GetMapping("/permissions")
    @PreAuthorize("hasAuthority('oa:role:list')")
    public CommonResult<List<PermissionRespVO>> permissions(@RequestParam Long roleId) {
        return CommonResult.success(roleService.listPermissions(roleId));
    }
}
