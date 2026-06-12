package cn.iocoder.yudao.module.oa.api.dto.operations;

import lombok.Data;

@Data
public class ContentStatsVO {

    private Integer totalCount = 0;
    private Integer hitCount = 0;
    private Long totalRead = 0L;
    private Long avgRead = 0L;
    // S-R8 B1: 5 KPI 卡全量聚合（避免前端 list.reduce 因分页导致数字跟着翻页变）
    private Long totalLikes = 0L;
    private Long totalComments = 0L;
    private Long totalShares = 0L;
}
