package cn.iocoder.yudao.module.oa.controller.collect;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.oa.api.dto.collect.CollectLogRespVO;
import cn.iocoder.yudao.module.oa.service.collect.CollectLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin-api/oa/collect/log")
@Validated
@RequiredArgsConstructor
public class CollectLogController {

    private final CollectLogService collectLogService;

    @GetMapping("/page")
    public CommonResult<PageResult<CollectLogRespVO>> page(
            @RequestParam(required = false) Long taskId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(defaultValue = "1") Integer pageNo,
            @RequestParam(defaultValue = "20") Integer pageSize) {
        return CommonResult.success(collectLogService.page(taskId, status, startDate, endDate, pageNo, pageSize));
    }
}
