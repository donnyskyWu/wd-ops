package cn.iocoder.yudao.module.oa.controller.company;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.oa.api.dto.company.CompanyCreateReq;
import cn.iocoder.yudao.module.oa.api.dto.company.CompanyExpandReq;
import cn.iocoder.yudao.module.oa.api.dto.company.CompanyMpStatsRespVO;
import cn.iocoder.yudao.module.oa.api.dto.company.CompanyRespVO;
import cn.iocoder.yudao.module.oa.api.dto.company.CompanyUpdateReq;
import cn.iocoder.yudao.module.oa.service.company.CompanyService;
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
@RequestMapping("/admin-api/oa/company")
@Validated
@RequiredArgsConstructor
public class CompanyController {

    private final CompanyService companyService;

    @GetMapping("/list")
    public CommonResult<PageResult<CompanyRespVO>> list(
            @RequestParam(required = false) String companyName,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") Integer pageNo,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        return CommonResult.success(companyService.list(companyName, status, pageNo, pageSize));
    }

    @GetMapping("/{id}")
    public CommonResult<CompanyRespVO> get(@PathVariable Long id) {
        return CommonResult.success(companyService.get(id));
    }

    @PostMapping("/create")
    public CommonResult<Long> create(@Valid @RequestBody CompanyCreateReq req) {
        return CommonResult.success(companyService.create(req));
    }

    @PutMapping("/update")
    public CommonResult<Boolean> update(@Valid @RequestBody CompanyUpdateReq req) {
        companyService.update(req);
        return CommonResult.success(true);
    }

    @DeleteMapping("/delete")
    public CommonResult<Boolean> delete(@RequestParam Long id) {
        companyService.delete(id);
        return CommonResult.success(true);
    }

    @PostMapping("/{id}/expand")
    public CommonResult<Boolean> expand(@PathVariable Long id, @Valid @RequestBody CompanyExpandReq req) {
        companyService.expand(id, req);
        return CommonResult.success(true);
    }

    @GetMapping("/{id}/mp-stats")
    public CommonResult<CompanyMpStatsRespVO> mpStats(@PathVariable Long id) {
        return CommonResult.success(companyService.mpStats(id));
    }
}
