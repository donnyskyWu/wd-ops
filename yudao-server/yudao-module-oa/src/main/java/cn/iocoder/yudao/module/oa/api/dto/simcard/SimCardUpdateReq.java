package cn.iocoder.yudao.module.oa.api.dto.simcard;

import cn.iocoder.yudao.module.oa.framework.dict.InDict;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class SimCardUpdateReq {

    @NotNull
    private Long id;

    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
    private String phoneNumber;

    @InDict("dict_yes_no")
    private String isPrimary;

    @InDict("dict_sim_operator")
    private String operator;

    private Long assignedUserId;

    @Size(max = 30)
    private String iccid;

    @Size(max = 100)
    private String packageName;

    @InDict("dict_sim_status")
    private String status;
}
