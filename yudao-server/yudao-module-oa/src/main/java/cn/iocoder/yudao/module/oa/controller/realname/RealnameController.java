package cn.iocoder.yudao.module.oa.controller.realname;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.oa.api.dto.realname.RealnameCreateReq;
import cn.iocoder.yudao.module.oa.api.dto.realname.RealnameRespVO;
import cn.iocoder.yudao.module.oa.api.dto.realname.RealnameUpdateReq;
import cn.iocoder.yudao.module.oa.service.realname.RealnameService;
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
@RequestMapping("/admin-api/oa/realname")
@Validated
@RequiredArgsConstructor
public class RealnameController {

    private final RealnameService realnameService;

    @GetMapping("/list")
    public CommonResult<PageResult<RealnameRespVO>> list(
            @RequestParam(required = false) String realName,
            @RequestParam(required = false) Long companyId,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") Integer pageNo,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        return CommonResult.success(realnameService.list(realName, companyId, status, pageNo, pageSize));
    }

    @GetMapping("/{id}")
    public CommonResult<RealnameRespVO> get(@PathVariable Long id) {
        return CommonResult.success(realnameService.get(id));
    }

    @PostMapping("/create")
    public CommonResult<Long> create(@Valid @RequestBody RealnameCreateReq req) {
        return CommonResult.success(realnameService.create(req));
    }

    @PutMapping("/update")
    public CommonResult<Boolean> update(@Valid @RequestBody RealnameUpdateReq req) {
        realnameService.update(req);
        return CommonResult.success(true);
    }

    @DeleteMapping("/delete")
    public CommonResult<Boolean> delete(@RequestParam Long id) {
        realnameService.delete(id);
        return CommonResult.success(true);
    }
}
