package cn.iocoder.yudao.module.oa.controller.collect;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.oa.api.dto.collect.CollectTaskCreateReq;
import cn.iocoder.yudao.module.oa.api.dto.collect.CollectTaskRespVO;
import cn.iocoder.yudao.module.oa.api.dto.collect.CollectTaskUpdateReq;
import cn.iocoder.yudao.module.oa.service.collect.CollectTaskService;
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
@RequestMapping("/admin-api/oa/collect/task")
@Validated
@RequiredArgsConstructor
public class CollectTaskController {

    private final CollectTaskService collectTaskService;

    @GetMapping("/page")
    public CommonResult<PageResult<CollectTaskRespVO>> page(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String platformType,
            @RequestParam(required = false) String method,
            @RequestParam(required = false) String frequency,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") Integer pageNo,
            @RequestParam(defaultValue = "20") Integer pageSize) {
        return CommonResult.success(collectTaskService.page(name, platformType, method, frequency, status, pageNo, pageSize));
    }

    @GetMapping("/{id}")
    public CommonResult<CollectTaskRespVO> get(@PathVariable Long id) {
        return CommonResult.success(collectTaskService.get(id));
    }

    @PostMapping("/create")
    public CommonResult<Long> create(@Valid @RequestBody CollectTaskCreateReq req) {
        return CommonResult.success(collectTaskService.create(req));
    }

    @PutMapping("/update")
    public CommonResult<Boolean> update(@Valid @RequestBody CollectTaskUpdateReq req) {
        collectTaskService.update(req);
        return CommonResult.success(true);
    }

    @DeleteMapping("/delete")
    public CommonResult<Boolean> delete(@RequestParam Long id) {
        collectTaskService.delete(id);
        return CommonResult.success(true);
    }

    @PostMapping("/{id}/run")
    public CommonResult<Boolean> run(@PathVariable Long id) {
        collectTaskService.run(id);
        return CommonResult.success(true);
    }

    @PutMapping("/{id}/status")
    public CommonResult<Boolean> updateStatus(@PathVariable Long id, @RequestParam String status) {
        collectTaskService.updateStatus(id, status);
        return CommonResult.success(true);
    }
}
