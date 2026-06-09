package cn.iocoder.yudao.module.oa.dal.dataobject.config;

import cn.iocoder.yudao.framework.tenant.core.db.TenantBaseDO;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("oa_collect_config")
public class CollectConfigDO extends TenantBaseDO {

    private String scope;
    private String configName;
    private String subType;
    private String platformType;
    private Long accountId;
    private String collectFrequency;
    private String collectMethod;
    private String apiUrl;
    private String apiKeyEncrypted;
    private String requestMethod;
    private String requestParams;
    private String responseMapping;
    private String collectFields;
    private String status;
    private String remark;
}
