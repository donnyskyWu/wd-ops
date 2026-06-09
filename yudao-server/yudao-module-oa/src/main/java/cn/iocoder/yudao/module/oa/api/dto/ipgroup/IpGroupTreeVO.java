package cn.iocoder.yudao.module.oa.api.dto.ipgroup;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
public class IpGroupTreeVO {

    private Long id;
    private String groupName;
    /** 1=大组 2=小组 */
    private Integer groupType;
    private Long parentId;
    private String parentName;
    private Long leaderId;
    private String leaderName;
    private Integer memberCount;
    private Integer accountCount;
    private Integer anchorCount;
    private Integer status;
    private LocalDateTime createTime;
    private List<IpGroupTreeVO> children = new ArrayList<>();
}
