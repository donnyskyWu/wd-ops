package cn.iocoder.yudao.module.oa.api.dto.metadata;

import cn.iocoder.yudao.module.oa.framework.dict.InDict;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class MetadataEntityUpdateReq {

    @NotNull
    private Long id;

    @Size(max = 128)
    private String entityName;

    @InDict("dict_metadata_entity_status")
    private String status;

    @Size(max = 512)
    private String remark;
}
