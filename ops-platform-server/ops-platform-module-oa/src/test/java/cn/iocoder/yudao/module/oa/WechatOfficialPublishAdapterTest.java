package cn.iocoder.yudao.module.oa;

import cn.iocoder.yudao.module.oa.dal.dataobject.account.AccountDO;
import cn.iocoder.yudao.module.oa.dal.dataobject.content.ProductionContentDO;
import cn.iocoder.yudao.module.oa.service.content.publish.PlatformPublishResult;
import cn.iocoder.yudao.module.oa.service.content.publish.WechatOfficialApiClient;
import cn.iocoder.yudao.module.oa.service.content.publish.WechatOfficialPublishAdapter;
import cn.iocoder.yudao.module.oa.util.AesUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WechatOfficialPublishAdapterTest {

    @Mock
    private WechatOfficialApiClient apiClient;

    @Mock
    private AesUtil aesUtil;

    @InjectMocks
    private WechatOfficialPublishAdapter adapter;

    private AccountDO account;
    private ProductionContentDO content;

    @BeforeEach
    void setUp() {
        account = new AccountDO();
        account.setId(9001L);
        account.setPlatformType("WECHAT_OFFICIAL");
        account.setUsageStatus("CERTIFIED");
        account.setAppId("wx_test");
        account.setAppSecretEncrypted("enc");

        content = new ProductionContentDO();
        content.setId(100L);
        content.setTitle("测试标题");
        content.setBody("测试摘要与正文");
        content.setLayoutHtml("<p>富文本正文</p>");
    }

    @Test
    void publishUsesLayoutHtmlAndReturnsDraftMediaId() {
        when(aesUtil.decrypt("enc")).thenReturn("secret");
        when(apiClient.getAccessToken("wx_test", "secret")).thenReturn("token");
        when(apiClient.resolveThumbBytes(null)).thenReturn(new byte[]{1, 2, 3});
        when(apiClient.uploadThumbMedia(eq("token"), any(), eq("cover.jpg"))).thenReturn("thumb_001");
        when(apiClient.addDraft(eq("token"), anyMap())).thenReturn("draft_media_001");

        PlatformPublishResult result = adapter.publish(content, account);

        assertTrue(result.isSuccess());
        assertFalse(result.isMock());
        assertEquals("draft_media_001", result.getExternalId());
        verify(apiClient).addDraft(eq("token"), anyMap());
    }

    @Test
    void publishFailsWhenNotCertified() {
        account.setUsageStatus("REGISTERED");

        PlatformPublishResult result = adapter.publish(content, account);

        assertFalse(result.isSuccess());
        assertTrue(result.getErrorMessage().contains("AppID/AppSecret"));
    }

    @Test
    void formalPublishUsesFreepublishSubmit() {
        when(aesUtil.decrypt("enc")).thenReturn("secret");
        when(apiClient.getAccessToken("wx_test", "secret")).thenReturn("token");
        when(apiClient.freepublishSubmit("token", "draft_media_001")).thenReturn("publish_001");

        PlatformPublishResult result = adapter.formalPublish(content, account, "draft_media_001");

        assertTrue(result.isSuccess());
        assertEquals("publish_001", result.getPublishId());
        verify(apiClient).freepublishSubmit("token", "draft_media_001");
    }
}
