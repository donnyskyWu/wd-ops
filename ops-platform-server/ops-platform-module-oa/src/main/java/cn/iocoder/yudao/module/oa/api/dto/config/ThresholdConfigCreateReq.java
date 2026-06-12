package cn.iocoder.yudao.module.oa.api.dto.config;

import cn.iocoder.yudao.module.oa.framework.dict.InDict;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class ThresholdConfigCreateReq {

    @NotBlank
    @InDict("dict_threshold_category")
    private String thresholdCategory;

    @InDict("dict_threshold_type")
    private String thresholdType;

    @Size(max = 128)
    private String metricName;

    @InDict("dict_threshold_metric")
    private String metricType;

    @InDict("dict_content_type")
    private String contentType;

    @InDict("dict_judge_mode")
    private String judgeMode;

    private Long lowFans;
    private Long highFans;
    private Integer dailyLow;
    private Integer dailyHigh;
    private Long hotValue;
    private Long lowValue;
    private Long overrideAccountId;
    private Long overrideValue;

    @InDict("dict_platform_type")
    private String platformType;

    private Long ipGroupId;

    @InDict("dict_compare_operator")
    private String compareOperator;

    private BigDecimal thresholdValue;

    @InDict("dict_alert_level")
    private String alertLevel;

    private String notifyMethods;

    @InDict("dict_config_status")
    private String status;

    @Size(max = 512)
    private String remark;
}
