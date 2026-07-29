package cn.iocoder.yudao.module.oa.controller.system;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.oa.api.dto.system.UserCreateReq;
import cn.iocoder.yudao.module.oa.api.dto.system.UserRespVO;
import cn.iocoder.yudao.module.oa.api.dto.system.UserUpdateReq;
import cn.iocoder.yudao.module.oa.service.system.ParallelSystemDeprecatedSupport;
import cn.iocoder.yudao.module.oa.service.system.UserService;
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

/**
 * Parallel OPS user management — superseded by Football Admin (D-DEDUP-01). C-WP0 → 410.
 */
@Deprecated(since = "9.1.0", forRemoval = true)
@RestController
@RequestMapping({"/admin-api/oa/system/user", "/admin-api/system/user"})
@Validated
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping("/list")
    @PreAuthorize("hasAuthority('oa:user:list')")
    public CommonResult<PageResult<UserRespVO>> list(
            @RequestParam(required = false) String username,
            @RequestParam(required = false) String nickname,
            @RequestParam(required = false) Long roleId,
            @RequestParam(required = false) Long deptId,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") Integer pageNo,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        ParallelSystemDeprecatedSupport.throwParallelSystemDeprecated();
        return null;
    }

    @GetMapping("/profile")
    @PreAuthorize("isAuthenticated()")
    public CommonResult<UserRespVO> profile() {
        return CommonResult.success(userService.profile());
    }

    @PostMapping("/create")
    @PreAuthorize("hasAuthority('oa:user:create')")
    public CommonResult<Long> create(@Valid @RequestBody UserCreateReq req) {
        ParallelSystemDeprecatedSupport.throwParallelSystemDeprecated();
        return null;
    }

    @PutMapping("/update")
    @PreAuthorize("hasAuthority('oa:user:update')")
    public CommonResult<Boolean> update(@Valid @RequestBody UserUpdateReq req) {
        ParallelSystemDeprecatedSupport.throwParallelSystemDeprecated();
        return null;
    }

    @DeleteMapping("/delete")
    @PreAuthorize("hasAuthority('oa:user:delete')")
    public CommonResult<Boolean> delete(@RequestParam Long id) {
        ParallelSystemDeprecatedSupport.throwParallelSystemDeprecated();
        return null;
    }
}
