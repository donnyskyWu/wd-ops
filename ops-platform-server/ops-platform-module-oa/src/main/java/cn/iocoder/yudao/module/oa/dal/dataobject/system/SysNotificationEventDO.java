package cn.iocoder.yudao.module.oa.dal.dataobject.system;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("sys_notification_event")
public class SysNotificationEventDO {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long tenantId;
    private String eventType;
    private String bizKey;
    private Long recipientUserId;
    private LocalDateTime createTime;
}
