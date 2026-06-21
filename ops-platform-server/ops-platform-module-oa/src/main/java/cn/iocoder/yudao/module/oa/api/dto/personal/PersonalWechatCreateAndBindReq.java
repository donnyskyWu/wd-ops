package cn.iocoder.yudao.module.oa.api.dto.personal;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class PersonalWechatCreateAndBindReq {

    @NotBlank
    @Size(max = 100)
    private String accountName;

    @NotBlank
    @Size(max = 64)
    private String wechatId;

    @Pattern(regexp = "^$|^1[3-9]\\d{9}$", message = "联系电话格式不正确")
    private String contactPhone;

    @NotBlank
    @Size(max = 64)
    private String aochuangWechatAccountId;

    @NotNull
    private Long aochuangAccountRefId;

    private String aochuangNickname;
    private String aochuangAvatar;
    private Boolean aochuangIsAlive;
}
