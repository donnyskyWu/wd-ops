package cn.iocoder.yudao.module.oa.controller.phone;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.oa.api.dto.phone.PhoneCreateReq;
import cn.iocoder.yudao.module.oa.api.dto.phone.PhoneRespVO;
import cn.iocoder.yudao.module.oa.api.dto.phone.PhoneUpdateReq;
import cn.iocoder.yudao.module.oa.service.phone.PhoneService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin-api/oa/phone")
@Validated
@RequiredArgsConstructor
public class PhoneController {

    private final PhoneService phoneService;

    @GetMapping("/list")
    public CommonResult<PageResult<PhoneRespVO>> list(
            @RequestParam(required = false) String phoneNumber,
            @RequestParam(required = false) Long realnameId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String phoneType,
            @RequestParam(defaultValue = "1") Integer pageNo,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        return CommonResult.success(phoneService.list(phoneNumber, realnameId, status, phoneType, pageNo, pageSize));
    }

    @PostMapping("/create")
    public CommonResult<Long> create(@Valid @RequestBody PhoneCreateReq req) {
        return CommonResult.success(phoneService.create(req));
    }

    @PutMapping("/update")
    public CommonResult<Boolean> update(@Valid @RequestBody PhoneUpdateReq req) {
        phoneService.update(req);
        return CommonResult.success(true);
    }

    @DeleteMapping("/delete")
    public CommonResult<Boolean> delete(@RequestParam Long id) {
        phoneService.delete(id);
        return CommonResult.success(true);
    }
}
