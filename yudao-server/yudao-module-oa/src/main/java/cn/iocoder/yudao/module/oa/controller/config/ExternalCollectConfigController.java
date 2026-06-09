package cn.iocoder.yudao.module.oa.controller.config;

import cn.iocoder.yudao.module.oa.service.config.CollectConfigScope;
import cn.iocoder.yudao.module.oa.service.config.CollectConfigService;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin-api/oa/config/external-collect")
public class ExternalCollectConfigController extends AbstractCollectConfigController {

    public ExternalCollectConfigController(CollectConfigService collectConfigService) {
        super(collectConfigService, CollectConfigScope.EXTERNAL);
    }
}
