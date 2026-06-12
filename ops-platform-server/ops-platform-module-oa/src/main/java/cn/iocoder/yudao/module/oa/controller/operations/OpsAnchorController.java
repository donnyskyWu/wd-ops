package cn.iocoder.yudao.module.oa.controller.operations;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.oa.api.dto.operations.OpsAnchorCreateReq;
import cn.iocoder.yudao.module.oa.api.dto.operations.OpsAnchorRelVO;
import cn.iocoder.yudao.module.oa.api.dto.operations.OpsAnchorStatsVO;
import cn.iocoder.yudao.module.oa.api.dto.operations.OpsAnchorUpdateReq;
import cn.iocoder.yudao.module.oa.service.operations.OpsAnchorService;
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
@RequestMapping("/admin-api/oa/ops-anchor")
@Validated
@RequiredArgsConstructor
public class OpsAnchorController {

    private final OpsAnchorService opsAnchorService;

    @GetMapping("/list")
    public CommonResult<PageResult<OpsAnchorRelVO>> list(
            @RequestParam(required = false) Long opsUserId,
            @RequestParam(required = false) Long anchorUserId,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer size) {
        return CommonResult.success(opsAnchorService.list(opsUserId, anchorUserId, page, size));
    }

    @PostMapping("/create")
    public CommonResult<Long> create(@Valid @RequestBody OpsAnchorCreateReq req) {
        return CommonResult.success(opsAnchorService.create(req));
    }

    @PutMapping("/update")
    public CommonResult<Boolean> update(@Valid @RequestBody OpsAnchorUpdateReq req) {
        opsAnchorService.update(req);
        return CommonResult.success(true);
    }

    @DeleteMapping("/delete")
    public CommonResult<Boolean> delete(@RequestParam Long id) {
        opsAnchorService.delete(id);
        return CommonResult.success(true);
    }
}
