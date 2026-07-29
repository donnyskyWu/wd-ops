package cn.iocoder.yudao.module.oa.controller.system;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.module.oa.api.dto.system.DeptCreateReq;
import cn.iocoder.yudao.module.oa.api.dto.system.DeptTreeVO;
import cn.iocoder.yudao.module.oa.api.dto.system.DeptUpdateReq;
import cn.iocoder.yudao.module.oa.api.dto.system.DingTalkSyncResultVO;
import cn.iocoder.yudao.module.oa.service.system.ParallelSystemDeprecatedSupport;
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

/**
 * Parallel OPS system dept management — superseded by Football Admin (D-DEDUP-01).
 * DingTalk sync endpoints Out of Scope (D-DING-02); C-WP0 → 410.
 */
@Deprecated(since = "9.1.0", forRemoval = true)
@RestController
@RequestMapping({"/admin-api/oa/system/dept", "/admin-api/system/dept"})
@Validated
@RequiredArgsConstructor
public class DeptController {

    @GetMapping("/tree")
    @PreAuthorize("hasAuthority('oa:dept:list')")
    public CommonResult<List<DeptTreeVO>> tree() {
        ParallelSystemDeprecatedSupport.throwParallelSystemDeprecated();
        return null;
    }

    @PostMapping("/create")
    @PreAuthorize("hasAuthority('oa:dept:create')")
    public CommonResult<Long> create(@Valid @RequestBody DeptCreateReq req) {
        ParallelSystemDeprecatedSupport.throwParallelSystemDeprecated();
        return null;
    }

    @PutMapping("/update")
    @PreAuthorize("hasAuthority('oa:dept:update')")
    public CommonResult<Boolean> update(@Valid @RequestBody DeptUpdateReq req) {
        ParallelSystemDeprecatedSupport.throwParallelSystemDeprecated();
        return null;
    }

    @DeleteMapping("/delete")
    @PreAuthorize("hasAuthority('oa:dept:delete')")
    public CommonResult<Boolean> delete(@RequestParam Long id) {
        ParallelSystemDeprecatedSupport.throwParallelSystemDeprecated();
        return null;
    }

    @Deprecated(since = "9.1.0", forRemoval = true)
    @PostMapping("/sync-dingtalk")
    @PreAuthorize("hasAuthority('oa:dept:sync-dingtalk')")
    public CommonResult<DingTalkSyncResultVO> syncDepartments() {
        ParallelSystemDeprecatedSupport.throwDingTalkSyncDeprecated();
        return null;
    }

    @Deprecated(since = "9.1.0", forRemoval = true)
    @PostMapping("/sync-dingtalk-users")
    @PreAuthorize("hasAuthority('oa:user:sync-dingtalk')")
    public CommonResult<DingTalkSyncResultVO> syncUsers() {
        ParallelSystemDeprecatedSupport.throwDingTalkSyncDeprecated();
        return null;
    }
}
