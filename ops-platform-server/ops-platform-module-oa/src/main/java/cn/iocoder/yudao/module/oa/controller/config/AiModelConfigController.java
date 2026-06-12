package cn.iocoder.yudao.module.oa.controller.config;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.oa.api.dto.config.AiModelConfigCreateReq;
import cn.iocoder.yudao.module.oa.api.dto.config.AiModelConfigRespVO;
import cn.iocoder.yudao.module.oa.api.dto.config.AiModelConfigUpdateReq;
import cn.iocoder.yudao.module.oa.service.config.AiModelConfigService;
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
@RequestMapping("/admin-api/oa/config/ai-model")
@Validated
@RequiredArgsConstructor
public class AiModelConfigController {

    private final AiModelConfigService aiModelConfigService;

    @GetMapping("/list")
    public CommonResult<PageResult<AiModelConfigRespVO>> list(
            @RequestParam(required = false) String modelName,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") Integer pageNo,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        return CommonResult.success(aiModelConfigService.list(modelName, status, pageNo, pageSize));
    }

    @PostMapping("/create")
    public CommonResult<Long> create(@Valid @RequestBody AiModelConfigCreateReq req) {
        return CommonResult.success(aiModelConfigService.create(req));
    }

    @PutMapping("/update")
    public CommonResult<Boolean> update(@Valid @RequestBody AiModelConfigUpdateReq req) {
        aiModelConfigService.update(req);
        return CommonResult.success(true);
    }

    @DeleteMapping("/delete")
    public CommonResult<Boolean> delete(@RequestParam Long id) {
        aiModelConfigService.delete(id);
        return CommonResult.success(true);
    }

    @GetMapping("/stats")
    public CommonResult<cn.iocoder.yudao.module.oa.api.dto.config.AiModelStatsVO> stats() {
        return CommonResult.success(aiModelConfigService.stats());
    }

    @PostMapping("/test-connection")
    public CommonResult<Boolean> testConnection(@RequestBody java.util.Map<String, Object> body) {
        Long id = Long.valueOf(body.get("id").toString());
        return CommonResult.success(aiModelConfigService.testConnection(id));
    }

    @PutMapping("/set-default")
    public CommonResult<Boolean> setDefault(@RequestBody java.util.Map<String, Object> body) {
        Long id = Long.valueOf(body.get("id").toString());
        aiModelConfigService.setDefault(id);
        return CommonResult.success(true);
    }
}
