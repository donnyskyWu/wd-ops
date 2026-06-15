package cn.iocoder.yudao.module.oa.framework.dingtalk;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 钉钉工作通知客户端（企业内部应用 asyncsend_v2）。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DingTalkWorkNotifyClient {

    private final DingTalkProperties properties;
    private final DingTalkClient dingTalkClient;

    public boolean isEnabled() {
        return getSkipReason() == null;
    }

    /**
     * @return null if work notify can send; otherwise human-readable reason.
     */
    public String getSkipReason() {
        if (!properties.isEnabled()) {
            return "oa.dingtalk.enabled=false";
        }
        if (StrUtil.hasBlank(properties.getClientId(), properties.getClientSecret())) {
            return "oa.dingtalk client-id/client-secret 未配置";
        }
        if (properties.getAgentId() == null) {
            return "oa.dingtalk.agent-id 未配置";
        }
        return null;
    }

    public JSONObject sendText(String dingUserId, String content) {
        assertCanSend();
        return dingTalkClient.sendWorkNotification(dingUserId, content);
    }

    public JSONObject sendMarkdown(String dingUserId, String title, String markdownText) {
        assertCanSend();
        return dingTalkClient.sendWorkNotificationMarkdown(dingUserId, title, markdownText);
    }

    private void assertCanSend() {
        String skipReason = getSkipReason();
        if (skipReason != null) {
            log.warn("DingTalk work notify skip: {}", skipReason);
            throw new IllegalStateException(skipReason);
        }
    }
}
