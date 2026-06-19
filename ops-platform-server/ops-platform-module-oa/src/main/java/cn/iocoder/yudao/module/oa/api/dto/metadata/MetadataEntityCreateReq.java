package cn.iocoder.yudao.module.oa.api.dto.metadata;

import cn.iocoder.yudao.module.oa.framework.dict.InDict;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class MetadataEntityCreateReq {

    @NotBlank
    @Pattern(regexp = "^[a-z][a-z0-9_]*$", message = "实体编码须小写字母开头")
    @Size(max = 64)
    private String entityCode;

    @NotBlank
    @Size(max = 128)
    private String entityName;

    @NotBlank
    @Pattern(regexp = "^[a-z][a-z0-9_]*$", message = "物理表名须小写字母开头")
    @Size(max = 128)
    private String physicalTable;

    @InDict("dict_metadata_entity_status")
    private String status;

    @Size(max = 512)
    private String remark;
}
