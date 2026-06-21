package cn.iocoder.yudao.module.oa.service.config.aochuang;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class AochuangMessageDTO {

    private String messageId;
    private String friendId;
    private String msgType;
    /** SENT / RECEIVED */
    private String direction;
    private String content;
    private LocalDateTime messageTime;
}
