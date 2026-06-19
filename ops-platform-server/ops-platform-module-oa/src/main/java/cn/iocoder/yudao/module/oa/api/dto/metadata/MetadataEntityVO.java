package cn.iocoder.yudao.module.oa.api.dto.metadata;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class MetadataEntityVO {

    private Long id;
    private String entityCode;
    private String entityName;
    private String physicalTable;
    private String status;
    private String remark;
    private LocalDateTime updateTime;
    private List<MetadataFieldVO> fields;
}
