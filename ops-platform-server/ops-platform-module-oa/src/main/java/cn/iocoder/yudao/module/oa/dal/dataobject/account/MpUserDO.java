package cn.iocoder.yudao.module.oa.dal.dataobject.account;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * Football 公众号粉丝 SSOT（shenyu-mp.mp_user）。
 */
@Data
@TableName("mp_user")
public class MpUserDO {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String openid;

    private String unionId;

    /** 1=已关注 */
    private Integer subscribeStatus;

    private LocalDateTime subscribeTime;

    private String nickname;

    private String headImageUrl;

    private Long accountId;

    private String appId;

    private Long tenantId;

    private LocalDateTime updateTime;

    @TableLogic
    private Boolean deleted;
}
