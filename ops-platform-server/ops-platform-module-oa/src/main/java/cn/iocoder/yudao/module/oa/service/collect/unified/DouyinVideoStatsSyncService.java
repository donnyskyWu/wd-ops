package cn.iocoder.yudao.module.oa.service.collect.unified;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import cn.iocoder.yudao.framework.common.exception.OaErrorCodes;
import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.module.oa.dal.dataobject.collect.CollectorAccountBindDO;
import cn.iocoder.yudao.module.oa.dal.dataobject.collect.DouyinVideoDO;
import cn.iocoder.yudao.module.oa.dal.mysql.collect.CollectorAccountBindMapper;
import cn.iocoder.yudao.module.oa.dal.mysql.collect.DouyinVideoMapper;
import cn.iocoder.yudao.module.oa.service.config.ConfigTenantSupport;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 抖音作品明细 video-stats 同步（M10 P2 · Channel-A）。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DouyinVideoStatsSyncService {

    private static final String BIND_STATUS_BOUND = "BOUND";
    /** 与 collector Guardian account_rate_max=10 对齐，单轮最多拉取明细条数 */
    private static final int MAX_DETAIL_FETCHES_PER_RUN = 8;
    private static final long INTER_CALL_DELAY_MS = 2_000L;
    private static final int MAX_RATE_LIMIT_RETRIES = 5;

    private final CollectorAccountBindMapper collectorAccountBindMapper;
    private final DouyinVideoMapper douyinVideoMapper;
    private final DouyinVideoSyncService douyinVideoSyncService;
    private final UnifiedCollectorApiClient unifiedCollectorApiClient;

    @Transactional
    public int syncVideoStats(Long oaAccountId) {
        Long tenantId = ConfigTenantSupport.requireTenantId();
        CollectorAccountBindDO bind = requireBoundCollector(oaAccountId, tenantId);

        List<DouyinVideoDO> videos = douyinVideoMapper.selectList(
                new LambdaQueryWrapper<DouyinVideoDO>()
                        .eq(DouyinVideoDO::getTenantId, tenantId)
                        .eq(DouyinVideoDO::getAccountId, oaAccountId)
                        .orderByDesc(DouyinVideoDO::getPublishedAt));
        if (videos.isEmpty()) {
            douyinVideoSyncService.syncVideos(oaAccountId);
            videos = douyinVideoMapper.selectList(
                    new LambdaQueryWrapper<DouyinVideoDO>()
                            .eq(DouyinVideoDO::getTenantId, tenantId)
                            .eq(DouyinVideoDO::getAccountId, oaAccountId));
        }
        if (videos.isEmpty()) {
            throw new ServiceException(OaErrorCodes.BAD_REQUEST.getCode(), "暂无作品可同步，请先执行作品列表采集");
        }

        LocalDateTime now = LocalDateTime.now();
        int synced = 0;
        int detailFetches = 0;
        boolean rateLimitHit = false;
        for (DouyinVideoDO video : videos) {
            if (hasStats(video)) {
                synced++;
                continue;
            }
            if (rateLimitHit || detailFetches >= MAX_DETAIL_FETCHES_PER_RUN) {
                break;
            }
            try {
                Map<String, Object> payload = fetchVideoStatsWithRetry(
                        bind.getCollectorAccountId(), video, detailFetches > 0);
                detailFetches++;
                JSONObject stats = JSONUtil.parseObj(JSONUtil.toJsonStr(payload));
                if (applyStats(video, stats, now)) {
                    ConfigTenantSupport.fillUpdate(video);
                    douyinVideoMapper.updateById(video);
                    synced++;
                }
            } catch (UnifiedCollectorApiException ex) {
                if (isRateLimit(ex)) {
                    rateLimitHit = true;
                    log.warn("抖音作品明细采集触发限流，已同步 {} 条，剩余 {} 条待下轮: {}",
                            synced, videos.size() - synced, ex.getMessage());
                    break;
                }
                throw ex;
            }
        }
        if (synced == 0 && detailFetches > 0) {
            throw new ServiceException(OaErrorCodes.BAD_REQUEST.getCode(),
                    "作品明细同步失败：Collector 限流，请稍后重试或减少单轮采集量");
        }
        return synced;
    }

    private Map<String, Object> fetchVideoStatsWithRetry(String collectorAccountId,
                                                         DouyinVideoDO video,
                                                         boolean throttle) throws UnifiedCollectorApiException {
        UnifiedCollectorApiException lastRateLimit = null;
        for (int attempt = 0; attempt < MAX_RATE_LIMIT_RETRIES; attempt++) {
            sleepBeforeFetch(throttle || attempt > 0, attempt);
            try {
                return unifiedCollectorApiClient.getDouyinVideoStats(
                        collectorAccountId, video.getVideoId(), video.getVideoUrl());
            } catch (UnifiedCollectorApiException ex) {
                if (!isRateLimit(ex)) {
                    throw ex;
                }
                lastRateLimit = ex;
            }
        }
        throw lastRateLimit != null ? lastRateLimit
                : new UnifiedCollectorApiException("DISCONNECTED", "Collector 限流重试耗尽");
    }

    private void sleepBeforeFetch(boolean throttle, int attempt) {
        if (!throttle && attempt == 0) {
            return;
        }
        long delay = INTER_CALL_DELAY_MS * Math.max(1, attempt + 1);
        try {
            Thread.sleep(delay);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new ServiceException(OaErrorCodes.BAD_REQUEST.getCode(), "作品明细同步被中断");
        }
    }

    private boolean hasStats(DouyinVideoDO video) {
        return video.getPlayCount() != null && video.getStatsSyncedAt() != null;
    }

    private boolean isRateLimit(UnifiedCollectorApiException ex) {
        String message = ex.getMessage();
        if (StrUtil.isBlank(message)) {
            return false;
        }
        return message.contains("account_rate_limit")
                || message.contains("global_rate_limit")
                || message.contains("频率超限")
                || message.contains("请求频率");
    }

    private boolean applyStats(DouyinVideoDO entity, JSONObject stats, LocalDateTime now) {
        Integer playCount = firstInt(stats, "play_count", "playCount");
        if (playCount == null) {
            return false;
        }
        entity.setPlayCount(playCount);
        entity.setLikeCount(firstInt(stats, "like_count", "digg_count", "likeCount"));
        entity.setShareCount(firstInt(stats, "share_count", "shareCount"));
        entity.setCommentCount(firstInt(stats, "comment_count", "commentCount"));
        entity.setCollectCount(firstInt(stats, "collect_count", "collectCount"));
        String title = stats.getStr("title");
        if (StrUtil.isNotBlank(title)) {
            entity.setTitle(title);
        }
        entity.setStatsSyncedAt(now);
        return true;
    }

    private CollectorAccountBindDO requireBoundCollector(Long oaAccountId, Long tenantId) {
        CollectorAccountBindDO bind = collectorAccountBindMapper.selectOne(
                new LambdaQueryWrapper<CollectorAccountBindDO>()
                        .eq(CollectorAccountBindDO::getTenantId, tenantId)
                        .eq(CollectorAccountBindDO::getOaAccountId, oaAccountId));
        if (bind == null || StrUtil.isBlank(bind.getCollectorAccountId())) {
            throw new ServiceException(OaErrorCodes.ENTITY_NOT_EXISTS.getCode(), "请先绑定 Collector 账号");
        }
        if (!BIND_STATUS_BOUND.equals(bind.getBindStatus())) {
            throw new ServiceException(2022, "Collector 账号未绑定成功，请先完成绑定");
        }
        return bind;
    }

    private Integer firstInt(JSONObject obj, String... keys) {
        for (String key : keys) {
            Object raw = obj.get(key);
            if (raw == null) {
                continue;
            }
            if (raw instanceof Number number) {
                return number.intValue();
            }
            String text = String.valueOf(raw);
            if (StrUtil.isNotBlank(text) && text.chars().allMatch(c -> Character.isDigit(c) || c == '-')) {
                return Integer.parseInt(text);
            }
        }
        return null;
    }
}
