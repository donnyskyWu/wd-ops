package cn.iocoder.yudao.module.oa.api.dto.aicontent;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
public class AiContentConversationVO {

    private List<AiContentConversationMessageDTO> conversationHistory = new ArrayList<>();
    private Integer roundCount;
    private String sourceSessionId;
    private LocalDateTime savedAt;
}
