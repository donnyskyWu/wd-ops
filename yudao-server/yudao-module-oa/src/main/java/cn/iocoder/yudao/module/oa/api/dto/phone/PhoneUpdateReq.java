package cn.iocoder.yudao.module.oa.api.dto.phone;

import cn.iocoder.yudao.module.oa.framework.dict.InDict;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class PhoneUpdateReq {

    @NotNull
    private Long id;

    private Long realnameId;

    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
    private String phoneNumber;

    @Size(max = 32)
    private String phoneCode;

    @Size(max = 100)
    private String phoneModel;

    private Long keeperId;

    @Size(max = 64)
    private String wechatBound;

    @InDict("dict_phone_status")
    private String status;
}
