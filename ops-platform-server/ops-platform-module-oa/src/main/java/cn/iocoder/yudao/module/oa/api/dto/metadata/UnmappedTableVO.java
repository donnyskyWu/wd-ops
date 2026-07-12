package cn.iocoder.yudao.module.oa.api.dto.metadata;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class UnmappedTableVO {

    private String tableName;
    private String suggestedEntityCode;
    private String suggestedEntityName;
    /** 数据库表注释（INFORMATION_SCHEMA.TABLES.TABLE_COMMENT） */
    private String tableComment;
}
