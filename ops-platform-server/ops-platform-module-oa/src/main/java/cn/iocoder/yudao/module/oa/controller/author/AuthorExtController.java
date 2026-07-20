package cn.iocoder.yudao.module.oa.controller.author;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.module.oa.api.dto.author.AuthorExtUpdateReq;
import cn.iocoder.yudao.module.oa.api.dto.author.AuthorExtVO;
import cn.iocoder.yudao.module.oa.service.author.AuthorService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin-api/oa/author-ext")
@Validated
@RequiredArgsConstructor
public class AuthorExtController {

    private final AuthorService authorService;

    @GetMapping("/{authorUserId}")
    public CommonResult<AuthorExtVO> get(@PathVariable Long authorUserId) {
        return CommonResult.success(authorService.getExt(authorUserId));
    }

    @PutMapping("/{authorUserId}")
    public CommonResult<Boolean> update(@PathVariable Long authorUserId,
                                        @Valid @RequestBody AuthorExtUpdateReq req) {
        authorService.updateExt(authorUserId, req);
        return CommonResult.success(true);
    }
}
