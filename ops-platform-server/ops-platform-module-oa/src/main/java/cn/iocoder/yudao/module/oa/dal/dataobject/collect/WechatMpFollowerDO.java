package cn.iocoder.yudao.module.oa.dal.dataobject.collect;

import cn.iocoder.yudao.framework.tenant.core.db.TenantBaseDO;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("oa_wechat_mp_follower")
public class WechatMpFollowerDO extends TenantBaseDO {

    private Long accountId;
    private String openid;
    private String nickname;
    private String avatar;
    private String unionid;
    private LocalDateTime subscribedAt;
    private LocalDateTime syncedAt;
}
