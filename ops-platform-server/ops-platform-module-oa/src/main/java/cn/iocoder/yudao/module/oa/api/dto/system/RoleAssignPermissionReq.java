package cn.iocoder.yudao.module.oa.api.dto.system;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class RoleAssignPermissionReq {

    @NotNull
    private Long roleId;

    @NotEmpty
    private List<Long> permissionIds;
}
