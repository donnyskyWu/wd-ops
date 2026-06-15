package cn.iocoder.yudao.module.oa.framework.dingtalk;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "oa.dingtalk.robot")
public class DingTalkRobotProperties {

    /** 是否启用自定义机器人 Webhook 推送 */
    private boolean enabled = false;

    /**
     * 钉钉机器人 robotCode（开放平台「机器人」标识，形如 dingxxxx）。
     * 当前推送仍走 Webhook；本字段供配置归档及后续 Open API 扩展。
     */
    private String robotId = "";

    /** 机器人 Webhook URL（含 access_token） */
    private String webhookUrl = "";

    /** 加签密钥（SEC 开头，可选） */
    private String secret = "";
}
