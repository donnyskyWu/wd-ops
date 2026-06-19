package cn.iocoder.yudao.module.oa.api.dto.metadata;

import cn.iocoder.yudao.module.oa.framework.dict.InDict;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class MetadataFieldUpdateItem {

    private Long id;

    @NotBlank
    @Size(max = 128)
    private String fieldName;

    @NotBlank
    @InDict("dict_metadata_query_condition_type")
    private String queryConditionType;

    @Size(max = 64)
    private String dictType;

    private Object selectorConfig;

    @NotNull
    private Integer sort;
}
