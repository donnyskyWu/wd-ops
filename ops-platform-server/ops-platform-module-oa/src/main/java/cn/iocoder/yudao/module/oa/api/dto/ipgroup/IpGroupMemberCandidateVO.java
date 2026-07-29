package cn.iocoder.yudao.module.oa.api.dto.ipgroup;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

/**
 * IP 组添加成员候选人（UserSelect 数据源；不依赖 Football simple-list 数据权限过滤）。
 */
@Data
public class IpGroupMemberCandidateVO {

    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Long id;

    private String username;

    private String nickname;
}
