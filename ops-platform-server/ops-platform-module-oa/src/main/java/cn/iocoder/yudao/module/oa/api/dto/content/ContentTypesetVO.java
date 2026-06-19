package cn.iocoder.yudao.module.oa.api.dto.content;

import lombok.Data;

@Data
public class ContentTypesetVO {

    private String html;
    /** Plain text before typeset (ADR-020 body preservation check) */
    private String plainTextBefore;
    private String plainTextAfter;
    private int rulesApplied;
    /** TEMPLATE | AUTO */
    private String mode;
    private Long templateId;
    /** Present when mode=TEMPLATE: merged layout instance */
    private Object layoutJson;
    private Integer overflowSegmentCount;
}
