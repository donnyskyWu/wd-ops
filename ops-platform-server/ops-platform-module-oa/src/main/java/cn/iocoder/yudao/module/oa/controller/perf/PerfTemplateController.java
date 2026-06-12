package cn.iocoder.yudao.module.oa.controller.perf;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.oa.api.dto.perf.PerfTemplateActivateReq;
import cn.iocoder.yudao.module.oa.api.dto.perf.PerfTemplateCreateReq;
import cn.iocoder.yudao.module.oa.api.dto.perf.PerfTemplateItemsVO;
import cn.iocoder.yudao.module.oa.api.dto.perf.PerfTemplateUpdateReq;
import cn.iocoder.yudao.module.oa.api.dto.perf.PerfTemplateVO;
import cn.iocoder.yudao.module.oa.service.perf.PerfTemplateService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin-api/oa/perf/template")
@Validated
@RequiredArgsConstructor
public class PerfTemplateController {

    private final PerfTemplateService perfTemplateService;

    @GetMapping("/list")
    public CommonResult<PageResult<PerfTemplateVO>> list(
            @RequestParam(required = false) String position,
            @RequestParam(required = false) Integer isActive,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "20") Integer pageSize) {
        return CommonResult.success(perfTemplateService.list(position, isActive, pageNum, pageSize));
    }

    @PostMapping("/create")
    public CommonResult<Long> create(@Valid @RequestBody PerfTemplateCreateReq req) {
        return CommonResult.success(perfTemplateService.create(req));
    }

    @PutMapping("/update")
    public CommonResult<Boolean> update(@Valid @RequestBody PerfTemplateUpdateReq req) {
        perfTemplateService.update(req);
        return CommonResult.success(true);
    }

    @PostMapping("/activate")
    public CommonResult<Boolean> activate(@Valid @RequestBody PerfTemplateActivateReq req) {
        perfTemplateService.activate(req);
        return CommonResult.success(true);
    }

    @GetMapping("/{id}/items")
    public CommonResult<PerfTemplateItemsVO> items(@PathVariable Long id) {
        return CommonResult.success(perfTemplateService.getItems(id));
    }
}
