package cn.iocoder.yudao.module.oa.api.dto.ipgroup;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class IpGroupDetailVO {

    private Long id;
    private String groupName;
    private Integer groupType;
    private Long parentId;
    private String parentName;
    private Long leaderId;
    private String leaderName;
    private Integer sortOrder;
    private Integer status;
    private String remark;
    private Integer memberCount;
    private Integer accountCount;
    private Integer anchorCount;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
