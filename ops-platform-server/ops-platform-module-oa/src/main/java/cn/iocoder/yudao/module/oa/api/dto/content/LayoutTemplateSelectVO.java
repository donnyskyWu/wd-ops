package cn.iocoder.yudao.module.oa.api.dto.content;

import lombok.Data;

@Data
public class LayoutTemplateSelectVO {

    private Long id;
    private String templateName;
    private String documentType;
    private String thumbnailUrl;
}
