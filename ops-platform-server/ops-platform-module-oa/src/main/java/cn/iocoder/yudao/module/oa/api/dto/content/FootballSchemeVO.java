package cn.iocoder.yudao.module.oa.api.dto.content;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * Football 发布方案桥接状态（ADR-054 §5.2 / §11.2）。
 */
@Data
public class FootballSchemeVO {

    private Long productionContentId;

    private Long authorArticleId;

    /** author_article.status：-1 草稿 / 0 下架 / 1 上架 … */
    private Integer shelfStatus;

    private String footballSyncError;

    private LocalDateTime syncFootballAt;
}
