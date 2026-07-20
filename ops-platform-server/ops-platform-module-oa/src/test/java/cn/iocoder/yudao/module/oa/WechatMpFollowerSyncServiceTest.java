package cn.iocoder.yudao.module.oa;

import cn.iocoder.yudao.module.oa.service.collect.unified.ChannelFollowerStatsSyncService;
import cn.iocoder.yudao.module.oa.service.collect.unified.WechatMpFollowerSyncService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WechatMpFollowerSyncServiceTest {

    @Mock
    private ChannelFollowerStatsSyncService channelFollowerStatsSyncService;

    @InjectMocks
    private WechatMpFollowerSyncService syncService;

    @Test
    @DisplayName("MP_FOLLOWER_LIST 兼容路由：转调粉丝总数采集")
    void syncFollowersDelegatesToStats() {
        when(channelFollowerStatsSyncService.syncWechatMpFollowerStats(9001L)).thenReturn(1);

        int count = syncService.syncFollowers(9001L);

        assertEquals(1, count);
        verify(channelFollowerStatsSyncService).syncWechatMpFollowerStats(9001L);
    }
}
