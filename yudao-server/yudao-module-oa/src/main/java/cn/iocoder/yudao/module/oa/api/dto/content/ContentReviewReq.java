package cn.iocoder.yudao.module.oa.api.dto.content;

import cn.iocoder.yudao.module.oa.framework.dict.InDict;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ContentReviewReq {

    @NotBlank
    @InDict("dict_content_review_result")
    private String action;
    @NotBlank
    @InDict("dict_review_stage")
    private String stage;
    private String comment;
}
