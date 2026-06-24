package cn.iocoder.yudao.module.oa;

import cn.iocoder.yudao.module.oa.service.collect.unified.WechatMpArticleStatsSyncService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class WechatMpArticleStatsSyncServiceTest {

    @Test
    @DisplayName("resolveMsgid: 纯数字 appmsgid 补全 itemidx")
    void resolveMsgidAppendsItemIdxForNumericId() {
        assertEquals("2651883347_1", WechatMpArticleStatsSyncService.resolveMsgid("2651883347"));
    }

    @Test
    @DisplayName("resolveMsgid: 已是 appmsgid_itemidx 则原样返回")
    void resolveMsgidKeepsCompositeId() {
        assertEquals("2651883347_2", WechatMpArticleStatsSyncService.resolveMsgid("2651883347_2"));
    }

    @Test
    @DisplayName("resolveMsgid: stub/非数字 ID 不追加后缀")
    void resolveMsgidKeepsStubId() {
        assertEquals("stub_article_001", WechatMpArticleStatsSyncService.resolveMsgid("stub_article_001"));
    }

    @Test
    @DisplayName("resolveMsgid: 空值返回 null")
    void resolveMsgidBlank() {
        assertNull(WechatMpArticleStatsSyncService.resolveMsgid(null));
        assertNull(WechatMpArticleStatsSyncService.resolveMsgid(""));
    }
}
