package cn.iocoder.yudao.module.oa.api.dto.ipgroup;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class IpGroupMemberCreateReq {

    @NotNull
    private Long userId;
    private String position;
    private Boolean isLeader;
}
