package cn.iocoder.yudao.module.oa.api.dto.collect;

import lombok.Data;

@Data
public class CollectQualityLogRespVO {
    private Long id;
    private Long checkId;
    private String checkName;
    private String checkTime;
    private Integer totalCount;
    private Integer passCount;
    private Integer failCount;
    private String level;
}
