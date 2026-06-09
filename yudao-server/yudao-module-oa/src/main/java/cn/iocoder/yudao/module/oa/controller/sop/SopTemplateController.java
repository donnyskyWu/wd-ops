package cn.iocoder.yudao.module.oa.controller.sop;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.oa.api.dto.sop.SopTemplateCreateReq;
import cn.iocoder.yudao.module.oa.api.dto.sop.SopTemplateUpdateReq;
import cn.iocoder.yudao.module.oa.api.dto.sop.SopTemplateVO;
import cn.iocoder.yudao.module.oa.service.sop.SopTemplateService;
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
@RequestMapping("/admin-api/oa/sop/template")
@Validated
@RequiredArgsConstructor
public class SopTemplateController {

    private final SopTemplateService sopTemplateService;

    @GetMapping("/list")
    public CommonResult<PageResult<SopTemplateVO>> list(
            @RequestParam(required = false) String templateName,
            @RequestParam(required = false) String contentType,
            @RequestParam(required = false) String platformType,
            @RequestParam(required = false) Integer status,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "20") Integer pageSize) {
        return CommonResult.success(sopTemplateService.list(templateName, contentType, platformType, status, pageNum, pageSize));
    }

    @PostMapping("/create")
    public CommonResult<Long> create(@Valid @RequestBody SopTemplateCreateReq req) {
        return CommonResult.success(sopTemplateService.create(req));
    }

    @PutMapping("/update")
    public CommonResult<Boolean> update(@Valid @RequestBody SopTemplateUpdateReq req) {
        sopTemplateService.update(req);
        return CommonResult.success(true);
    }

    @DeleteMapping("/{id}")
    public CommonResult<Boolean> delete(@PathVariable Long id) {
        sopTemplateService.delete(id);
        return CommonResult.success(true);
    }
}
