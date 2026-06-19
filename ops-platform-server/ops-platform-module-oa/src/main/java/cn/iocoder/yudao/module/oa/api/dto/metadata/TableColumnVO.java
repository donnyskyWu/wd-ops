package cn.iocoder.yudao.module.oa.api.dto.metadata;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class TableColumnVO {

    private String columnName;
    private String dataType;
    private String suggestedFieldCode;
    private String suggestedQueryConditionType;
    private String suggestedDictType;
}
