package cn.iocoder.yudao.module.oa.api.dto.content;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.List;

@Data
public class ContentTypesetReq {

    @NotBlank
    private String html;
    /** Plain-text body for template merge; falls back to extracted text from html */
    private String body;
    /** TEMPLATE = merge layout_schema; AUTO = rule-based optimization */
    private String mode;
    /** Direct template id for「按模板排版」 */
    private Long templateId;
    /** Rule codes to apply in AUTO mode; empty = all enabled (excluding TEMPLATE_LINK unless selected) */
    private List<String> ruleCodes;
    /** Optional style param overrides merged into template globalStyles */
    private Object paramOverrides;
}
