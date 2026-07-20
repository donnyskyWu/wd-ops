package cn.iocoder.yudao.module.oa.api.dto.author;

import cn.iocoder.yudao.module.oa.framework.dict.InDict;
import lombok.Data;

@Data
public class AuthorExtVO {

    private Long authorUserId;
    private String authorName;
    private Long ipGroupId;
    private String ipGroupName;
    private String authorType;
    private Long primaryAccountId;
    private String primaryAccountName;
    private Integer status;
    private String remark;
}
