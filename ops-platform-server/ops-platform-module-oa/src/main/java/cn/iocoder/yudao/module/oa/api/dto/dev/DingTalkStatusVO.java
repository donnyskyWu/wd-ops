package cn.iocoder.yudao.module.oa.api.dto.dev;

import lombok.Data;

@Data
public class DingTalkStatusVO {

    /** 任一通道可发（工作通知优先，或机器人降级可用） */
    private boolean sendEnabled;
    /** 主通道不可发时的原因；工作通知可用时为 null */
    private String skipReason;
    /** 主通道：work_notify | robot_webhook | none */
    private String primaryChannel;
    /** 工作通知（asyncsend_v2）是否可用 */
    private boolean workNotifyEnabled;
    private String workNotifySkipReason;
    private boolean agentIdConfigured;
    /** 群机器人降级通道是否启用 */
    private boolean robotFallbackEnabled;
    private String robotId;
    private boolean webhookConfigured;
    private boolean secretConfigured;
    /** 消息跳转链接是否已配置 */
    private boolean platformBaseUrlConfigured;
}
