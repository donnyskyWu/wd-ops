package cn.iocoder.yudao.module.oa.api.dto.content;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class LayoutMergePreviewReq {

    @NotBlank
    private String body;
    private Object existingLayoutJson;
    /** Optional param overrides merged into template default_params / globalStyles */
    private Object paramOverrides;
    /** partial-apply: block types to include (e.g. heading, slot, divider). Null = full merge */
    private java.util.List<String> includeBlockTypes;
    /** apply-background: only apply globalStyles without consuming body segments */
    private Boolean backgroundOnly;
}
