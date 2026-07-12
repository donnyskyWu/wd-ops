package cn.iocoder.yudao.module.oa.dal.dataobject.monitor;

import cn.iocoder.yudao.framework.tenant.core.db.TenantBaseDO;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("oa_external_account")
public class ExternalAccountDO extends TenantBaseDO {

    private Long collectConfigId;
    private String platformType;
    private String externalUserId;
    private String displayName;
    private Long followerCount;
    private Integer workCount;
    private String avatarUrl;
    private LocalDateTime lastSyncedAt;
}
