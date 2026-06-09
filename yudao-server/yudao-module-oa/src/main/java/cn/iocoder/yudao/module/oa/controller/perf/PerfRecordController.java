package cn.iocoder.yudao.module.oa.controller.perf;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.oa.api.dto.perf.PerfRecordAdjustReq;
import cn.iocoder.yudao.module.oa.api.dto.perf.PerfRecordCalculateReq;
import cn.iocoder.yudao.module.oa.api.dto.perf.PerfRecordConfirmReq;
import cn.iocoder.yudao.module.oa.api.dto.perf.PerfRecordCreateReq;
import cn.iocoder.yudao.module.oa.api.dto.perf.PerfRecordDetailVO;
import cn.iocoder.yudao.module.oa.api.dto.perf.PerfRecordVO;
import cn.iocoder.yudao.module.oa.service.perf.PerfRecordService;
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

@RestController
@RequestMapping("/admin-api/oa/perf/record")
@Validated
@RequiredArgsConstructor
public class PerfRecordController {

    private final PerfRecordService perfRecordService;

    @GetMapping("/list")
    public CommonResult<PageResult<PerfRecordVO>> list(
            @RequestParam(required = false) Long ipGroupId,
            @RequestParam(required = false) Long targetUserId,
            @RequestParam(required = false) String periodType,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "20") Integer pageSize) {
        return CommonResult.success(perfRecordService.list(ipGroupId, targetUserId, periodType, status, pageNum, pageSize));
    }

    @PostMapping("/create")
    public CommonResult<Long> create(@Valid @RequestBody PerfRecordCreateReq req) {
        return CommonResult.success(perfRecordService.create(req));
    }

    @PostMapping("/calculate")
    public CommonResult<Boolean> calculate(@Valid @RequestBody PerfRecordCalculateReq req) {
        perfRecordService.calculate(req);
        return CommonResult.success(true);
    }

    @PutMapping("/adjust")
    public CommonResult<Boolean> adjust(@Valid @RequestBody PerfRecordAdjustReq req) {
        perfRecordService.adjust(req);
        return CommonResult.success(true);
    }

    @GetMapping("/detail")
    public CommonResult<PerfRecordDetailVO> detail(@RequestParam Long id) {
        return CommonResult.success(perfRecordService.detail(id));
    }

    @PostMapping("/confirm")
    public CommonResult<Boolean> confirm(@Valid @RequestBody PerfRecordConfirmReq req) {
        perfRecordService.confirm(req);
        return CommonResult.success(true);
    }
}
