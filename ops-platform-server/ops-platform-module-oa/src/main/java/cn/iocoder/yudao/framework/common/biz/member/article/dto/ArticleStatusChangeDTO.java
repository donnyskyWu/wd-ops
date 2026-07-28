package cn.iocoder.yudao.framework.common.biz.member.article.dto;

import lombok.Data;

/**
 * Vendored subset of Football {@code ArticleStatusChangeDTO} (G-MEM-03).
 */
@Data
public class ArticleStatusChangeDTO {

    private Long id;
    /** 0 off / 1 on */
    private Integer status;
}
