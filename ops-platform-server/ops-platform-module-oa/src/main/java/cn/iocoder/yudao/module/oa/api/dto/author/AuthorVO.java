package cn.iocoder.yudao.module.oa.api.dto.author;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class AuthorVO {

    /** API 兼容：等于 author_user.id */
    private Long id;
    private Long authorUserId;
    private String authorName;
    /** member author_user.nickname */
    private String nickname;
    private Long ipGroupId;
    private String ipGroupName;
    private String authorType;
    private Long primaryAccountId;
    private String primaryAccountName;
    private Long userId;
    private String userName;
    private Integer status;
    private String remark;
    private LocalDateTime createTime;
}
