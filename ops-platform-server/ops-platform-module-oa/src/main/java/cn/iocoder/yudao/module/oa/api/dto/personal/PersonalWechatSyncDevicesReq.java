package cn.iocoder.yudao.module.oa.api.dto.personal;

import lombok.Data;

@Data
public class PersonalWechatSyncDevicesReq {

    /** 指定奥创账号；为空则同步租户下全部 ENABLED 奥创账号 */
    private Long aoCreateAccountId;
}
