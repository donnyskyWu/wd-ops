package cn.iocoder.yudao.module.oa.api.dto.metadata;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class UnmappedTableVO {

    private String tableName;
    private String suggestedEntityCode;
    private String suggestedEntityName;
}
