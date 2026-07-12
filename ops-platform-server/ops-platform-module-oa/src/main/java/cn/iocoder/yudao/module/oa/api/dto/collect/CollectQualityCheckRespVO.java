package cn.iocoder.yudao.module.oa.api.dto.collect;

import lombok.Data;

@Data
public class CollectQualityCheckRespVO {
    private Long id;
    private String name;
    private String checkType;
    private String level;
    private String tableName;
    private String rule;
    private Boolean enabled;
    private String lastCheckAt;
    private Double passRate;
}
