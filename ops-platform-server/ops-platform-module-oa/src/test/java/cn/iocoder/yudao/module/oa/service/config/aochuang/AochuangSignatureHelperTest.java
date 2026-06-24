package cn.iocoder.yudao.module.oa.service.config.aochuang;

import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AochuangSignatureHelperTest {

    @Test
    void sign_matchesApifoxV2Example() {
        String uri = "/api/v1/wechatFriends";
        Map<String, String> params = new LinkedHashMap<>();
        params.put("lastUpdateTime", "2026-06-01 16:54:49");
        params.put("maxWechatFriendId", "6746efbf4985c27332cbc0fd284a4e5c4ae7493a");
        String payload = AochuangSignatureHelper.encodeQueryString(params);
        assertEquals(
                "lastUpdateTime=2026-06-01+16%3A54%3A49&maxWechatFriendId=6746efbf4985c27332cbc0fd284a4e5c4ae7493a",
                payload);

        String timestamp = "1717234489000";
        String accountName = "shxxx01";
        String secret = "75exxx71e";
        String signature = AochuangSignatureHelper.sign(uri, payload, timestamp, accountName, secret);
        assertEquals(40, signature.length());
    }
}
