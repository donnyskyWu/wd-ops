package cn.iocoder.yudao.module.oa.api.dto.content;

import cn.iocoder.yudao.module.oa.framework.dict.InDict;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
public class ProductionContentUpdateReq {

    @NotNull
    private Long id;
    @Size(max = 200)
    private String title;
    @InDict("dict_content_type")
    private String contentType;
    @InDict("dict_platform_type")
    private String platformType;
    private List<String> platformTypes;
    private Long accountId;
    private List<Long> accountIds;
    private Long creatorUserId;
    private String body;
    @InDict("dict_content_body_format")
    private String bodyFormat;
    private Object layoutJson;
    private String layoutHtml;
    private Long layoutTemplateId;
    private String coverImage;
    private Integer aiGenerated;
    @InDict("dict_document_type")
    private String documentType;
    private Long ipGroupId;
    private Long authorId;
    private String generatedVideoUrl;
    private String finalVideoUrl;
    /** 外部赛事 scheduleId */
    private String competitionId;
    /** 赛事展示名快照 */
    private String competitionName;
}
