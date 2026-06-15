package cn.iocoder.yudao.module.oa.framework.notification;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "oa.notification")
public class NotificationProperties {

    /** 前端平台根 URL，用于钉钉消息中的跳转链接 */
    private String platformBaseUrl = "";
}
