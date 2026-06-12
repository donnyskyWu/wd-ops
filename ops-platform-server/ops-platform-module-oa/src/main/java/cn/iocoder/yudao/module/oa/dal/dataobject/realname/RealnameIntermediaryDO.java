package cn.iocoder.yudao.module.oa.dal.dataobject.realname;

import cn.iocoder.yudao.framework.tenant.core.db.TenantBaseDO;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("oa_realname_intermediary")
public class RealnameIntermediaryDO extends TenantBaseDO {

    private Long realnameId;
    private String intermediaryName;
    private String intermediaryPhoneEncrypted;
    private String intermediaryWechat;
    private String relationType;
    private BigDecimal commissionRate;
    private String remark;
}
