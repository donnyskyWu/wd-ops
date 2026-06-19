package cn.iocoder.yudao.module.oa.api.dto.metadata;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

@Data
public class MetadataFieldBatchUpdateReq {

    @NotEmpty
    @Valid
    private List<MetadataFieldUpdateItem> fields;
}
