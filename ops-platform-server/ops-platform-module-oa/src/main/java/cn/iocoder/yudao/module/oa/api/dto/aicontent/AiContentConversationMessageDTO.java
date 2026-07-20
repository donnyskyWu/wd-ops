package cn.iocoder.yudao.module.oa.api.dto.aicontent;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class AiContentConversationMessageDTO {

    @NotBlank
    private String role;
    @NotBlank
    private String content;
}
