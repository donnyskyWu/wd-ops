package cn.iocoder.yudao.module.oa.controller.match;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.oa.api.dto.match.MatchLeagueVO;
import cn.iocoder.yudao.module.oa.api.dto.match.MatchVO;
import cn.iocoder.yudao.module.oa.service.match.MatchProxyService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 外部赛事 API 代理（租户无关，ADR-016 / BLK-M2-004 已决）。
 */
@RestController
@RequestMapping("/admin-api/oa/match")
@RequiredArgsConstructor
public class MatchController {

    private final MatchProxyService matchProxyService;

    @GetMapping("/list")
    public CommonResult<PageResult<MatchVO>> list(
            @RequestParam(required = false) String date,
            @RequestParam(defaultValue = "1") Integer pageNo,
            @RequestParam(defaultValue = "20") Integer pageSize,
            @RequestParam(required = false) String leagueId,
            @RequestParam(required = false) String teamKeyword,
            @RequestParam(required = false) String lotteryType) {
        return CommonResult.success(matchProxyService.listMatches(
                date, pageNo, pageSize, leagueId, teamKeyword, lotteryType));
    }

    @GetMapping("/leagues")
    public CommonResult<List<MatchLeagueVO>> leagues() {
        return CommonResult.success(matchProxyService.listLeagues());
    }
}
