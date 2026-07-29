package cn.iocoder.yudao.module.oa.api.dto.ipgroup;

import cn.iocoder.yudao.module.oa.framework.dict.InDict;
import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class IpGroupUpdateReq {

    @NotNull
    private Long id;
    private String groupName;
    /**
     * 上级组 ID（仅小组可设置，大组必须为 null）
     * P-GATE-UNMOCK S-E: 后端补字段，原 spec 缺漏
     */
    private Long parentId;
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Long leaderId;
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Long leaderUserId;
    private Integer sortOrder;
    private Integer status;
    @InDict("dict_ip_group_level")
    private String level;
    private String remark;
}
