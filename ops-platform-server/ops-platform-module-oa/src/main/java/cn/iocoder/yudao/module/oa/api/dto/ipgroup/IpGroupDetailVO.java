package cn.iocoder.yudao.module.oa.api.dto.ipgroup;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class IpGroupDetailVO {

    private Long id;
    private String groupName;
    private Integer groupType;
    private Long parentId;
    private String parentName;
    /** UserSelect id (string in JSON to avoid JS snowflake precision loss) */
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Long leaderId;
    private String leaderName;
    private Integer sortOrder;
    private Integer status;
    /** dict_ip_group_level: S/A/B/C */
    private String level;
    private String remark;
    private Integer memberCount;
    private Integer accountCount;
    private Integer anchorCount;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
