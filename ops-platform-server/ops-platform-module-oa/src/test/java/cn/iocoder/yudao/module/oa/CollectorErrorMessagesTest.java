package cn.iocoder.yudao.module.oa;

import cn.iocoder.yudao.module.oa.service.collect.unified.CollectorErrorMessages;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CollectorErrorMessagesTest {

    @Test
    @DisplayName("300333：视频号 Cookie 失效")
    void enrichesWechatVideoAuthError() {
        String raw = "API 调用失败: API [/auth/auth_data] Error 300333: request failed";
        String enriched = CollectorErrorMessages.enrich(raw);
        assertTrue(enriched.contains("300333"));
        assertTrue(enriched.contains("[原因: 视频号登录态/Cookie 已失效"));
    }

    @Test
    @DisplayName("Connection refused：Collector 不可达")
    void enrichesConnectionRefused() {
        String raw = "Collector 服务不可达，请确认 unify-collector-api 已启动（:8000）：Connection refused";
        String enriched = CollectorErrorMessages.enrich(raw);
        assertTrue(enriched.contains("Connection refused"));
        assertTrue(enriched.contains("[原因: unify-collector-api 未启动或网络不通"));
    }

    @Test
    @DisplayName("Collector API 40007 业务码")
    void enrichesCollectorBusinessCode() {
        String raw = "上游采集失败";
        String enriched = CollectorErrorMessages.enrich(raw, 40007);
        assertEquals("上游采集失败 [原因: 上游平台采集失败（Cookie 失效、风控拦截、熔断或接口未实现）]", enriched);
    }

    @Test
    @DisplayName("未知上游错误码")
    void enrichesUnknownUpstreamCode() {
        String raw = "API 调用失败: Error 999999: unknown";
        String enriched = CollectorErrorMessages.enrich(raw);
        assertTrue(enriched.contains("[原因: 未知错误码 999999，请联系运维]"));
    }

    @Test
    @DisplayName("已含原因标记时不重复追加")
    void skipsDoubleEnrich() {
        String already = "原始 [原因: 已有说明]";
        assertEquals(already, CollectorErrorMessages.enrich(already));
    }

    @Test
    @DisplayName("无匹配时不改动")
    void keepsPlainMessageWhenNoHint() {
        String raw = "采集任务未配置平台账号";
        assertEquals(raw, CollectorErrorMessages.enrich(raw));
    }

    @Test
    @DisplayName("部分失败摘要中的 MP_FOLLOWER_STATS 前缀场景")
    void enrichesPartialFailureSnippet() {
        String raw = "MP_FOLLOWER_STATS: Collector 服务不可达，请确认 unify-collector-api 已启动（:8000）：Connection refused";
        String enriched = CollectorErrorMessages.enrich(raw);
        assertTrue(enriched.startsWith("MP_FOLLOWER_STATS:"));
        assertTrue(enriched.contains("[原因: unify-collector-api 未启动或网络不通"));
    }
}
