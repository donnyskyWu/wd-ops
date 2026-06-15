package cn.iocoder.yudao.module.oa.dal.dataobject.account;

import cn.iocoder.yudao.framework.tenant.core.db.TenantBaseDO;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("oa_wechat_official_cert_renewal")
public class WechatOfficialCertRenewalDO extends TenantBaseDO {

    private Long accountId;
    private LocalDateTime renewalTime;
    private Long renewerUserId;
    private BigDecimal renewalAmount;
}
