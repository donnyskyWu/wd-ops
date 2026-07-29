package cn.iocoder.yudao.module.oa.api.dto.ipgroup;

import com.fasterxml.jackson.annotation.JsonFormat;
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
    /** UserSelect id (string in JSON to avoid JS snowflake precision loss) */
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Long leaderId;
    private String leaderName;
    private Integer memberCount;
    private Integer accountCount;
    private Integer anchorCount;
    private Integer status;
    /** dict_ip_group_level: S/A/B/C */
    private String level;
    private LocalDateTime createTime;
    private List<IpGroupTreeVO> children = new ArrayList<>();
}
