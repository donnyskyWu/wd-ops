package cn.iocoder.yudao.module.oa.controller.content;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.oa.api.dto.content.TypesettingRuleCreateReq;
import cn.iocoder.yudao.module.oa.api.dto.content.TypesettingRuleUpdateReq;
import cn.iocoder.yudao.module.oa.api.dto.content.TypesettingRuleVO;
import cn.iocoder.yudao.module.oa.service.content.TypesettingRuleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
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

import java.util.List;

@RestController
@RequestMapping("/admin-api/oa/typesetting-rule")
@Validated
@RequiredArgsConstructor
public class TypesettingRuleController {

    private final TypesettingRuleService typesettingRuleService;

    @GetMapping("/list")
    @PreAuthorize("hasAuthority('oa:typesetting-rule:query')")
    public CommonResult<PageResult<TypesettingRuleVO>> list(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "20") Integer pageSize) {
        return CommonResult.success(typesettingRuleService.list(name, status, pageNum, pageSize));
    }

    @GetMapping("/enabled-list")
    @PreAuthorize("hasAuthority('oa:typesetting-rule:query')")
    public CommonResult<List<TypesettingRuleVO>> enabledList() {
        return CommonResult.success(typesettingRuleService.listEnabled());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('oa:typesetting-rule:query')")
    public CommonResult<TypesettingRuleVO> get(@PathVariable Long id) {
        return CommonResult.success(typesettingRuleService.getById(id));
    }

    @PostMapping("/create")
    @PreAuthorize("hasAuthority('oa:typesetting-rule:create')")
    public CommonResult<Long> create(@Valid @RequestBody TypesettingRuleCreateReq req) {
        return CommonResult.success(typesettingRuleService.create(req));
    }

    @PutMapping("/update")
    @PreAuthorize("hasAuthority('oa:typesetting-rule:update')")
    public CommonResult<Boolean> update(@Valid @RequestBody TypesettingRuleUpdateReq req) {
        typesettingRuleService.update(req);
        return CommonResult.success(true);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('oa:typesetting-rule:delete')")
    public CommonResult<Boolean> delete(@PathVariable Long id) {
        typesettingRuleService.delete(id);
        return CommonResult.success(true);
    }
}
