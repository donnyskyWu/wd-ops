package cn.iocoder.yudao.module.oa.dal.dataobject.analytics;

import cn.iocoder.yudao.framework.tenant.core.db.TenantBaseDO;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("oa_content_daily")
public class ContentDailyDO extends TenantBaseDO {
    private Long contentId;
    private LocalDate statDate;
    private Long readCount;
    private Long playCount;
}
