package cn.iocoder.yudao.module.oa.dal.dataobject.football;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Football 发布方案 SSOT（shenyu-member.author_article，ADR-054）。
 */
@Data
@TableName("author_article")
public class AuthorArticleDO {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long authorId;

    private String title;

    private String intro;

    private String freeContent;

    private String content;

    private String privilegeTypes;

    private BigDecimal price;

    private Integer refundType;

    /** -1 草稿 / 0 下架 / 1 上架 / 2 审核中 / 3 预约 / 4 审核不通过 */
    private Integer status;

    private Integer matchType;

    private String matchScheme;

    private Integer sortNum;

    private LocalDateTime publishTime;

    private LocalDateTime orderDeadline;

    private Long tenantId;

    private String creator;

    private LocalDateTime createTime;

    private String updater;

    private LocalDateTime updateTime;

    @TableLogic
    private Integer deleted;
}
