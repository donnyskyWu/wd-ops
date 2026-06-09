package cn.iocoder.yudao.module.oa.api.dto.monitor;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class ExternalWorkVO {

    private Long id;
    private Long accountId;
    private String platformType;
    private String title;
    private String workUrl;
    private Long playCount;
    private BigDecimal completionRate;
    private Integer likeCount;
    private LocalDateTime publishTime;
    private String industry;
    private Long ipGroupId;
}
