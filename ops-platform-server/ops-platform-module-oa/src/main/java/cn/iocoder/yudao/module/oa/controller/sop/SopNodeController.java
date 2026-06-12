package cn.iocoder.yudao.module.oa.controller.sop;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.module.oa.api.dto.sop.DagValidateReq;
import cn.iocoder.yudao.module.oa.api.dto.sop.DagValidateResp;
import cn.iocoder.yudao.module.oa.api.dto.sop.SopNodeCreateReq;
import cn.iocoder.yudao.module.oa.api.dto.sop.SopNodeUpdateReq;
import cn.iocoder.yudao.module.oa.api.dto.sop.SopNodeVO;
import cn.iocoder.yudao.module.oa.service.sop.SopNodeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/admin-api/oa/sop/node")
@Validated
@RequiredArgsConstructor
public class SopNodeController {

    private final SopNodeService sopNodeService;

    @GetMapping("/list")
    public CommonResult<List<SopNodeVO>> list(@RequestParam Long templateId) {
        return CommonResult.success(sopNodeService.listByTemplateId(templateId));
    }

    @PostMapping("/create")
    public CommonResult<Long> create(@Valid @RequestBody SopNodeCreateReq req) {
        return CommonResult.success(sopNodeService.create(req));
    }

    @PutMapping("/update")
    public CommonResult<Boolean> update(@Valid @RequestBody SopNodeUpdateReq req) {
        sopNodeService.update(req);
        return CommonResult.success(true);
    }

    @PostMapping("/validate-dag")
    public CommonResult<DagValidateResp> validateDag(@Valid @RequestBody DagValidateReq req) {
        return CommonResult.success(sopNodeService.validateDag(req));
    }
}
