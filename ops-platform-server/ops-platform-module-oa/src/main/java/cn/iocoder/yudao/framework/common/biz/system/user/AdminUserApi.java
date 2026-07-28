package cn.iocoder.yudao.framework.common.biz.system.user;

import cn.iocoder.yudao.framework.common.biz.system.user.dto.AdminUserRespDTO;
import cn.iocoder.yudao.framework.common.enums.RpcConstants;
import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Collection;
import java.util.List;

/**
 * Football system-server user RPC (vendored subset, G-SYS-01).
 * Aligns with {@code football.module.system.api.user.AdminUserApi#getSimpleUserList}.
 */
@FeignClient(name = RpcConstants.SYSTEM_NAME, primary = false)
public interface AdminUserApi {

    String PREFIX = RpcConstants.SYSTEM_PREFIX + "/user";

    @GetMapping(PREFIX + "/simple-list")
    CommonResult<List<AdminUserRespDTO>> getSimpleUserList(
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "status", required = false) Integer status,
            @RequestParam(value = "deptId", required = false) Long deptId);

    @GetMapping(PREFIX + "/get")
    CommonResult<AdminUserRespDTO> getUser(@RequestParam("id") Long id);

    @GetMapping(PREFIX + "/valid")
    CommonResult<Boolean> validateUserList(@RequestParam("ids") Collection<Long> ids);

    @GetMapping(PREFIX + "/getUserListByRoleId")
    CommonResult<List<AdminUserRespDTO>> getUserListByRoleId(@RequestParam("roleId") Long roleId);
}
