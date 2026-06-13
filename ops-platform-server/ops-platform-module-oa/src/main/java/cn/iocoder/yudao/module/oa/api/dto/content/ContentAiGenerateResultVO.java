package cn.iocoder.yudao.module.oa.api.dto.content;

import lombok.Data;

@Data
public class ContentAiGenerateResultVO {

    private String content;
    private String title;
    /** Resolved event info injected into prompt */
    private String eventInfo;
    private Boolean mock;
    private String message;
}
