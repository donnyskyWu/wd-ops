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
    /** 赛事展示名，用于填充 {eventinfo} / {competitionName} / {{match}} */
    private String competitionName;
    private Long taskId;
    /** 所属 IP 组（校验作者归属，可选） */
    private Long ipGroupId;
    /** 作者/主播 user id（member author_user_id） */
    private Long authorId;
    /** 作者展示名；未传时由 authorId 解析 */
    private String authorName;
    /** 历史战绩（可选） */
    private String historicalRecord;
    /** 赛事方向（可选） */
    private String matchDirection;
    /** 主播人设（可选） */
    private String streamerPersona;
    /** 修改意见（可选） */
    private String revisionFeedback;
    /** 篇幅类型 dict_content_length_type：SHORT / MEDIUM / LONG */
    @InDict("dict_content_length_type")
    private String lengthType;
}
