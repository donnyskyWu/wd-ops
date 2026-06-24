package cn.iocoder.yudao.module.oa.controller.personal;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.oa.api.dto.personal.WeworkCreateReq;
import cn.iocoder.yudao.module.oa.api.dto.personal.WeworkRespVO;
import cn.iocoder.yudao.module.oa.api.dto.personal.WeworkTestConnectionRespVO;
import cn.iocoder.yudao.module.oa.api.dto.personal.WeworkUpdateReq;
import cn.iocoder.yudao.module.oa.service.personal.WeworkAccountService;
import cn.iocoder.yudao.module.oa.service.collect.wework.WeComAdapter;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin-api/oa/internal/wework")
@Validated
@RequiredArgsConstructor
public class WeworkAccountController {

    private final WeworkAccountService weworkAccountService;
    private final WeComAdapter weComAdapter;

    @GetMapping("/list")
    public CommonResult<PageResult<WeworkRespVO>> list(
            @RequestParam(required = false) String accountName,
            @RequestParam(required = false) String corpId,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") Integer pageNo,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        return CommonResult.success(weworkAccountService.list(accountName, corpId, status, pageNo, pageSize));
    }

    @GetMapping("/get")
    public CommonResult<WeworkRespVO> get(@RequestParam Long id) {
        return CommonResult.success(weworkAccountService.get(id));
    }

    @PostMapping("/create")
    public CommonResult<Long> create(@Valid @RequestBody WeworkCreateReq req) {
        return CommonResult.success(weworkAccountService.create(req));
    }

    @PutMapping("/update")
    public CommonResult<Boolean> update(@Valid @RequestBody WeworkUpdateReq req) {
        weworkAccountService.update(req);
        return CommonResult.success(true);
    }

    @DeleteMapping("/delete")
    public CommonResult<Boolean> delete(@RequestParam Long id) {
        weworkAccountService.delete(id);
        return CommonResult.success(true);
    }

    @PostMapping("/{id}/test-connection")
    public CommonResult<WeworkTestConnectionRespVO> testConnection(@PathVariable Long id) {
        return CommonResult.success(weComAdapter.testConnection(id));
    }
}
