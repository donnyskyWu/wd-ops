package cn.iocoder.yudao.module.oa.api.dto.content;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class KnowledgeVO {

    private Long id;
    private String title;
    private String content;
    private String category;
    private String tags;
    private Integer isPublic;
    private Integer status;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    /** P-GATE-UNMOCK-R S-R1 P0-1：creatorName 复用 DO.creator（username） */
    private String creatorName;
    /** P-GATE-UNMOCK-R S-R1 P0-1：留口，暂无 DB 字段支撑，固定 0（S-R2 补） */
    private Integer viewCount = 0;
    /** P-GATE-UNMOCK-R S-R1 P0-1：留口，暂无 DB 字段支撑，固定 0（S-R2 补） */
    private Integer likeCount = 0;
    /** P-GATE-UNMOCK-R S-R1 P0-1：当前用户是否已收藏，S-R1 不持久化，前端本地维护 */
    private Boolean isLiked = false;
}
