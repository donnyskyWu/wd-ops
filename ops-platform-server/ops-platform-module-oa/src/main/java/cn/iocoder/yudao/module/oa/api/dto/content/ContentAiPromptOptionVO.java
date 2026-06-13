package cn.iocoder.yudao.module.oa.api.dto.content;

import lombok.Data;

@Data
public class ContentAiPromptOptionVO {

    private Long id;
    private String templateName;
    private String scene;
    private String contentType;
    private String documentType;
}
