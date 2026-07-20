package cn.iocoder.yudao.module.oa.dal.dataobject.content;

import cn.iocoder.yudao.framework.tenant.core.db.TenantBaseDO;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * OPS 内容生产 ↔ Football author_article 桥接扩展（ADR-054 §4）。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("oa_production_content_ext")
public class ProductionContentExtDO extends TenantBaseDO {

    private Long productionContentId;

    private Long authorArticleId;

    private Long ipGroupId;

    private Long taskId;

    private String schemeTypes;

    private String competitionId;

    private String competitionName;

    private LocalDateTime syncFootballAt;

    private String footballSyncError;

    /** OPS / AMPHIPODA_LEGACY */
    private String source;
}
