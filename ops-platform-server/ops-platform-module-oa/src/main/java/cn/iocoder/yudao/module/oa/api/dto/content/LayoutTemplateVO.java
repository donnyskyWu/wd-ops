package cn.iocoder.yudao.module.oa.api.dto.content;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class LayoutTemplateVO {

    private Long id;
    private String templateName;
    private String contentType;
    private String documentType;
    private String description;
    private String sourceType;
    private String sourceUrl;
    private String status;
    private String thumbnailUrl;
    /** Inline preview snippet for list cards when thumbnailUrl absent. */
    private String previewHtml;
    private Long creatorUserId;
    private String creatorName;
    private LocalDateTime updateTime;
    private String tags;
    private Object defaultParams;
}
