package cn.iocoder.yudao.framework.common.biz.member.article.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Vendored subset of Football {@code ArticleSaveDTO} for OPS content bridge (G-MEM-03).
 */
@Data
public class ArticleSaveDTO {

    private Long id;
    private Long authorId;
    private String title;
    private String intro;
    private String freeContent;
    private String content;
    private Integer schedulePublishStatus;
    private List<Integer> privilegeTypes;
    private BigDecimal price;
    private Integer refundType;
    private Integer sortNum;
    private Integer status;
    private Integer matchType;
    private LocalDateTime orderDeadline;
    private LocalDateTime publishTime;
}
