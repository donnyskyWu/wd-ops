package cn.iocoder.yudao.module.oa.controller.realname;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.module.oa.api.dto.realname.IntermediaryCreateReq;
import cn.iocoder.yudao.module.oa.api.dto.realname.IntermediaryRespVO;
import cn.iocoder.yudao.module.oa.api.dto.realname.IntermediaryUpdateReq;
import cn.iocoder.yudao.module.oa.service.realname.RealnameIntermediaryService;
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
@RequestMapping("/admin-api/oa/realname")
@Validated
@RequiredArgsConstructor
public class RealnameIntermediaryController {

    private final RealnameIntermediaryService intermediaryService;

    @GetMapping("/{realnameId}/intermediaries")
    public CommonResult<List<IntermediaryRespVO>> list(@PathVariable Long realnameId) {
        return CommonResult.success(intermediaryService.listByRealname(realnameId));
    }

    @PostMapping("/{realnameId}/intermediary")
    public CommonResult<Long> create(@PathVariable Long realnameId,
                                     @Valid @RequestBody IntermediaryCreateReq req) {
        return CommonResult.success(intermediaryService.create(realnameId, req));
    }

    @PutMapping("/intermediary/update")
    public CommonResult<Boolean> update(@Valid @RequestBody IntermediaryUpdateReq req) {
        intermediaryService.update(req);
        return CommonResult.success(true);
    }

    @DeleteMapping("/intermediary/delete")
    public CommonResult<Boolean> delete(@RequestParam Long id) {
        intermediaryService.delete(id);
        return CommonResult.success(true);
    }
}
