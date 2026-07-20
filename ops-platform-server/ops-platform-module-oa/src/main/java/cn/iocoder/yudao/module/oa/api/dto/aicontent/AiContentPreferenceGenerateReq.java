package cn.iocoder.yudao.module.oa.api.dto.aicontent;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class AiContentPreferenceGenerateReq {

    @NotBlank
    private String sessionId;
    @NotNull
    @Valid
    private AiContentContextDTO context;
    @NotEmpty
    private List<AiContentConversationMessageDTO> conversationHistory;
    private Long authorId;
    private Long contentId;
    /** 本次会话中用户编辑过的偏好文本，作为提炼参考 */
    private String preferenceSummary;
}
