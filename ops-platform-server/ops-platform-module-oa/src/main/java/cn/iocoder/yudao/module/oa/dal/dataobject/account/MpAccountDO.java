package cn.iocoder.yudao.module.oa.dal.dataobject.account;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * Football 微信公众号 SSOT（shenyu-mp.mp_account）。
 */
@Data
@TableName("mp_account")
public class MpAccountDO {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String name;

    private String account;

    private String appId;

    private String appSecret;

    private String token;

    private String remark;

    private Integer status;

    private Long bindAuthorId;

    private Long tenantId;

    private String creator;

    private LocalDateTime createTime;

    private String updater;

    private LocalDateTime updateTime;
}
