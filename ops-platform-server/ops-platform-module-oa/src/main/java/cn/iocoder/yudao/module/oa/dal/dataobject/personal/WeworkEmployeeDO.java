package cn.iocoder.yudao.module.oa.dal.dataobject.personal;

import cn.iocoder.yudao.framework.tenant.core.db.TenantBaseDO;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("oa_wework_employee")
public class WeworkEmployeeDO extends TenantBaseDO {

    private Long weworkAccountId;
    private String nickname;
    private String weworkUserId;
    private String phone;
    private String department;
    private String position;
    private String status;
}
