package cn.iocoder.yudao.module.oa.dal.dataobject.collect;

import cn.iocoder.yudao.framework.tenant.core.db.TenantBaseDO;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("oa_aochuang_friend")
public class AochuangFriendDO extends TenantBaseDO {

    private Long personalWechatId;
    private String aochuangWechatAccountId;
    private String aochuangFriendId;
    private String wechatId;
    private String alias;
    private String nickname;
    private String avatar;
    private String remark;
    private LocalDateTime syncedAt;
}
