package cn.iocoder.yudao.module.oa.api.dto.operations;

import lombok.Data;

@Data
public class ContentStatsVO {

    private Integer totalCount = 0;
    private Integer hitCount = 0;
    private Long totalRead = 0L;
    private Long avgRead = 0L;
}
