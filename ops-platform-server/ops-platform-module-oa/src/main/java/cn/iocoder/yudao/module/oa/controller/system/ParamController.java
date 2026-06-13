package cn.iocoder.yudao.module.oa.controller.system;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.oa.api.dto.system.ParamCreateReq;
import cn.iocoder.yudao.module.oa.api.dto.system.ParamRespVO;
import cn.iocoder.yudao.module.oa.api.dto.system.ParamUpdateReq;
import cn.iocoder.yudao.module.oa.service.system.ParamService;
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
@RequestMapping("/admin-api/oa/system/param")
@Validated
@RequiredArgsConstructor
public class ParamController {

    private final ParamService paramService;

    @GetMapping("/list")
    public CommonResult<PageResult<ParamRespVO>> list(
            @RequestParam(required = false) String paramName,
            @RequestParam(required = false) String paramKey,
            @RequestParam(required = false) String category,
            @RequestParam(defaultValue = "1") Integer pageNo,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        return CommonResult.success(paramService.list(paramName, paramKey, category, pageNo, pageSize));
    }

    @PostMapping("/create")
    public CommonResult<Long> create(@Valid @RequestBody ParamCreateReq req) {
        return CommonResult.success(paramService.create(req));
    }

    @PutMapping("/update")
    public CommonResult<Boolean> update(@Valid @RequestBody ParamUpdateReq req) {
        paramService.update(req);
        return CommonResult.success(true);
    }

    @DeleteMapping("/delete")
    public CommonResult<Boolean> delete(@RequestParam Long id) {
        paramService.delete(id);
        return CommonResult.success(true);
    }
}
