package cn.iocoder.yudao.module.oa.api.dto.ipgroup;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class IpGroupAnchorVO {

    private Long relId;
    /** DB anchor_user_id，语义为 member author_user.id（ADR-051） */
    private Long anchorUserId;
    /** API 兼容别名 */
    private Long authorId;
    private String anchorUserName;
    /** API 兼容别名：作者昵称 */
    private String authorName;
    private String anchorType;
    /** member author_user.author_level：0=作者 1=专家 */
    private Integer authorLevel;
    /** 作者所属 IP 组等级 dict_ip_group_level */
    private String ipGroupLevel;
    private LocalDateTime boundAt;
}
