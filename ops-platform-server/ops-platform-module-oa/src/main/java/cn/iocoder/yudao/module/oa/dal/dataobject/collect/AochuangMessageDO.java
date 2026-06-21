package cn.iocoder.yudao.module.oa.dal.dataobject.collect;

import cn.iocoder.yudao.framework.tenant.core.db.TenantBaseDO;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("oa_aochuang_message")
public class AochuangMessageDO extends TenantBaseDO {

    private Long personalWechatId;
    private String aochuangWechatAccountId;
    private String aochuangMessageId;
    private String aochuangFriendId;
    private String msgType;
    private String direction;
    private String content;
    private LocalDateTime messageTime;
    private LocalDateTime syncedAt;
}
