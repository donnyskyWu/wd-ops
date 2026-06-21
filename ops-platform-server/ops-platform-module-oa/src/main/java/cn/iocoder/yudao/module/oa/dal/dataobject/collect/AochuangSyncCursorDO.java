package cn.iocoder.yudao.module.oa.dal.dataobject.collect;

import cn.iocoder.yudao.framework.tenant.core.db.TenantBaseDO;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("oa_aochuang_sync_cursor")
public class AochuangSyncCursorDO extends TenantBaseDO {

    private String syncType;
    private String aochuangWechatAccountId;
    private Long personalWechatId;
    private String cursorValue;
    private LocalDateTime lastSyncAt;
}
