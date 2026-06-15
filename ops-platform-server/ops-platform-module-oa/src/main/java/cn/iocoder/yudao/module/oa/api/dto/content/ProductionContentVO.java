package cn.iocoder.yudao.module.oa.api.dto.content;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class ProductionContentVO {

    private Long id;
    private String title;
    private String body;
    private String bodyFormat;
    private Object layoutJson;
    private String layoutHtml;
    private Long layoutTemplateId;
    private String coverImage;
    private Long creatorUserId;
    private String creatorUserName;
    private Long accountId;
    private String accountName;
    private List<Long> accountIds;
    private List<String> accountNames;
    private String platformType;
    private List<String> platformTypes;
    private String contentType;
    private String status;
    private Integer aiGenerated;
    private Long taskId;
    private String competitionId;
    private String competitionName;
    private String documentType;
    private Long ipGroupId;
    private String ipGroupName;
    private Long authorId;
    private String authorName;
    private String generatedVideoUrl;
    private String finalVideoUrl;
    private LocalDateTime createTime;
    /** 是否已转知识库 */
    private Integer transferredToKnowledge;
    /** 关联知识库记录 ID */
    private Long knowledgeId;
    /** 审核流程进度（查看详情时返回） */
    private List<ContentReviewStepVO> reviewProgress;
}
