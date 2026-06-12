package cn.iocoder.yudao.module.oa.controller.system;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.module.oa.api.dto.system.DeptCreateReq;
import cn.iocoder.yudao.module.oa.api.dto.system.DeptTreeVO;
import cn.iocoder.yudao.module.oa.api.dto.system.DeptUpdateReq;
import cn.iocoder.yudao.module.oa.api.dto.system.DingTalkSyncResultVO;
import cn.iocoder.yudao.module.oa.service.system.DeptService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping({"/admin-api/oa/system/dept", "/admin-api/system/dept"})
@Validated
@RequiredArgsConstructor
public class DeptController {

    private final DeptService deptService;

    @GetMapping("/tree")
    @PreAuthorize("hasAuthority('oa:dept:list')")
    public CommonResult<List<DeptTreeVO>> tree() {
        return CommonResult.success(deptService.getTree());
    }

    @PostMapping("/create")
    @PreAuthorize("hasAuthority('oa:dept:create')")
    public CommonResult<Long> create(@Valid @RequestBody DeptCreateReq req) {
        return CommonResult.success(deptService.create(req));
    }

    @PutMapping("/update")
    @PreAuthorize("hasAuthority('oa:dept:update')")
    public CommonResult<Boolean> update(@Valid @RequestBody DeptUpdateReq req) {
        deptService.update(req);
        return CommonResult.success(true);
    }

    @DeleteMapping("/delete")
    @PreAuthorize("hasAuthority('oa:dept:delete')")
    public CommonResult<Boolean> delete(@RequestParam Long id) {
        deptService.delete(id);
        return CommonResult.success(true);
    }

    @PostMapping("/sync-dingtalk")
    @PreAuthorize("hasAuthority('oa:dept:sync-dingtalk')")
    public CommonResult<DingTalkSyncResultVO> syncDepartments() {
        return CommonResult.success(deptService.syncDepartmentsFromDingTalk());
    }

    @PostMapping("/sync-dingtalk-users")
    @PreAuthorize("hasAuthority('oa:user:sync-dingtalk')")
    public CommonResult<DingTalkSyncResultVO> syncUsers() {
        return CommonResult.success(deptService.syncUsersFromDingTalk());
    }
}
