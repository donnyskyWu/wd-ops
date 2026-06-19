package cn.iocoder.yudao.module.oa.api.dto.content;

import lombok.Data;

@Data
public class LayoutImportJobVO {

    private Long id;
    private String status;
    private String sourceType;
    private String sourceUrl;
    private Object previewLayoutJson;
    private Object previewLayoutSchema;
    private Object extractionReport;
    /** Full-fidelity imported HTML for wizard preview (ADR-027 §4.2). */
    private String previewHtml;
    private String styleCss;
    private String suggestedName;
    private String errorMessage;
}
