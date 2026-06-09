package cn.iocoder.yudao.module.oa.controller.personal;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.oa.api.dto.personal.PersonalWechatApiConfigReq;
import cn.iocoder.yudao.module.oa.api.dto.personal.PersonalWechatCreateReq;
import cn.iocoder.yudao.module.oa.api.dto.personal.PersonalWechatRespVO;
import cn.iocoder.yudao.module.oa.api.dto.personal.PersonalWechatUpdateReq;
import cn.iocoder.yudao.module.oa.service.personal.PersonalWechatAccountService;
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
@RequestMapping("/admin-api/oa/internal/personal-account")
@Validated
@RequiredArgsConstructor
public class PersonalWechatAccountController {

    private final PersonalWechatAccountService personalWechatAccountService;

    @GetMapping("/list")
    public CommonResult<PageResult<PersonalWechatRespVO>> list(
            @RequestParam(required = false) String accountName,
            @RequestParam(required = false) String wechatId,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") Integer pageNo,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        return CommonResult.success(personalWechatAccountService.list(
                accountName, wechatId, status, pageNo, pageSize));
    }

    @GetMapping("/{id}")
    public CommonResult<PersonalWechatRespVO> get(@PathVariable Long id) {
        return CommonResult.success(personalWechatAccountService.get(id));
    }

    @PostMapping("/create")
    public CommonResult<Long> create(@Valid @RequestBody PersonalWechatCreateReq req) {
        return CommonResult.success(personalWechatAccountService.create(req));
    }

    @PutMapping("/update")
    public CommonResult<Boolean> update(@Valid @RequestBody PersonalWechatUpdateReq req) {
        personalWechatAccountService.update(req);
        return CommonResult.success(true);
    }

    @DeleteMapping("/delete")
    public CommonResult<Boolean> delete(@RequestParam Long id) {
        personalWechatAccountService.delete(id);
        return CommonResult.success(true);
    }

    @PostMapping("/api-config")
    public CommonResult<Boolean> saveApiConfig(@Valid @RequestBody PersonalWechatApiConfigReq req) {
        personalWechatAccountService.saveApiConfig(req);
        return CommonResult.success(true);
    }

    @GetMapping("/api-config/{id}")
    public CommonResult<PersonalWechatRespVO> getApiConfig(@PathVariable Long id) {
        return CommonResult.success(personalWechatAccountService.getApiConfig(id));
    }
}
