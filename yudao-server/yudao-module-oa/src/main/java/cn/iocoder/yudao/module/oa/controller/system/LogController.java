package cn.iocoder.yudao.module.oa.controller.system;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.oa.api.dto.system.LoginLogVO;
import cn.iocoder.yudao.module.oa.api.dto.system.OperationLogVO;
import cn.iocoder.yudao.module.oa.service.system.LogService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping({"/admin-api/oa/system/log", "/admin-api/system/log"})
@Validated
@RequiredArgsConstructor
public class LogController {

    private final LogService logService;

    @GetMapping("/operation")
    @PreAuthorize("hasAuthority('oa:log:operation')")
    public CommonResult<PageResult<OperationLogVO>> operation(
            @RequestParam(required = false) String username,
            @RequestParam(required = false) String module,
            @RequestParam(required = false) String level,
            @RequestParam(required = false) String startTime,
            @RequestParam(required = false) String endTime,
            @RequestParam(defaultValue = "1") Integer pageNo,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        return CommonResult.success(logService.listOperation(username, module, level, startTime, endTime, pageNo, pageSize));
    }

    @GetMapping("/login")
    @PreAuthorize("hasAuthority('oa:log:login')")
    public CommonResult<PageResult<LoginLogVO>> login(
            @RequestParam(required = false) String username,
            @RequestParam(required = false) String ip,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String startTime,
            @RequestParam(required = false) String endTime,
            @RequestParam(defaultValue = "1") Integer pageNo,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        return CommonResult.success(logService.listLogin(username, ip, status, startTime, endTime, pageNo, pageSize));
    }
}
