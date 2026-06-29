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

    @Test
    @DisplayName("resolvePublishArticleId: 多图文 composite ID 还原 publish article_id")
    void resolvePublishArticleIdStripsItemIdx() {
        assertEquals("ARTICLE_HASH", WechatMpArticleStatsSyncService.resolvePublishArticleId("ARTICLE_HASH_2"));
        assertEquals("fp_pub_001", WechatMpArticleStatsSyncService.resolvePublishArticleId("fp_pub_001"));
    }

    @Test
    @DisplayName("resolveNewsItemIndex: composite ID 解析 0-based 下标")
    void resolveNewsItemIndexFromCompositeId() {
        assertEquals(0, WechatMpArticleStatsSyncService.resolveNewsItemIndex("ARTICLE_HASH"));
        assertEquals(1, WechatMpArticleStatsSyncService.resolveNewsItemIndex("ARTICLE_HASH_2"));
    }
}
