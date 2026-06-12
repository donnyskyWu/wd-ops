package cn.iocoder.yudao.module.oa.controller.operations;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.module.oa.api.dto.operations.OpsAnchorStatsVO;
import cn.iocoder.yudao.module.oa.service.operations.OpsAnchorService;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin-api/oa/ops")
@Validated
@RequiredArgsConstructor
public class OpsStatsController {

    private final OpsAnchorService opsAnchorService;

    @GetMapping("/{userId}/anchor-stats")
    public CommonResult<OpsAnchorStatsVO> anchorStats(@PathVariable Long userId) {
        return CommonResult.success(opsAnchorService.anchorStats(userId));
    }
}
