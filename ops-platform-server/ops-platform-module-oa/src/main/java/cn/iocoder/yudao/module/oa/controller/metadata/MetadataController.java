package cn.iocoder.yudao.module.oa.controller.metadata;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.oa.api.dto.metadata.MetadataEntityCreateReq;
import cn.iocoder.yudao.module.oa.api.dto.metadata.MetadataEntityUpdateReq;
import cn.iocoder.yudao.module.oa.api.dto.metadata.MetadataEntityVO;
import cn.iocoder.yudao.module.oa.api.dto.metadata.MetadataFieldBatchUpdateReq;
import cn.iocoder.yudao.module.oa.api.dto.metadata.MetadataFieldVO;
import cn.iocoder.yudao.module.oa.api.dto.metadata.TableColumnVO;
import cn.iocoder.yudao.module.oa.api.dto.metadata.UnmappedTableVO;
import cn.iocoder.yudao.module.oa.service.metadata.MetadataService;
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
@RequestMapping("/admin-api/oa/metadata")
@Validated
@RequiredArgsConstructor
public class MetadataController {

    private final MetadataService metadataService;

    @GetMapping("/list")
    @PreAuthorize("hasAuthority('oa:metadata:query')")
    public CommonResult<PageResult<MetadataEntityVO>> list(
            @RequestParam(required = false) String entityName,
            @RequestParam(required = false) String entityCode,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "20") Integer pageSize) {
        return CommonResult.success(metadataService.list(entityName, entityCode, status, pageNum, pageSize));
    }

    @GetMapping("/unmapped-tables")
    @PreAuthorize("hasAuthority('oa:metadata:query')")
    public CommonResult<List<UnmappedTableVO>> unmappedTables() {
        return CommonResult.success(metadataService.listUnmappedTables());
    }

    @GetMapping("/table-columns")
    @PreAuthorize("hasAuthority('oa:metadata:query')")
    public CommonResult<List<TableColumnVO>> tableColumns(@RequestParam String tableName) {
        return CommonResult.success(metadataService.listTableColumns(tableName));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('oa:metadata:query')")
    public CommonResult<MetadataEntityVO> get(@PathVariable Long id) {
        return CommonResult.success(metadataService.getById(id));
    }

    @GetMapping("/entity/{code}/fields")
    @PreAuthorize("hasAuthority('oa:metadata:query')")
    public CommonResult<List<MetadataFieldVO>> fieldsByCode(@PathVariable String code) {
        return CommonResult.success(metadataService.getFieldsByEntityCode(code));
    }

    @PostMapping("/create")
    @PreAuthorize("hasAuthority('oa:metadata:create')")
    public CommonResult<Long> create(@Valid @RequestBody MetadataEntityCreateReq req) {
        return CommonResult.success(metadataService.create(req));
    }

    @PutMapping("/update")
    @PreAuthorize("hasAuthority('oa:metadata:update')")
    public CommonResult<Boolean> update(@Valid @RequestBody MetadataEntityUpdateReq req) {
        metadataService.update(req);
        return CommonResult.success(true);
    }

    @PutMapping("/{entityId}/fields")
    @PreAuthorize("hasAuthority('oa:metadata:update')")
    public CommonResult<Boolean> updateFields(@PathVariable Long entityId,
                                              @Valid @RequestBody MetadataFieldBatchUpdateReq req) {
        metadataService.updateFields(entityId, req);
        return CommonResult.success(true);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('ROLE_OA_ADMIN')")
    public CommonResult<Boolean> delete(@PathVariable Long id) {
        metadataService.delete(id);
        return CommonResult.success(true);
    }
}
