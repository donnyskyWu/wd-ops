package cn.iocoder.yudao.module.oa.api.dto.system;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class RoleCreateReq {

    @NotBlank
    @Size(max = 64)
    @Pattern(regexp = "^[A-Z][A-Z0-9_]*$", message = "角色编码须大写字母+下划线")
    private String code;

    @NotBlank
    @Size(max = 64)
    private String name;

    @Size(max = 512)
    private String remark;
}
