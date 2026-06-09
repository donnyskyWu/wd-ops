package cn.iocoder.yudao.module.oa.api.dto.ipgroup;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class IpGroupUpdateReq {

    @NotNull
    private Long id;
    private String groupName;
    private Long leaderId;
    private Long leaderUserId;
    private Integer sortOrder;
    private Integer status;
    private String remark;
}
