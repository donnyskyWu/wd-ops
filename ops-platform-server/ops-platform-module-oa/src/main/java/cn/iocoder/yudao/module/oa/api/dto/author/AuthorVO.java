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
    /** member author_user.author_level：0=作者 1=专家 */
    private Integer authorLevel;
    /** 作者所属 IP 组等级 dict_ip_group_level（便于选择器回填） */
    private String ipGroupLevel;
    private Long primaryAccountId;
    private String primaryAccountName;
    private Long userId;
    private String userName;
    private Integer status;
    private String remark;
    private LocalDateTime createTime;
}
