package cn.iocoder.yudao.module.oa.controller.personal;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.oa.api.dto.personal.WeworkEmployeeCreateReq;
import cn.iocoder.yudao.module.oa.api.dto.personal.WeworkEmployeeRespVO;
import cn.iocoder.yudao.module.oa.api.dto.personal.WeworkEmployeeUpdateReq;
import cn.iocoder.yudao.module.oa.service.personal.WeworkEmployeeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
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
@RequestMapping("/admin-api/oa/internal/wework/employee")
@Validated
@RequiredArgsConstructor
public class WeworkEmployeeController {

    private final WeworkEmployeeService weworkEmployeeService;

    @GetMapping("/list")
    public CommonResult<PageResult<WeworkEmployeeRespVO>> list(
            @RequestParam(required = false) Long weworkAccountId,
            @RequestParam(required = false) String nickname,
            @RequestParam(required = false) String weworkUserId,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") Integer pageNo,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        return CommonResult.success(weworkEmployeeService.list(
                weworkAccountId, nickname, weworkUserId, status, pageNo, pageSize));
    }

    @GetMapping("/get")
    public CommonResult<WeworkEmployeeRespVO> get(@RequestParam Long id) {
        return CommonResult.success(weworkEmployeeService.get(id));
    }

    @PostMapping("/create")
    public CommonResult<Long> create(@Valid @RequestBody WeworkEmployeeCreateReq req) {
        return CommonResult.success(weworkEmployeeService.create(req));
    }

    @PutMapping("/update")
    public CommonResult<Boolean> update(@Valid @RequestBody WeworkEmployeeUpdateReq req) {
        weworkEmployeeService.update(req);
        return CommonResult.success(true);
    }

    @DeleteMapping("/delete")
    public CommonResult<Boolean> delete(@RequestParam Long id) {
        weworkEmployeeService.delete(id);
        return CommonResult.success(true);
    }
}
