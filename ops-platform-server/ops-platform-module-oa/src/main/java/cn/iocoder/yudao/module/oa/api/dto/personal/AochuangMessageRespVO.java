package cn.iocoder.yudao.module.oa.api.dto.personal;

import lombok.Data;

@Data
public class AochuangMessageRespVO {

    private Long id;
    private Long personalWechatId;
    private String aochuangWechatAccountId;
    private String aochuangMessageId;
    private String aochuangFriendId;
    private String msgType;
    private String direction;
    private String content;
    private String messageTime;
    private String syncedAt;
}
