package cn.iocoder.yudao.module.oa.controller.plan;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.oa.api.dto.plan.ContentPlanCreateReq;
import cn.iocoder.yudao.module.oa.api.dto.plan.ContentPlanRespVO;
import cn.iocoder.yudao.module.oa.api.dto.plan.ContentPlanTerminateReq;
import cn.iocoder.yudao.module.oa.service.plan.ContentPlanService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin-api/oa/plan")
@Validated
@RequiredArgsConstructor
public class ContentPlanController {

    private final ContentPlanService contentPlanService;

    @GetMapping("/list")
    public CommonResult<PageResult<ContentPlanRespVO>> list(
            @RequestParam(required = false) String planName,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") Integer pageNo,
            @RequestParam(defaultValue = "20") Integer pageSize) {
        return CommonResult.success(contentPlanService.list(planName, status, pageNo, pageSize));
    }

    @GetMapping("/get")
    public CommonResult<ContentPlanRespVO> get(@RequestParam Long id) {
        return CommonResult.success(contentPlanService.get(id));
    }

    @PostMapping("/create")
    public CommonResult<Long> create(@Valid @RequestBody ContentPlanCreateReq req) {
        return CommonResult.success(contentPlanService.create(req));
    }

    @PostMapping("/{id}/start")
    public CommonResult<Boolean> start(@PathVariable Long id) {
        contentPlanService.start(id);
        return CommonResult.success(true);
    }

    @PostMapping("/{id}/terminate")
    public CommonResult<Boolean> submitTerminate(@PathVariable Long id,
                                                 @RequestBody(required = false) ContentPlanTerminateReq req) {
        contentPlanService.submitTerminate(id, req);
        return CommonResult.success(true);
    }

    @PostMapping("/{id}/terminate/approve")
    public CommonResult<Boolean> approveTerminate(@PathVariable Long id) {
        contentPlanService.approveTerminate(id);
        return CommonResult.success(true);
    }

    @PostMapping("/{id}/terminate/reject")
    public CommonResult<Boolean> rejectTerminate(@PathVariable Long id) {
        contentPlanService.rejectTerminate(id);
        return CommonResult.success(true);
    }

    @DeleteMapping("/delete")
    public CommonResult<Boolean> delete(@RequestParam Long id) {
        contentPlanService.delete(id);
        return CommonResult.success(true);
    }
}
