package cn.iocoder.yudao.module.oa.api.dto.dev;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class DingTalkTestSendReq {

    /** 目标用户 sys_user.id，如张武 = 2036 */
    @NotNull
    private Long userId;
}
