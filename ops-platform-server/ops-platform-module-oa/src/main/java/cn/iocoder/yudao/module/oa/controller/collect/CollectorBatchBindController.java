package cn.iocoder.yudao.module.oa.controller.collect;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.module.oa.api.dto.collect.CollectorBatchBindImportRespVO;
import cn.iocoder.yudao.module.oa.service.collect.CollectorBatchBindService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin-api/oa/collector-bind")
@Validated
@RequiredArgsConstructor
public class CollectorBatchBindController {

    private final CollectorBatchBindService collectorBatchBindService;

    @PostMapping("/batch-import")
    @PreAuthorize("hasAuthority('oa:account:list')")
    public CommonResult<CollectorBatchBindImportRespVO> batchImport() {
        return CommonResult.success(collectorBatchBindService.batchImport());
    }
}
