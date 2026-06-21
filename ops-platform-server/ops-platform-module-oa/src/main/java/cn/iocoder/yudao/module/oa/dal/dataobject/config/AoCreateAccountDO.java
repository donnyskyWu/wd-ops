package cn.iocoder.yudao.module.oa.dal.dataobject.config;

import cn.iocoder.yudao.framework.tenant.core.db.TenantBaseDO;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("oa_aocreate_account")
public class AoCreateAccountDO extends TenantBaseDO {

    private Long aocreateApiId;
    private String accountName;
    private String aochuangAccountId;
    private String status;
    private LocalDateTime lastDeviceSyncAt;
    private String connStatus;
}
