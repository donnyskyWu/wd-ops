package cn.iocoder.yudao.module.oa.api.dto.realname;

import cn.iocoder.yudao.module.oa.framework.dict.InDict;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class IntermediaryCreateReq {

    @NotBlank
    @Size(max = 64)
    private String intermediaryName;

    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
    private String intermediaryPhone;

    @Size(max = 64)
    private String intermediaryWechat;

    @NotBlank
    @InDict("dict_intermediary_relation")
    private String relationType;

    @NotNull
    @DecimalMin("0")
    @DecimalMax("100")
    private BigDecimal commissionRate;

    @Size(max = 200)
    private String remark;
}
