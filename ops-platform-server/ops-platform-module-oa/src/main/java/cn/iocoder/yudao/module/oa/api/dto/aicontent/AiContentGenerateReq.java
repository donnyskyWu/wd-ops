package cn.iocoder.yudao.module.oa.api.dto.aicontent;

import cn.iocoder.yudao.module.oa.framework.dict.InDict;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class AiContentGenerateReq {

    @NotBlank
    private String sessionId;
    @NotNull
    private Long modelId;
    @NotBlank
    private String message;
    @NotNull
    @Valid
    private AiContentContextDTO context;
    private String preferenceSummary = "";
    private List<AiContentConversationMessageDTO> conversationHistory = new ArrayList<>();
    private Integer roundCount;
}
