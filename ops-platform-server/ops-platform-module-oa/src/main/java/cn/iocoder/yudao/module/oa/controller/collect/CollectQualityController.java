package cn.iocoder.yudao.module.oa.controller.collect;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.oa.api.dto.collect.CollectQualityCheckRespVO;
import cn.iocoder.yudao.module.oa.api.dto.collect.CollectQualityLogRespVO;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collections;

/**
 * M10 数据质量 API（API-M10 §2）。
 * v1 stub：返回空分页；后续接 oa_collect_quality_* 表。
 */
@RestController
@RequestMapping("/admin-api/oa/collect/quality")
@Validated
public class CollectQualityController {

    @GetMapping({"/list", "/check/page"})
    public CommonResult<PageResult<CollectQualityCheckRespVO>> list(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String checkType,
            @RequestParam(required = false) String level,
            @RequestParam(defaultValue = "1") Integer pageNo,
            @RequestParam(defaultValue = "20") Integer pageSize) {
        return CommonResult.success(new PageResult<>(Collections.emptyList(), 0L));
    }

    @GetMapping({"/log", "/log/page"})
    public CommonResult<PageResult<CollectQualityLogRespVO>> log(
            @RequestParam(required = false) String level,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(defaultValue = "1") Integer pageNo,
            @RequestParam(defaultValue = "20") Integer pageSize) {
        return CommonResult.success(new PageResult<>(Collections.emptyList(), 0L));
    }
}
