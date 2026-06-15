package cn.iocoder.yudao.module.oa.framework.dingtalk;

import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;

/**
 * 钉钉自定义机器人 Webhook 客户端。
 * 文档：https://open.dingtalk.com/document/development/custom-robot-access
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DingTalkRobotClient {

    private final DingTalkRobotProperties properties;

    public boolean isEnabled() {
        return getSkipReason() == null;
    }

    /**
     * @return null if robot can send; otherwise human-readable reason (for logs / dev diagnostics).
     */
    public String getSkipReason() {
        if (!properties.isEnabled()) {
            return "oa.dingtalk.robot.enabled=false";
        }
        if (StrUtil.isBlank(properties.getWebhookUrl())) {
            return "oa.dingtalk.robot.webhook-url 未配置（robot-id 不能替代 Webhook access_token）";
        }
        return null;
    }

    public void sendMarkdown(String title, String markdownText, List<String> atUserIds) {
        String skipReason = getSkipReason();
        if (skipReason != null) {
            log.warn("DingTalk robot skip [{}]: {}", title, skipReason);
            return;
        }
        JSONObject body = JSONUtil.createObj()
                .set("msgtype", "markdown")
                .set("markdown", JSONUtil.createObj()
                        .set("title", title)
                        .set("text", markdownText));
        if (atUserIds != null && !atUserIds.isEmpty()) {
            body.set("at", JSONUtil.createObj()
                    .set("atUserIds", atUserIds)
                    .set("isAtAll", false));
        }
        String url = signedUrl(properties.getWebhookUrl(), properties.getSecret());
        HttpResponse response = HttpRequest.post(url)
                .header("Content-Type", "application/json")
                .body(body.toString())
                .timeout(15_000)
                .execute();
        JSONObject result = JSONUtil.parseObj(response.body());
        Integer errcode = result.getInt("errcode");
        if (errcode != null && errcode != 0) {
            log.warn("DingTalk robot send failed: {} {}", errcode, result.getStr("errmsg"));
            throw new IllegalStateException("钉钉机器人推送失败: " + result.getStr("errmsg", response.body()));
        }
    }

    public static String signedUrl(String webhookUrl, String secret) {
        if (StrUtil.isBlank(secret)) {
            return webhookUrl;
        }
        long timestamp = System.currentTimeMillis();
        String sign = sign(timestamp, secret);
        String separator = webhookUrl.contains("?") ? "&" : "?";
        return webhookUrl + separator + "timestamp=" + timestamp + "&sign=" + sign;
    }

    public static String sign(long timestamp, String secret) {
        try {
            String stringToSign = timestamp + "\n" + secret;
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] signData = mac.doFinal(stringToSign.getBytes(StandardCharsets.UTF_8));
            return URLEncoder.encode(Base64.getEncoder().encodeToString(signData), StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new IllegalStateException("钉钉签名计算失败", e);
        }
    }
}
