package cn.iocoder.yudao.module.oa.api.dto.personal;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class WeworkEmployeeCreateReq {

    @NotNull
    private Long weworkAccountId;

    @NotBlank
    @Size(max = 100)
    private String nickname;

    @NotBlank
    @Size(max = 64)
    private String weworkUserId;

    @Pattern(regexp = "^$|^1[3-9]\\d{9}$", message = "手机号格式不正确")
    private String phone;

    @Size(max = 100)
    private String department;

    @Size(max = 100)
    private String position;

    private String status;
}
