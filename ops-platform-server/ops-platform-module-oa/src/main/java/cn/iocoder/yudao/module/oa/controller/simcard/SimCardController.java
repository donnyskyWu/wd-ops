package cn.iocoder.yudao.module.oa.controller.simcard;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.oa.api.dto.simcard.LinkedAccountsRespVO;
import cn.iocoder.yudao.module.oa.api.dto.simcard.SimCardCreateReq;
import cn.iocoder.yudao.module.oa.api.dto.simcard.SimCardRespVO;
import cn.iocoder.yudao.module.oa.api.dto.simcard.SimCardUpdateReq;
import cn.iocoder.yudao.module.oa.service.simcard.SimCardService;
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
@RequestMapping("/admin-api/oa/sim-card")
@Validated
@RequiredArgsConstructor
public class SimCardController {

    private final SimCardService simCardService;

    @GetMapping("/list")
    public CommonResult<PageResult<SimCardRespVO>> list(
            @RequestParam(required = false) String iccid,
            @RequestParam(required = false) Long phoneId,
            @RequestParam(required = false) String operator,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") Integer pageNo,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        return CommonResult.success(simCardService.list(iccid, phoneId, operator, status, pageNo, pageSize));
    }

    @PostMapping("/create")
    public CommonResult<Long> create(@Valid @RequestBody SimCardCreateReq req) {
        return CommonResult.success(simCardService.create(req));
    }

    @PutMapping("/update")
    public CommonResult<Boolean> update(@Valid @RequestBody SimCardUpdateReq req) {
        simCardService.update(req);
        return CommonResult.success(true);
    }

    @DeleteMapping("/delete")
    public CommonResult<Boolean> delete(@RequestParam Long id) {
        simCardService.delete(id);
        return CommonResult.success(true);
    }

    @GetMapping("/{id}/linked-accounts")
    public CommonResult<LinkedAccountsRespVO> linkedAccounts(
            @PathVariable Long id,
            @RequestParam(required = false) String platformType,
            @RequestParam(required = false) String operator) {
        return CommonResult.success(simCardService.linkedAccounts(id, platformType, operator));
    }
}
