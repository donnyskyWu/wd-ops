package cn.iocoder.yudao.module.oa.dal.dataobject.simcard;

import cn.iocoder.yudao.framework.tenant.core.db.TenantBaseDO;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("oa_sim_card")
public class SimCardDO extends TenantBaseDO {

    private Long phoneId;
    private String phoneNumberEncrypted;
    private String phoneNumberHash;
    private String isPrimary;
    private String operator;
    private Long assignedUserId;
    private String iccidEncrypted;
    private String iccidHash;
    private String packageName;
    private String status;
    private Integer accountBoundCount;
}
