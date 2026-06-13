package cn.iocoder.yudao.module.oa.api.dto.user;

import lombok.Data;

@Data
public class UserIpGroupVO {

    private Long ipGroupId;
    private String ipGroupName;
    private String groupType;
    private Long authorId;
    private String authorName;
}
