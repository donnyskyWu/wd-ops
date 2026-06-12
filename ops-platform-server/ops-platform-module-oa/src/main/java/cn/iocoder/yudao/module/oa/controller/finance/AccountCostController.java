package cn.iocoder.yudao.module.oa.controller.finance;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.oa.api.dto.finance.AccountCostCreateReq;
import cn.iocoder.yudao.module.oa.api.dto.finance.AccountCostUpdateReq;
import cn.iocoder.yudao.module.oa.api.dto.finance.AccountCostVO;
import cn.iocoder.yudao.module.oa.service.finance.AccountCostService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
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

import java.time.LocalDate;

@RestController
@RequestMapping("/admin-api/oa/finance/cost")
@Validated
@RequiredArgsConstructor
public class AccountCostController {

    private final AccountCostService accountCostService;

    @GetMapping("/list")
    public CommonResult<PageResult<AccountCostVO>> list(
            @RequestParam(required = false) Long accountId,
            @RequestParam(required = false) String costType,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "20") Integer pageSize) {
        return CommonResult.success(accountCostService.list(accountId, costType, startDate, endDate, pageNum, pageSize));
    }

    @PostMapping("/create")
    public CommonResult<Long> create(@Valid @RequestBody AccountCostCreateReq req) {
        return CommonResult.success(accountCostService.create(req));
    }

    @PutMapping("/update")
    public CommonResult<Boolean> update(@Valid @RequestBody AccountCostUpdateReq req) {
        accountCostService.update(req);
        return CommonResult.success(true);
    }

    @DeleteMapping("/{id}")
    public CommonResult<Boolean> delete(@PathVariable Long id) {
        accountCostService.delete(id);
        return CommonResult.success(true);
    }
}
