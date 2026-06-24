package cn.iocoder.yudao.module.oa.dal.dataobject.collect;

import cn.iocoder.yudao.framework.tenant.core.db.TenantBaseDO;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("oa_douyin_follower")
public class DouyinFollowerDO extends TenantBaseDO {

    private Long accountId;
    private String followerId;
    private String nickname;
    private String avatar;
    private LocalDateTime followedAt;
    private LocalDateTime syncedAt;
}
