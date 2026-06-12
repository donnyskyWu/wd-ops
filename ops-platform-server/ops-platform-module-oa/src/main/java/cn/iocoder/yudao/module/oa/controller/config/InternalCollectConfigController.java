package cn.iocoder.yudao.module.oa.controller.config;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.module.oa.api.dto.config.AoCreateApiReq;
import cn.iocoder.yudao.module.oa.api.dto.config.AoCreateApiRespVO;
import cn.iocoder.yudao.module.oa.service.config.AoCreateApiService;
import cn.iocoder.yudao.module.oa.service.config.CollectConfigScope;
import cn.iocoder.yudao.module.oa.service.config.CollectConfigService;
import jakarta.validation.Valid;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin-api/oa/config/internal-collect")
@Validated
public class InternalCollectConfigController extends AbstractCollectConfigController {

    private final AoCreateApiService aoCreateApiService;

    public InternalCollectConfigController(CollectConfigService collectConfigService,
                                           AoCreateApiService aoCreateApiService) {
        super(collectConfigService, CollectConfigScope.INTERNAL);
        this.aoCreateApiService = aoCreateApiService;
    }

    @GetMapping("/aocreate")
    public CommonResult<AoCreateApiRespVO> getAoCreate() {
        return CommonResult.success(aoCreateApiService.get());
    }

    @PostMapping("/aocreate")
    public CommonResult<Boolean> saveAoCreate(@Valid @RequestBody AoCreateApiReq req) {
        aoCreateApiService.save(req);
        return CommonResult.success(true);
    }
}
