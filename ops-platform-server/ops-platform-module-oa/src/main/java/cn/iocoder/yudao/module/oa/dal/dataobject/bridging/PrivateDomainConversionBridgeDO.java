package cn.iocoder.yudao.module.oa.dal.dataobject.bridging;

import cn.iocoder.yudao.framework.tenant.core.db.TenantBaseDO;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("oa_private_domain_conversion_bridge")
public class PrivateDomainConversionBridgeDO extends TenantBaseDO {

    private String sourceType;
    private Long sourceId;
    private String targetType;
    private Long targetId;
    private String matchMethod;
    private BigDecimal confidence;
    private String matchEvidenceJson;
    private String reviewStatus;
    private String linkedBy;
    private LocalDateTime linkedAt;
}
