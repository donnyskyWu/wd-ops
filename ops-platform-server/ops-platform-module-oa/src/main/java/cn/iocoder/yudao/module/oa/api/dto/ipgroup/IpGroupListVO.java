package cn.iocoder.yudao.module.oa.api.dto.ipgroup;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * S-R12 修复：IP 组列表 VO（spec API-M1 §2.2 定义）
 */
@Data
public class IpGroupListVO {

    private Long id;
    private String groupName;
    /** 1=大组 2=小组 */
    private Integer groupType;
    private Long parentId;
    private String parentName;
    private String leaderName;
    private Integer memberCount;
    private Integer accountCount;
    private Integer anchorCount;
    /** 0=停用 1=启用 */
    private Integer status;
    private LocalDateTime createTime;
}
