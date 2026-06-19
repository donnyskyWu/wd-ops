package cn.iocoder.yudao.module.oa.dal.dataobject.perf;

import cn.iocoder.yudao.framework.tenant.core.db.TenantBaseDO;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("oa_metric")
public class MetricDO extends TenantBaseDO {

    private String metricName;
    private String metricCode;
    private String unit;
    private String category;
    private String metricFormula;
    private String dataSource;
    /** 指标构建器 JSON（含参数化查询条件） */
    private String paramsJson;
    private Integer status;
}
