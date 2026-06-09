package cn.iocoder.yudao.module.oa.api.dto.ipgroup;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class IpGroupCreateReq {

    @NotBlank
    private String groupName;
    /** 1=大组 2=小组 */
    @NotNull
    private Integer groupType;
    private Long parentId;
    private Long leaderId;
    private Long leaderUserId;
    private Integer sortOrder;
    private Integer status;
    private String remark;
}
