package cn.iocoder.yudao.module.oa.dal.dataobject.config;

import cn.iocoder.yudao.framework.tenant.core.db.TenantBaseDO;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("oa_aocreate_api")
public class AoCreateApiDO extends TenantBaseDO {

    private String apiUrl;
    private String appId;
    private String appSecretEncrypted;
    private String tokenEncrypted;
    private String status;
    private Integer dailyQuota;
    private Integer currentUsage;
}
