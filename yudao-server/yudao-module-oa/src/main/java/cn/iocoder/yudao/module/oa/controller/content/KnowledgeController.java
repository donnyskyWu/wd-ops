package cn.iocoder.yudao.module.oa.controller.content;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.oa.api.dto.content.KnowledgeCreateReq;
import cn.iocoder.yudao.module.oa.api.dto.content.KnowledgeLikeReq;
import cn.iocoder.yudao.module.oa.api.dto.content.KnowledgeUpdateReq;
import cn.iocoder.yudao.module.oa.api.dto.content.KnowledgeVO;
import cn.iocoder.yudao.module.oa.service.content.KnowledgeBaseService;
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
@RequestMapping("/admin-api/oa/knowledge")
@Validated
@RequiredArgsConstructor
public class KnowledgeController {

    private final KnowledgeBaseService knowledgeBaseService;

    @GetMapping("/list")
    public CommonResult<PageResult<KnowledgeVO>> list(
            @RequestParam(required = false) String title,
            @RequestParam(required = false) String category,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "20") Integer pageSize) {
        return CommonResult.success(knowledgeBaseService.list(title, category, pageNum, pageSize));
    }

    @GetMapping("/{id}")
    public CommonResult<KnowledgeVO> getById(@PathVariable Long id) {
        return CommonResult.success(knowledgeBaseService.getById(id));
    }

    @PostMapping("/create")
    public CommonResult<Long> create(@Valid @RequestBody KnowledgeCreateReq req) {
        return CommonResult.success(knowledgeBaseService.create(req));
    }

    @PutMapping("/update")
    public CommonResult<Boolean> update(@Valid @RequestBody KnowledgeUpdateReq req) {
        knowledgeBaseService.update(req);
        return CommonResult.success(true);
    }

    @DeleteMapping("/delete")
    public CommonResult<Boolean> delete(@RequestParam Long id) {
        knowledgeBaseService.delete(id);
        return CommonResult.success(true);
    }

    @PostMapping("/like")
    public CommonResult<Boolean> like(@Valid @RequestBody KnowledgeLikeReq req) {
        knowledgeBaseService.toggleLike(req);
        return CommonResult.success(true);
    }
}
