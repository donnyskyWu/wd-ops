package cn.iocoder.yudao.module.oa.dal.dataobject.config;

import cn.iocoder.yudao.framework.tenant.core.db.TenantBaseDO;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("oa_ai_prompt_config")
public class AiPromptConfigDO extends TenantBaseDO {

    private String templateName;
    private String scene;
    private String promptContent;
    private String variableDesc;
    private BigDecimal temperature;
    private String status;
    private String remark;
}
