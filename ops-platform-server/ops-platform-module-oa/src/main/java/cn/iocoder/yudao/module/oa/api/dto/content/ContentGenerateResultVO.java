package cn.iocoder.yudao.module.oa.api.dto.content;

import lombok.Data;

@Data
public class ContentGenerateResultVO {

    /** ARTICLE: generated text; SHORT_VIDEO: unused */
    private String body;
    /** SHORT_VIDEO: mock/generated video URL */
    private String generatedVideoUrl;
    /** Whether M8 prompt was found (false = mock placeholder, BLK-M2-005) */
    private Boolean promptMatched;
    private String message;
}
