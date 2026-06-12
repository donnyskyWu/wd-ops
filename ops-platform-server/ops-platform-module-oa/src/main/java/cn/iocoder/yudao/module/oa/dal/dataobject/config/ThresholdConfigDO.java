package cn.iocoder.yudao.module.oa.dal.dataobject.config;

import cn.iocoder.yudao.framework.tenant.core.db.TenantBaseDO;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("oa_threshold_config")
public class ThresholdConfigDO extends TenantBaseDO {

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
}
