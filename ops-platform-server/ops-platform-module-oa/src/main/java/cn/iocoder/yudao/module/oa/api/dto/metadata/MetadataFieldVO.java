package cn.iocoder.yudao.module.oa.api.dto.metadata;

import lombok.Data;

@Data
public class MetadataFieldVO {

    private Long id;
    private Long entityId;
    private String fieldCode;
    private String fieldName;
    private String columnName;
    private String dataType;
    private String queryConditionType;
    private String dictType;
    private Object selectorConfig;
    private Integer sort;
}
