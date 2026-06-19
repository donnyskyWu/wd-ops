package cn.iocoder.yudao.module.oa.api.dto.content;

import cn.iocoder.yudao.module.oa.framework.dict.InDict;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class LayoutTemplateCreateReq {

    @NotBlank
    @Size(max = 100)
    private String templateName;
    @Size(max = 500)
    private String description;
    @InDict("dict_document_type")
    private String documentType;
    /** ADR-020 SSOT */
    private Object layoutSchema;
    /** v1 legacy fallback */
    private Object layoutJson;
    @NotBlank
    @InDict("dict_layout_template_status")
    private String status;
    /** Full visual HTML from import; overrides skeleton preview when present. */
    private String previewHtml;
    private String layoutHtml;
    private String styleCss;
    private String sourceType;
    private String sourceUrl;
}
