package cn.iocoder.yudao.module.oa.api.dto.content;

import cn.iocoder.yudao.module.oa.framework.dict.InDict;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ContentAiGenerateReq {

    @NotNull
    private Long modelId;
    @NotNull
    private Long promptId;
    @InDict("dict_content_type")
    private String contentType;
    @InDict("dict_document_type")
    private String documentType;
    /** 内容表单所选赛事 scheduleId，优先于 taskId */
    private String competitionId;
    /** 赛事展示名，用于填充 {eventinfo} / {competitionName} */
    private String competitionName;
    private Long taskId;
}
