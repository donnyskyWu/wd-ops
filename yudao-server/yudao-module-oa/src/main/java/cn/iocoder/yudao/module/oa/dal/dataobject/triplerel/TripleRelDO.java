package cn.iocoder.yudao.module.oa.dal.dataobject.triplerel;

import cn.iocoder.yudao.framework.tenant.core.db.TenantBaseDO;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("oa_account_wechat_video_wework_rel")
public class TripleRelDO extends TenantBaseDO {

    private Long wechatAccountId;
    private Long videoAccountId;
    private Long weworkAccountId;
    private String relationType;
    private LocalDateTime bindTime;
    private Integer status;
}
