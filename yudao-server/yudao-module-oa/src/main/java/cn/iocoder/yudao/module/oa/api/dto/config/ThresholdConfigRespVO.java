package cn.iocoder.yudao.module.oa.api.dto.config;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class ThresholdConfigRespVO {

    private Long id;
    private String metricName;
    private String metricType;
    private String platformType;
    private Long ipGroupId;
    private String compareOperator;
    private BigDecimal thresholdValue;
    private String alertLevel;
    private String notifyMethods;
    private String status;
    private String remark;
    private LocalDateTime createTime;
}
