package cn.iocoder.yudao.module.oa.api.dto.config;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class ThresholdConfigRespVO {

    private Long id;
    private String thresholdCategory;
    private String thresholdType;
    private String metricName;
    private String metricType;
    private String platformType;
    private String contentType;
    private String judgeMode;
    private Long lowFans;
    private Long highFans;
    private Integer dailyLow;
    private Integer dailyHigh;
    private Long hotValue;
    private Long lowValue;
    private Long overrideAccountId;
    private Long overrideValue;
    private Long ipGroupId;
    private String compareOperator;
    private BigDecimal thresholdValue;
    private String alertLevel;
    private String notifyMethods;
    private String status;
    private String remark;
    private LocalDateTime createTime;
}
