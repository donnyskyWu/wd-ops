package cn.iocoder.yudao.module.oa.api.dto.aicontent;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

@Data
public class AiContentConversationSaveReq {

    @NotBlank
    private String sessionId;
    @NotEmpty
    @Valid
    private List<AiContentConversationMessageDTO> conversationHistory;
    private Long authorId;
    private Long contentId;
}
