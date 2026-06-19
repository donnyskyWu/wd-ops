package cn.iocoder.yudao.module.oa.dal.dataobject.realname;

import cn.iocoder.yudao.framework.tenant.core.db.TenantBaseDO;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("oa_realname")
public class RealnameDO extends TenantBaseDO {

    private Long companyId;
    private String realName;
    private String idType;
    private String idCardEncrypted;
    private String phoneEncrypted;
    private String wechat;
    private String gender;
    private String status;
    private Integer accountBoundCount;
    private String idCardFrontKey;
    private String idCardBackKey;
}
