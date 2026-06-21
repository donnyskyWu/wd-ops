package cn.iocoder.yudao.module.oa.dal.dataobject.personal;

import cn.iocoder.yudao.framework.tenant.core.db.TenantBaseDO;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("oa_personal_wechat_daily_stats")
public class PersonalWechatDailyStatsDO extends TenantBaseDO {

    private Long personalWechatId;
    private LocalDate statDate;
    private Integer totalFriends;
    private Integer newFriends;
    private Integer deletedFriends;
    private Integer messagesSent;
    private Integer messagesReceived;
    private Integer groupCount;
}
