package cn.iocoder.yudao.module.oa.controller.config;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.oa.api.dto.config.AiPromptConfigCreateReq;
import cn.iocoder.yudao.module.oa.api.dto.config.AiPromptConfigRespVO;
import cn.iocoder.yudao.module.oa.api.dto.config.AiPromptConfigUpdateReq;
import cn.iocoder.yudao.module.oa.service.config.AiPromptConfigService;
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
@RequestMapping("/admin-api/oa/config/ai-prompt")
@Validated
@RequiredArgsConstructor
public class AiPromptConfigController {

    private final AiPromptConfigService aiPromptConfigService;

    @GetMapping("/list")
    public CommonResult<PageResult<AiPromptConfigRespVO>> list(
            @RequestParam(required = false) String templateName,
            @RequestParam(required = false) String scene,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") Integer pageNo,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        return CommonResult.success(aiPromptConfigService.list(templateName, scene, status, pageNo, pageSize));
    }

    @PostMapping("/create")
    public CommonResult<Long> create(@Valid @RequestBody AiPromptConfigCreateReq req) {
        return CommonResult.success(aiPromptConfigService.create(req));
    }

    @PutMapping("/update")
    public CommonResult<Boolean> update(@Valid @RequestBody AiPromptConfigUpdateReq req) {
        aiPromptConfigService.update(req);
        return CommonResult.success(true);
    }

    @DeleteMapping("/delete")
    public CommonResult<Boolean> delete(@RequestParam Long id) {
        aiPromptConfigService.delete(id);
        return CommonResult.success(true);
    }
}
