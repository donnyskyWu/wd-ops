package cn.iocoder.yudao.module.oa.api.dto.realname;

import cn.iocoder.yudao.module.oa.framework.dict.InDict;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class RealnameCreateReq {

    private Long companyId;

    @NotBlank
    @Size(max = 64)
    private String realName;

    @NotBlank
    @InDict("dict_id_type")
    private String idType;

    @NotBlank
    @Pattern(regexp = "^[1-9]\\d{5}(19|20)\\d{2}(0[1-9]|1[0-2])(0[1-9]|[12]\\d|3[01])\\d{3}[\\dXx]$",
            message = "身份证号格式不正确")
    private String idCard;

    @NotBlank
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
    private String phone;

    @Size(max = 64)
    private String wechat;

    @InDict("dict_gender")
    private String gender;

    @InDict("dict_realname_status")
    private String status;

    @Size(max = 512)
    private String idCardFrontKey;

    @Size(max = 512)
    private String idCardBackKey;
}
