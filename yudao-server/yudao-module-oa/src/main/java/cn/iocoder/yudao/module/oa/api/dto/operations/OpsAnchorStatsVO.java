package cn.iocoder.yudao.module.oa.api.dto.operations;

import lombok.Data;

@Data
public class OpsAnchorStatsVO {

    private Long opsUserId;
    private String opsUserName;
    private Integer anchorCount = 0;
    private Long totalFollower = 0L;
    private Integer totalContent = 0;
    private Double totalRevenue = 0.0;
}
