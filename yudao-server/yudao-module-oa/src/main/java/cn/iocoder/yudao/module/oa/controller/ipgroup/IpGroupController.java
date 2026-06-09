package cn.iocoder.yudao.module.oa.controller.ipgroup;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.module.oa.api.dto.ipgroup.IpGroupAccountBindReq;
import cn.iocoder.yudao.module.oa.api.dto.ipgroup.IpGroupAccountVO;
import cn.iocoder.yudao.module.oa.api.dto.ipgroup.IpGroupAnchorBindReq;
import cn.iocoder.yudao.module.oa.api.dto.ipgroup.IpGroupAnchorVO;
import cn.iocoder.yudao.module.oa.api.dto.ipgroup.IpGroupCreateReq;
import cn.iocoder.yudao.module.oa.api.dto.ipgroup.IpGroupDetailVO;
import cn.iocoder.yudao.module.oa.api.dto.ipgroup.IpGroupMemberCreateReq;
import cn.iocoder.yudao.module.oa.api.dto.ipgroup.IpGroupMemberUpdateReq;
import cn.iocoder.yudao.module.oa.api.dto.ipgroup.IpGroupMemberVO;
import cn.iocoder.yudao.module.oa.api.dto.ipgroup.IpGroupStatsVO;
import cn.iocoder.yudao.module.oa.api.dto.ipgroup.IpGroupStatusReq;
import cn.iocoder.yudao.module.oa.api.dto.ipgroup.IpGroupTreeVO;
import cn.iocoder.yudao.module.oa.api.dto.ipgroup.IpGroupUpdateReq;
import cn.iocoder.yudao.module.oa.service.ipgroup.IpGroupService;
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
@RequestMapping("/admin-api/oa/ip-group")
@Validated
@RequiredArgsConstructor
public class IpGroupController {

    private final IpGroupService ipGroupService;

    @GetMapping("/tree")
    public CommonResult<List<IpGroupTreeVO>> tree() {
        return CommonResult.success(ipGroupService.getTree());
    }

    @GetMapping("/{id}")
    public CommonResult<IpGroupDetailVO> get(@PathVariable Long id) {
        return CommonResult.success(ipGroupService.getDetail(id));
    }

    @PostMapping("/create")
    public CommonResult<Long> create(@Valid @RequestBody IpGroupCreateReq req) {
        return CommonResult.success(ipGroupService.create(req));
    }

    @PutMapping("/update")
    public CommonResult<Boolean> update(@Valid @RequestBody IpGroupUpdateReq req) {
        ipGroupService.update(req);
        return CommonResult.success(true);
    }

    @PutMapping("/{id}/status")
    public CommonResult<Boolean> updateStatus(@PathVariable Long id, @Valid @RequestBody IpGroupStatusReq req) {
        ipGroupService.updateStatus(id, req.getStatus());
        return CommonResult.success(true);
    }

    @DeleteMapping("/delete")
    public CommonResult<Boolean> delete(@RequestParam Long id) {
        ipGroupService.delete(id);
        return CommonResult.success(true);
    }

    @GetMapping("/{id}/stats")
    public CommonResult<IpGroupStatsVO> stats(@PathVariable Long id) {
        return CommonResult.success(ipGroupService.getStats(id));
    }

    @GetMapping("/{id}/members")
    public CommonResult<List<IpGroupMemberVO>> members(@PathVariable Long id) {
        return CommonResult.success(ipGroupService.listMembers(id));
    }

    @PostMapping("/{id}/members")
    public CommonResult<Boolean> addMember(@PathVariable Long id, @Valid @RequestBody IpGroupMemberCreateReq req) {
        ipGroupService.addMember(id, req);
        return CommonResult.success(true);
    }

    @PutMapping("/{id}/members/{memberId}")
    public CommonResult<Boolean> updateMember(@PathVariable Long id,
                                              @PathVariable Long memberId,
                                              @Valid @RequestBody IpGroupMemberUpdateReq req) {
        ipGroupService.updateMember(id, memberId, req);
        return CommonResult.success(true);
    }

    @DeleteMapping("/{id}/members/{memberId}")
    public CommonResult<Boolean> deleteMember(@PathVariable Long id, @PathVariable Long memberId) {
        ipGroupService.deleteMember(id, memberId);
        return CommonResult.success(true);
    }

    @GetMapping("/{id}/accounts")
    public CommonResult<List<IpGroupAccountVO>> accounts(@PathVariable Long id) {
        return CommonResult.success(ipGroupService.listAccounts(id));
    }

    @PostMapping("/{id}/accounts")
    public CommonResult<Boolean> bindAccounts(@PathVariable Long id, @Valid @RequestBody IpGroupAccountBindReq req) {
        ipGroupService.bindAccounts(id, req);
        return CommonResult.success(true);
    }

    @DeleteMapping("/{id}/accounts/{accountId}")
    public CommonResult<Boolean> unbindAccount(@PathVariable Long id, @PathVariable Long accountId) {
        ipGroupService.unbindAccount(id, accountId);
        return CommonResult.success(true);
    }

    @GetMapping("/{id}/anchors")
    public CommonResult<List<IpGroupAnchorVO>> anchors(@PathVariable Long id) {
        return CommonResult.success(ipGroupService.listAnchors(id));
    }

    @PostMapping("/{id}/anchors")
    public CommonResult<Boolean> bindAnchors(@PathVariable Long id, @Valid @RequestBody IpGroupAnchorBindReq req) {
        ipGroupService.bindAnchors(id, req);
        return CommonResult.success(true);
    }

    @DeleteMapping("/{id}/anchors/{anchorUserId}")
    public CommonResult<Boolean> unbindAnchor(@PathVariable Long id, @PathVariable Long anchorUserId) {
        ipGroupService.unbindAnchor(id, anchorUserId);
        return CommonResult.success(true);
    }
}
