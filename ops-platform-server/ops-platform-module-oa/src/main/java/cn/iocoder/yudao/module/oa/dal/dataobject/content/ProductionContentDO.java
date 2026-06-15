package cn.iocoder.yudao.module.oa.dal.dataobject.content;

import cn.iocoder.yudao.framework.tenant.core.db.TenantBaseDO;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("oa_production_content")
public class ProductionContentDO extends TenantBaseDO {

    private String title;
    private String body;
    /** dict_content_body_format: PLAIN / LAYOUT */
    private String bodyFormat;
    /** 富版式 JSON（bodyFormat=LAYOUT 时 SSOT） */
    private String layoutJson;
    /** 消毒后 HTML（查看/审核渲染） */
    private String layoutHtml;
    /** 应用来源模板 FK（快照，非强绑定） */
    private Long layoutTemplateId;
    private String coverImage;
    private Long creatorUserId;
    private Long accountId;
    /** JSON array of account ids (multi-select) */
    private String accountIdsJson;
    private String platformType;
    /** JSON array of dict_platform_type values (multi-select) */
    private String platformTypesJson;
    private String contentType;
    private String status;
    private Integer aiGenerated;
    /** 关联任务（任务驱动创作，0..1） */
    private Long taskId;
    /** 外部赛事 scheduleId（继承任务） */
    private String competitionId;
    /** 赛事展示名快照 */
    private String competitionName;
    /** 文档类型（ARTICLE 时必填，dict_document_type） */
    private String documentType;
    private Long ipGroupId;
    private Long authorId;
    /** AI 生成视频 URL（BLK-M2-010 占位） */
    private String generatedVideoUrl;
    /** 最终视频：上传优先，否则取 generatedVideoUrl */
    private String finalVideoUrl;
    /** 是否已转知识库（0/1） */
    private Integer transferredToKnowledge;
    /** 关联知识库记录 FK */
    private Long knowledgeId;
}
