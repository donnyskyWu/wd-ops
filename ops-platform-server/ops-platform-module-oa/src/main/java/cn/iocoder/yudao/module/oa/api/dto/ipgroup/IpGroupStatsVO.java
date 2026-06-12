package cn.iocoder.yudao.module.oa.api.dto.ipgroup;

import lombok.Data;

@Data
public class IpGroupStatsVO {

    private Long ipGroupId;
    private Integer memberCount;
    private Integer accountCount;
    private Integer anchorCount;
    private Long totalFollowers;
    private Integer totalContent;
    private Integer totalLiveHours;
    private Double roi;
}
