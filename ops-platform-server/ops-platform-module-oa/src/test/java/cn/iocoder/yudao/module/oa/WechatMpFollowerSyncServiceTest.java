package cn.iocoder.yudao.module.oa;

import cn.hutool.json.JSONObject;
import cn.iocoder.yudao.module.oa.dal.dataobject.collect.WechatMpFollowerDO;
import cn.iocoder.yudao.module.oa.dal.mysql.account.AccountMapper;
import cn.iocoder.yudao.module.oa.dal.mysql.collect.CollectorAccountBindMapper;
import cn.iocoder.yudao.module.oa.dal.mysql.collect.WechatMpFollowerMapper;
import cn.iocoder.yudao.module.oa.service.collect.unified.UnifiedCollectorApiClient;
import cn.iocoder.yudao.module.oa.service.collect.unified.WechatMpFollowerSyncService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith(MockitoExtension.class)
class WechatMpFollowerSyncServiceTest {

    @Mock
    private AccountMapper accountMapper;
    @Mock
    private CollectorAccountBindMapper collectorAccountBindMapper;
    @Mock
    private WechatMpFollowerMapper wechatMpFollowerMapper;
    @Mock
    private UnifiedCollectorApiClient unifiedCollectorApiClient;

    @InjectMocks
    private WechatMpFollowerSyncService syncService;

    @Test
    @DisplayName("官方 batch 空 profile 不覆盖已有 avatar/nickname")
    void officialBatchDoesNotWipeExistingProfile() {
        WechatMpFollowerDO existing = new WechatMpFollowerDO();
        existing.setOpenid("oExisting001");
        existing.setNickname("已有昵称");
        existing.setAvatar("https://thirdwx.qlogo.cn/existing.png");
        existing.setSubscribedAt(LocalDateTime.of(2026, 1, 1, 0, 0));

        JSONObject emptyProfile = new JSONObject();
        emptyProfile.set("openid", "oExisting001");
        emptyProfile.set("subscribe_time", 1_735_689_600);

        invokeApplyFollowerFields(existing, emptyProfile);

        assertEquals("已有昵称", existing.getNickname());
        assertEquals("https://thirdwx.qlogo.cn/existing.png", existing.getAvatar());
    }

    @Test
    @DisplayName("解析嵌套 user_attr.headimgurl")
    void resolvesNestedAvatarField() {
        WechatMpFollowerDO entity = new WechatMpFollowerDO();
        JSONObject follower = new JSONObject();
        follower.set("openid", "oNested001");
        JSONObject userAttr = new JSONObject();
        userAttr.set("headimgurl", "https://thirdwx.qlogo.cn/nested.png");
        userAttr.set("nickname", "嵌套昵称");
        follower.set("user_attr", userAttr);

        invokeApplyFollowerFields(entity, follower);

        assertEquals("嵌套昵称", entity.getNickname());
        assertEquals("https://thirdwx.qlogo.cn/nested.png", entity.getAvatar());
    }

    @Test
    @DisplayName("headImgUrl 别名可映射为 avatar")
    void resolvesCamelCaseAvatarAlias() {
        WechatMpFollowerDO entity = new WechatMpFollowerDO();
        JSONObject follower = new JSONObject();
        follower.set("openid", "oAlias001");
        follower.set("headImgUrl", "https://thirdwx.qlogo.cn/alias.png");

        invokeApplyFollowerFields(entity, follower);

        assertEquals("https://thirdwx.qlogo.cn/alias.png", entity.getAvatar());
    }

    private void invokeApplyFollowerFields(WechatMpFollowerDO entity, JSONObject follower) {
        try {
            var method = WechatMpFollowerSyncService.class.getDeclaredMethod(
                    "applyFollowerFields", WechatMpFollowerDO.class, JSONObject.class, LocalDateTime.class);
            method.setAccessible(true);
            method.invoke(syncService, entity, follower, LocalDateTime.now());
        } catch (ReflectiveOperationException ex) {
            throw new AssertionError(ex);
        }
    }
}
