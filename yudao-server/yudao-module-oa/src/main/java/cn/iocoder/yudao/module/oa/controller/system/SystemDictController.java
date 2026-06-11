package cn.iocoder.yudao.module.oa.controller.system;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.oa.api.dto.dict.DictTypeRespVO;
import cn.iocoder.yudao.module.oa.api.dto.system.DictAdminRowVO;
import cn.iocoder.yudao.module.oa.api.dto.system.DictCreateReq;
import cn.iocoder.yudao.module.oa.api.dto.system.DictTypeDetailVO;
import cn.iocoder.yudao.module.oa.api.dto.system.DictUpdateReq;
import cn.iocoder.yudao.module.oa.service.system.SystemDictService;
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
@RequestMapping({"/admin-api/oa/system/dict", "/admin-api/system/dict"})
@Validated
@RequiredArgsConstructor
public class SystemDictController {

    private final SystemDictService systemDictService;

    @GetMapping("/type-list")
    @PreAuthorize("hasAuthority('oa:dict:admin-list')")
    public CommonResult<List<DictTypeRespVO>> typeList() {
        return CommonResult.success(systemDictService.typeList());
    }

    @GetMapping("/type")
    @PreAuthorize("hasAuthority('oa:dict:admin-list')")
    public CommonResult<DictTypeDetailVO> getByType(@RequestParam String type) {
        return CommonResult.success(systemDictService.getByType(type));
    }

    @GetMapping("/list")
    @PreAuthorize("hasAuthority('oa:dict:admin-list')")
    public CommonResult<PageResult<DictAdminRowVO>> list(
            @RequestParam(required = false) String dictName,
            @RequestParam(required = false) String dictType,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") Integer pageNo,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        return CommonResult.success(systemDictService.adminList(dictName, dictType, status, pageNo, pageSize));
    }

    @PostMapping("/create")
    @PreAuthorize("hasAuthority('oa:dict:create')")
    public CommonResult<Long> create(@Valid @RequestBody DictCreateReq req) {
        return CommonResult.success(systemDictService.create(req));
    }

    @PutMapping("/update")
    @PreAuthorize("hasAuthority('oa:dict:update')")
    public CommonResult<Boolean> update(@Valid @RequestBody DictUpdateReq req) {
        systemDictService.update(req);
        return CommonResult.success(true);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('oa:dict:delete')")
    public CommonResult<Boolean> delete(@PathVariable Long id) {
        systemDictService.deleteData(id);
        return CommonResult.success(true);
    }
}
