package cn.iocoder.yudao.module.oa.api.dto.ipgroup;

import cn.iocoder.yudao.module.oa.framework.dict.InDict;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class IpGroupMemberCreateReq {

    @NotNull
    private Long userId;

    @NotBlank
    @InDict("dict_position")
    private String position;

    private Boolean isLeader;
}
