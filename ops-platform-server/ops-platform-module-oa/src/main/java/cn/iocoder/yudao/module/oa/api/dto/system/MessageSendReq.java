package cn.iocoder.yudao.module.oa.api.dto.system;

import cn.iocoder.yudao.module.oa.framework.dict.InDict;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

@Data
public class MessageSendReq {

    @NotBlank(message = "消息标题不能为空")
    private String title;

    @NotBlank(message = "消息分类不能为空")
    @InDict("dict_message_category")
    private String category;

    @NotEmpty(message = "发送渠道不能为空")
    private List<String> channels;

    @NotBlank(message = "接收人不能为空")
    private String receiver;

    @NotBlank(message = "消息内容不能为空")
    private String content;
}
