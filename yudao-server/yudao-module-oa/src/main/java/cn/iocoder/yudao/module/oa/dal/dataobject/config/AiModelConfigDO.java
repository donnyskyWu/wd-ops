package cn.iocoder.yudao.module.oa.dal.dataobject.config;

import cn.iocoder.yudao.framework.tenant.core.db.TenantBaseDO;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("oa_ai_model_config")
public class AiModelConfigDO extends TenantBaseDO {

    private String modelName;
    private String modelId;
    private String modelType;
    private String apiEndpoint;
    private String apiKeyEncrypted;
    private Integer maxTokens;
    private Integer timeout;
    private Integer isDefault;
    private String connStatus;
    private BigDecimal temperature;
    private BigDecimal topP;
    private String status;
    private String remark;
}
