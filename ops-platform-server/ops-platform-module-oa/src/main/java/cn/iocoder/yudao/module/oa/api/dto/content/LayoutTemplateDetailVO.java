package cn.iocoder.yudao.module.oa.api.dto.content;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class LayoutTemplateDetailVO extends LayoutTemplateVO {

    private Object layoutJson;
    private Object layoutSchema;
    private Integer schemaVersion;
    private String layoutHtml;
    private String previewHtml;
}
