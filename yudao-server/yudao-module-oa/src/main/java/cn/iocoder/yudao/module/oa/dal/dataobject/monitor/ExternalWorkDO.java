package cn.iocoder.yudao.module.oa.dal.dataobject.monitor;

import cn.iocoder.yudao.framework.tenant.core.db.TenantBaseDO;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("oa_external_work")
public class ExternalWorkDO extends TenantBaseDO {
    private Long accountId;
    private String platformType;
    private String title;
    private String workUrl;
    private Long playCount;
    private BigDecimal completionRate;
    private Integer likeCount;
    private LocalDateTime publishTime;
    private String industry;
    private Long ipGroupId;
    private Integer isExternal;
}
