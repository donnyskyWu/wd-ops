package cn.iocoder.yudao.module.oa.controller.analytics;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.oa.api.dto.analytics.CustomQueryCreateReq;
import cn.iocoder.yudao.module.oa.api.dto.analytics.CustomQueryPreviewReq;
import cn.iocoder.yudao.module.oa.api.dto.analytics.CustomQueryUpdateReq;
import cn.iocoder.yudao.module.oa.dal.dataobject.analytics.CustomQueryDO;
import cn.iocoder.yudao.module.oa.service.analytics.CustomQueryService;
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

import java.util.Map;

@RestController
@RequestMapping("/admin-api/oa/query")
@Validated
@RequiredArgsConstructor
public class CustomQueryController {

    private final CustomQueryService customQueryService;

    @GetMapping("/list")
    public CommonResult<PageResult<CustomQueryDO>> list(
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "20") Integer pageSize) {
        return CommonResult.success(customQueryService.list(status, pageNum, pageSize));
    }

    @PostMapping("/create")
    public CommonResult<Long> create(@Valid @RequestBody CustomQueryCreateReq req) {
        return CommonResult.success(customQueryService.create(req));
    }

    @PutMapping("/update")
    public CommonResult<Boolean> update(@Valid @RequestBody CustomQueryUpdateReq req) {
        customQueryService.update(req);
        return CommonResult.success(true);
    }

    @PostMapping("/preview")
    public CommonResult<Map<String, Object>> preview(@Valid @RequestBody CustomQueryPreviewReq req) {
        return CommonResult.success(customQueryService.preview(req));
    }

    @PostMapping("/{id}/execute")
    public CommonResult<Map<String, Object>> execute(@PathVariable Long id) {
        return CommonResult.success(customQueryService.execute(id));
    }

    @PostMapping("/{id}/publish")
    public CommonResult<Boolean> publish(@PathVariable Long id) {
        customQueryService.publish(id);
        return CommonResult.success(true);
    }
}
