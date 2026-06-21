package cn.iocoder.yudao.module.oa.api.dto.personal;

import cn.iocoder.yudao.module.oa.framework.dict.InDict;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class PersonalWechatBindDeviceReq {

    @NotBlank
    @Size(max = 64)
    private String aochuangWechatAccountId;

    @NotNull
    private Long aochuangAccountRefId;

    @InDict("dict_aochuang_bind_status")
    private String bindStatus;

    private String aochuangNickname;
    private String aochuangAvatar;
    private Boolean aochuangIsAlive;
}
