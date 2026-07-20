package cn.iocoder.yudao.module.oa.service.collect.unified;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 微信公众号粉丝 follower-list 同步（M10-API-S-05）。
 * <p>
 * 明细粉丝列表已停用：改由 Football {@code shenyu-mp.mp_user} 维护；OPS 采集仅保留粉丝总数。
 */
@Service
@RequiredArgsConstructor
public class WechatMpFollowerSyncService {

    private final ChannelFollowerStatsSyncService channelFollowerStatsSyncService;

    /**
     * @deprecated 不再写入 {@code oa_wechat_mp_follower}；兼容旧任务时转调粉丝总数采集。
     */
    @Transactional
    public int syncFollowers(Long oaAccountId) {
        return channelFollowerStatsSyncService.syncWechatMpFollowerStats(oaAccountId);
    }
}
