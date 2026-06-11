package cn.iocoder.yudao.module.oa.api.dto.system;

import cn.iocoder.yudao.module.oa.framework.dict.InDict;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
public class UserUpdateReq {

    @NotNull
    private Long id;

    @Size(max = 64)
    private String nickname;

    @Size(max = 128)
    private String email;

    @Size(max = 11)
    private String phone;

    @InDict("dict_position")
    private String position;

    private Long ipGroupId;

    private Long deptId;

    @InDict("dict_user_status")
    private String status;

    private List<Long> roleIds;

    @Size(max = 512)
    private String remark;
}
