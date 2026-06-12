package cn.iocoder.yudao.module.oa.controller.demo;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.module.oa.dal.dataobject.demo.OaDemoItemDO;
import cn.iocoder.yudao.module.oa.framework.dict.InDict;
import cn.iocoder.yudao.module.oa.service.demo.DemoItemService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin-api/oa/demo")
@Validated
@RequiredArgsConstructor
public class DemoController {

    private final DemoItemService demoItemService;

    @GetMapping("/items/{id}")
    public CommonResult<OaDemoItemDO> getItem(@PathVariable Long id) {
        return CommonResult.success(demoItemService.getRequired(id));
    }

    @PostMapping("/validate-dict")
    public CommonResult<Boolean> validateDict(@Valid @RequestBody DictValidateReq req) {
        return CommonResult.success(Boolean.TRUE);
    }

    @Data
    public static class DictValidateReq {
        @NotBlank
        @InDict(value = "dict_platform_type", message = "字典值不合法")
        private String platformType;
    }
}
