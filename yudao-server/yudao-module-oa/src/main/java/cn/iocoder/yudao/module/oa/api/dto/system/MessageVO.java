package cn.iocoder.yudao.module.oa.api.dto.system;

import lombok.Data;

@Data
public class MessageVO {

    private Long id;
    private String title;
    private String category;
    private String channel;
    private String receiver;
    private String content;
    private String status;
    private String failReason;
    private String sendTime;
    private String readTime;
    private Boolean read;
}
