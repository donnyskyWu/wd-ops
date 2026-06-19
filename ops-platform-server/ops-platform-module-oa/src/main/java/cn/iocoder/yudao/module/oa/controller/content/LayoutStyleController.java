package cn.iocoder.yudao.module.oa.controller.content;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.oa.api.dto.content.LayoutStyleCreateReq;
import cn.iocoder.yudao.module.oa.api.dto.content.LayoutStyleUpdateReq;
import cn.iocoder.yudao.module.oa.api.dto.content.LayoutStyleVO;
import cn.iocoder.yudao.module.oa.service.content.LayoutStyleService;
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
@RequestMapping("/admin-api/oa/layout-style")
@Validated
@RequiredArgsConstructor
public class LayoutStyleController {

    private final LayoutStyleService layoutStyleService;

    @GetMapping("/list")
    @PreAuthorize("hasAuthority('oa:layout-style:query')")
    public CommonResult<PageResult<LayoutStyleVO>> list(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String tags,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "50") Integer pageSize) {
        return CommonResult.success(layoutStyleService.list(name, category, tags, status, pageNum, pageSize));
    }

    @GetMapping("/enabled-list")
    @PreAuthorize("hasAuthority('oa:layout-style:query')")
    public CommonResult<List<LayoutStyleVO>> enabledList(
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String keyword) {
        return CommonResult.success(layoutStyleService.listEnabled(category, keyword));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('oa:layout-style:query')")
    public CommonResult<LayoutStyleVO> get(@PathVariable Long id) {
        return CommonResult.success(layoutStyleService.getById(id));
    }

    @PostMapping("/create")
    @PreAuthorize("hasAuthority('oa:layout-style:create')")
    public CommonResult<Long> create(@Valid @RequestBody LayoutStyleCreateReq req) {
        return CommonResult.success(layoutStyleService.create(req));
    }

    @PutMapping("/update")
    @PreAuthorize("hasAuthority('oa:layout-style:update')")
    public CommonResult<Boolean> update(@Valid @RequestBody LayoutStyleUpdateReq req) {
        layoutStyleService.update(req);
        return CommonResult.success(true);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('oa:layout-style:delete')")
    public CommonResult<Boolean> delete(@PathVariable Long id) {
        layoutStyleService.delete(id);
        return CommonResult.success(true);
    }
}
