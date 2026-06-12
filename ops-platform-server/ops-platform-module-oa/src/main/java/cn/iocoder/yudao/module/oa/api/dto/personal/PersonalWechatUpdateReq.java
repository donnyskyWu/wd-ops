package cn.iocoder.yudao.module.oa.api.dto.personal;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class PersonalWechatUpdateReq {

    @NotNull
    private Long id;
    private String accountName;
    private String wechatId;
    @Pattern(regexp = "^$|^1[3-9]\\d{9}$", message = "联系电话格式不正确")
    private String contactPhone;
    private Long phoneId;
    private String status;
}
