package cn.iocoder.yudao.module.oa.controller.triplerel;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.oa.api.dto.triplerel.TripleRelCreateReq;
import cn.iocoder.yudao.module.oa.api.dto.triplerel.TripleRelGraphRespVO;
import cn.iocoder.yudao.module.oa.api.dto.triplerel.TripleRelRespVO;
import cn.iocoder.yudao.module.oa.api.dto.triplerel.TripleRelStatisticsVO;
import cn.iocoder.yudao.module.oa.service.triplerel.TripleRelService;
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
@RequestMapping("/admin-api/oa/internal/triple-rel")
@Validated
@RequiredArgsConstructor
public class TripleRelController {

    private final TripleRelService tripleRelService;

    @GetMapping("/list")
    public CommonResult<PageResult<TripleRelRespVO>> list(
            @RequestParam(required = false) String relationType,
            @RequestParam(required = false) Integer status,
            @RequestParam(defaultValue = "1") Integer pageNo,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        return CommonResult.success(tripleRelService.list(relationType, status, pageNo, pageSize));
    }

    @GetMapping("/graph")
    public CommonResult<TripleRelGraphRespVO> graph(@RequestParam Long personalWechatId) {
        return CommonResult.success(tripleRelService.graph(personalWechatId));
    }

    @PostMapping("/create")
    public CommonResult<Long> create(@Valid @RequestBody TripleRelCreateReq req) {
        return CommonResult.success(tripleRelService.create(req));
    }

    @PutMapping("/unbind")
    public CommonResult<Boolean> unbind(@RequestParam Long id) {
        tripleRelService.unbind(id);
        return CommonResult.success(true);
    }

    @PutMapping("/rebind")
    public CommonResult<Boolean> rebind(@RequestParam Long id) {
        tripleRelService.rebind(id);
        return CommonResult.success(true);
    }

    @GetMapping("/statistics")
    public CommonResult<TripleRelStatisticsVO> statistics() {
        return CommonResult.success(tripleRelService.statistics());
    }
}
