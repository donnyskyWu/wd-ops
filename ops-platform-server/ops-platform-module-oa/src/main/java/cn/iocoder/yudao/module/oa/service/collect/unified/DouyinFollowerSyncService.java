package cn.iocoder.yudao.module.oa.service.collect.unified;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 抖音粉丝 follower-list 同步（M10 P2 · Channel-A）。
 * <p>
 * 明细粉丝列表已停用；OPS 采集仅保留粉丝总数（{@code oa_account_status_log.follower_count}）。
 */
@Service
@RequiredArgsConstructor
public class DouyinFollowerSyncService {

    private final ChannelFollowerStatsSyncService channelFollowerStatsSyncService;

    /**
     * @deprecated 不再写入 {@code oa_douyin_follower}；兼容旧任务时转调粉丝总数采集。
     */
    @Transactional
    public int syncFollowers(Long oaAccountId) {
        return channelFollowerStatsSyncService.syncDouyinFollowerStats(oaAccountId);
    }
}
