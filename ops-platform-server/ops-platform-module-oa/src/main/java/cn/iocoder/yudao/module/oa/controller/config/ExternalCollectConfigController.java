package cn.iocoder.yudao.module.oa.controller.config;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.oa.api.dto.config.ImportResultVO;
import cn.iocoder.yudao.module.oa.api.dto.config.KeywordConfigCreateReq;
import cn.iocoder.yudao.module.oa.api.dto.config.KeywordConfigRespVO;
import cn.iocoder.yudao.module.oa.api.dto.config.KeywordConfigUpdateReq;
import cn.iocoder.yudao.module.oa.service.config.CollectConfigScope;
import cn.iocoder.yudao.module.oa.service.config.CollectConfigService;
import cn.iocoder.yudao.module.oa.service.config.KeywordConfigService;
import jakarta.validation.Valid;
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
@RequestMapping("/admin-api/oa/config/external-collect")
@Validated
public class ExternalCollectConfigController extends AbstractCollectConfigController {

    private final KeywordConfigService keywordConfigService;

    public ExternalCollectConfigController(CollectConfigService collectConfigService,
                                           KeywordConfigService keywordConfigService) {
        super(collectConfigService, CollectConfigScope.EXTERNAL);
        this.keywordConfigService = keywordConfigService;
    }

    @PostMapping("/import")
    public CommonResult<ImportResultVO> importCsv(@RequestBody java.util.Map<String, String> body) {
        return CommonResult.success(collectConfigService.importExternalAccounts(body.get("content")));
    }

    @GetMapping("/keyword/list")
    public CommonResult<PageResult<KeywordConfigRespVO>> keywordList(
            @RequestParam(required = false) String platform,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") Integer pageNo,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        return CommonResult.success(keywordConfigService.list(platform, keyword, status, pageNo, pageSize));
    }

    @PostMapping("/keyword/create")
    public CommonResult<Long> keywordCreate(@Valid @RequestBody KeywordConfigCreateReq req) {
        return CommonResult.success(keywordConfigService.create(req));
    }

    @PutMapping("/keyword/update")
    public CommonResult<Boolean> keywordUpdate(@Valid @RequestBody KeywordConfigUpdateReq req) {
        keywordConfigService.update(req);
        return CommonResult.success(true);
    }

    @DeleteMapping("/keyword/delete")
    public CommonResult<Boolean> keywordDelete(@RequestParam Long id) {
        keywordConfigService.delete(id);
        return CommonResult.success(true);
    }
}
