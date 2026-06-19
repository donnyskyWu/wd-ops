package cn.iocoder.yudao.module.oa.dal.dataobject.company;

import cn.iocoder.yudao.framework.tenant.core.db.TenantBaseDO;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("oa_company")
public class CompanyDO extends TenantBaseDO {

    private String companyName;
    private String creditCode;
    private String industry;
    private String address;
    private String legalName;
    private String legalIdCardEncrypted;
    private Integer mpCapacityStandard;
    private Integer mpRegisteredCount;
    private String status;

    /** JSON array of file keys under tenant prefix, e.g. ["1/content/uuid.png"] */
    @TableField("business_license_keys")
    private String businessLicenseKeys;
}
