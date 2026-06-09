package cn.iocoder.yudao.module.oa.dal.dataobject.home;

import cn.iocoder.yudao.framework.tenant.core.db.TenantBaseDO;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("oa_home_alert")
public class HomeAlertDO extends TenantBaseDO {

    private String alertLevel;
    private String alertContent;
    private String alertSource;
    private LocalDateTime triggerTime;
    private String status;
}
