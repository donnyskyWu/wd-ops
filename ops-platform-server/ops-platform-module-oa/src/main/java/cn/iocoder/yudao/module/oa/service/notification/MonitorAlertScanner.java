package cn.iocoder.yudao.module.oa.service.notification;

import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.oa.dal.dataobject.account.AccountDO;
import cn.iocoder.yudao.module.oa.dal.dataobject.auth.SysTenantDO;
import cn.iocoder.yudao.module.oa.dal.dataobject.config.ThresholdConfigDO;
import cn.iocoder.yudao.module.oa.dal.dataobject.monitor.ExternalWorkDO;
import cn.iocoder.yudao.module.oa.dal.dataobject.operations.ContentDO;
import cn.iocoder.yudao.module.oa.dal.dataobject.operations.FollowerDailyDO;
import cn.iocoder.yudao.module.oa.dal.mysql.account.AccountMapper;
import cn.iocoder.yudao.module.oa.dal.mysql.auth.SysTenantMapper;
import cn.iocoder.yudao.module.oa.dal.mysql.config.ThresholdConfigMapper;
import cn.iocoder.yudao.module.oa.dal.mysql.monitor.ExternalWorkMapper;
import cn.iocoder.yudao.module.oa.dal.mysql.operations.ContentMapper;
import cn.iocoder.yudao.module.oa.dal.mysql.operations.FollowerDailyMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 定时扫描监控指标，触发低分/爆款作品与低粉/高粉账号通知。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MonitorAlertScanner {

    private static final long HIT_PLAY_THRESHOLD = 1_000_000L;
    private static final BigDecimal LOW_SCORE_THRESHOLD = new BigDecimal("0.20");
    private static final long DEFAULT_HIGH_FANS = 1_000_000L;
    private static final long DEFAULT_LOW_FANS = 10_000L;

    private final SysTenantMapper sysTenantMapper;
    private final ExternalWorkMapper externalWorkMapper;
    private final ContentMapper contentMapper;
    private final AccountMapper accountMapper;
    private final FollowerDailyMapper followerDailyMapper;
    private final ThresholdConfigMapper thresholdConfigMapper;
    private final NotificationService notificationService;

    @Scheduled(cron = "${oa.notification.monitor-scan-cron:0 0/30 * * * ?}")
    public void scanAlerts() {
        List<SysTenantDO> tenants = sysTenantMapper.selectList(new LambdaQueryWrapper<SysTenantDO>()
                .in(SysTenantDO::getStatus, List.of("NORMAL", "TRIAL", "ENABLED")));
        for (SysTenantDO tenant : tenants) {
            try {
                scanTenant(tenant.getId());
            } catch (Exception e) {
                log.warn("Monitor alert scan failed for tenant {}: {}", tenant.getId(), e.getMessage());
            }
        }
    }

    void scanTenant(Long tenantId) {
        TenantContextHolder.setTenantId(tenantId);
        try {
            scanExternalWorks(tenantId);
            scanInternalHits(tenantId);
            scanFollowerAlerts(tenantId);
        } finally {
            TenantContextHolder.clear();
        }
    }

    private void scanExternalWorks(Long tenantId) {
        List<ExternalWorkDO> works = externalWorkMapper.selectList(new LambdaQueryWrapper<ExternalWorkDO>()
                .eq(ExternalWorkDO::getTenantId, tenantId)
                .eq(ExternalWorkDO::getIsExternal, 1));
        for (ExternalWorkDO work : works) {
            if (work.getPlayCount() != null && work.getPlayCount() >= HIT_PLAY_THRESHOLD) {
                notificationService.notifyExternalWorkAlert(tenantId, work, NotificationEventType.WORK_HIT);
            }
            if (work.getCompletionRate() != null && work.getCompletionRate().compareTo(LOW_SCORE_THRESHOLD) < 0) {
                notificationService.notifyExternalWorkAlert(tenantId, work, NotificationEventType.WORK_LOW_SCORE);
            }
        }
    }

    private void scanInternalHits(Long tenantId) {
        List<ContentDO> hits = contentMapper.selectList(new LambdaQueryWrapper<ContentDO>()
                .eq(ContentDO::getTenantId, tenantId)
                .eq(ContentDO::getIsHit, 1));
        for (ContentDO work : hits) {
            notificationService.notifyInternalWorkHit(tenantId, work);
        }
    }

    private void scanFollowerAlerts(Long tenantId) {
        List<AccountDO> accounts = accountMapper.selectList(new LambdaQueryWrapper<AccountDO>()
                .eq(AccountDO::getTenantId, tenantId));
        Map<String, ThresholdConfigDO> fansConfigCache = new HashMap<>();
        for (AccountDO account : accounts) {
            FollowerDailyDO latest = followerDailyMapper.selectOne(new LambdaQueryWrapper<FollowerDailyDO>()
                    .eq(FollowerDailyDO::getTenantId, tenantId)
                    .eq(FollowerDailyDO::getAccountId, account.getId())
                    .orderByDesc(FollowerDailyDO::getStatDate)
                    .last("LIMIT 1"));
            if (latest == null || latest.getFollowerCount() == null || latest.getStatDate() == null) {
                continue;
            }
            long followerCount = latest.getFollowerCount();
            long highThreshold = resolveFansThreshold(tenantId, account.getPlatformType(), true, fansConfigCache);
            long lowThreshold = resolveFansThreshold(tenantId, account.getPlatformType(), false, fansConfigCache);
            if (followerCount >= highThreshold) {
                notificationService.notifyFollowerAlert(tenantId, account, followerCount, latest.getStatDate(),
                        NotificationEventType.ACCOUNT_HIGH_FANS);
            }
            if (followerCount <= lowThreshold) {
                notificationService.notifyFollowerAlert(tenantId, account, followerCount, latest.getStatDate(),
                        NotificationEventType.ACCOUNT_LOW_FANS);
            }
        }
    }

    private long resolveFansThreshold(Long tenantId, String platformType, boolean highFollower,
                                      Map<String, ThresholdConfigDO> cache) {
        String cacheKey = (platformType == null ? "*" : platformType) + ":" + highFollower;
        ThresholdConfigDO cfg = cache.get(cacheKey);
        if (cfg == null) {
            cfg = thresholdConfigMapper.selectOne(new LambdaQueryWrapper<ThresholdConfigDO>()
                    .eq(ThresholdConfigDO::getTenantId, tenantId)
                    .eq(ThresholdConfigDO::getThresholdCategory, "FANS")
                    .eq(ThresholdConfigDO::getStatus, "ENABLED")
                    .eq(platformType != null, ThresholdConfigDO::getPlatformType, platformType)
                    .orderByDesc(ThresholdConfigDO::getId)
                    .last("LIMIT 1"));
            cache.put(cacheKey, cfg);
        }
        if (cfg == null) {
            return highFollower ? DEFAULT_HIGH_FANS : DEFAULT_LOW_FANS;
        }
        if (highFollower) {
            return cfg.getHighFans() != null ? cfg.getHighFans() : DEFAULT_HIGH_FANS;
        }
        return cfg.getLowFans() != null ? cfg.getLowFans() : DEFAULT_LOW_FANS;
    }
}
