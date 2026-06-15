package cn.iocoder.yudao.module.oa;

import cn.iocoder.yudao.module.oa.framework.dingtalk.DingTalkRobotClient;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DingTalkRobotClientTest {

    @Test
    @DisplayName("无 secret 时返回原始 webhook URL")
    void signedUrlWithoutSecret() {
        String url = "https://oapi.dingtalk.com/robot/send?access_token=abc";
        assertEquals(url, DingTalkRobotClient.signedUrl(url, ""));
    }

    @Test
    @DisplayName("有 secret 时追加 timestamp 与 sign 参数")
    void signedUrlWithSecret() {
        String url = "https://oapi.dingtalk.com/robot/send?access_token=abc";
        String signed = DingTalkRobotClient.signedUrl(url, "SECtest");
        assertTrue(signed.startsWith(url + "&timestamp="));
        assertTrue(signed.contains("&sign="));
    }

    @Test
    @DisplayName("HMAC 签名可重复计算")
    void signIsDeterministicForTimestamp() {
        String first = DingTalkRobotClient.sign(1700000000000L, "SECtest");
        String second = DingTalkRobotClient.sign(1700000000000L, "SECtest");
        assertEquals(first, second);
    }
}
