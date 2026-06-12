package cn.iocoder.yudao.module.oa.api.dto.system;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class RoleUpdateReq {

    @NotNull
    private Long id;

    @Size(max = 64)
    private String name;

    @Size(max = 512)
    private String remark;
}
