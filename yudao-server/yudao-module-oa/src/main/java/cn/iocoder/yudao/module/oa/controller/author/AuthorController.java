package cn.iocoder.yudao.module.oa.controller.author;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.oa.api.dto.author.AuthorCreateReq;
import cn.iocoder.yudao.module.oa.api.dto.author.AuthorDashboardVO;
import cn.iocoder.yudao.module.oa.api.dto.author.AuthorUpdateReq;
import cn.iocoder.yudao.module.oa.api.dto.author.AuthorVO;
import cn.iocoder.yudao.module.oa.api.dto.author.OpsUserVO;
import cn.iocoder.yudao.module.oa.service.author.AuthorService;
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

import java.util.List;

@RestController
@RequestMapping("/admin-api/oa/author")
@Validated
@RequiredArgsConstructor
public class AuthorController {

    private final AuthorService authorService;

    @GetMapping("/list")
    public CommonResult<PageResult<AuthorVO>> list(
            @RequestParam(required = false) Long ipGroupId,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer status,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer size) {
        return CommonResult.success(authorService.list(ipGroupId, keyword, status, page, size));
    }

    @PostMapping("/create")
    public CommonResult<Long> create(@Valid @RequestBody AuthorCreateReq req) {
        return CommonResult.success(authorService.create(req));
    }

    @PutMapping("/update")
    public CommonResult<Boolean> update(@Valid @RequestBody AuthorUpdateReq req) {
        authorService.update(req);
        return CommonResult.success(true);
    }

    @DeleteMapping("/delete")
    public CommonResult<Boolean> delete(@RequestParam Long id) {
        authorService.delete(id);
        return CommonResult.success(true);
    }

    @GetMapping("/{id}/dashboard")
    public CommonResult<AuthorDashboardVO> dashboard(@PathVariable Long id) {
        return CommonResult.success(authorService.dashboard(id));
    }

    @GetMapping("/{id}/ops-list")
    public CommonResult<List<OpsUserVO>> opsList(@PathVariable Long id) {
        return CommonResult.success(authorService.opsList(id));
    }
}
