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
}
