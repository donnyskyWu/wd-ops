package cn.iocoder.yudao.module.oa.controller.config;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.oa.api.dto.config.CollectConfigCreateReq;
import cn.iocoder.yudao.module.oa.api.dto.config.CollectConfigRespVO;
import cn.iocoder.yudao.module.oa.api.dto.config.CollectConfigUpdateReq;
import cn.iocoder.yudao.module.oa.service.config.CollectConfigService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

@RequiredArgsConstructor
abstract class AbstractCollectConfigController {

    private final CollectConfigService collectConfigService;
    private final String scope;

    @GetMapping("/list")
    public CommonResult<PageResult<CollectConfigRespVO>> list(
            @RequestParam(required = false) String configName,
            @RequestParam(required = false) String platformType,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") Integer pageNo,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        return CommonResult.success(collectConfigService.list(scope, configName, platformType, status, pageNo, pageSize));
    }

    @PostMapping("/create")
    public CommonResult<Long> create(@Valid @RequestBody CollectConfigCreateReq req) {
        return CommonResult.success(collectConfigService.create(scope, req));
    }

    @PutMapping("/update")
    public CommonResult<Boolean> update(@Valid @RequestBody CollectConfigUpdateReq req) {
        collectConfigService.update(scope, req);
        return CommonResult.success(true);
    }

    @DeleteMapping("/delete")
    public CommonResult<Boolean> delete(@RequestParam Long id) {
        collectConfigService.delete(scope, id);
        return CommonResult.success(true);
    }
}
