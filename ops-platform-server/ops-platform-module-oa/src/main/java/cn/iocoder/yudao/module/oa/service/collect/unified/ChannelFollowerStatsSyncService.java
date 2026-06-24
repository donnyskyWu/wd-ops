package cn.iocoder.yudao.module.oa.service.collect.unified;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.exception.OaErrorCodes;
import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.module.oa.dal.dataobject.analytics.AccountStatusLogDO;
import cn.iocoder.yudao.module.oa.dal.dataobject.collect.CollectorAccountBindDO;
import cn.iocoder.yudao.module.oa.dal.mysql.analytics.AccountStatusLogMapper;
import cn.iocoder.yudao.module.oa.dal.mysql.collect.CollectorAccountBindMapper;
import cn.iocoder.yudao.module.oa.service.config.ConfigTenantSupport;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Map;
import java.util.function.Function;

/**
 * Channel-A 粉丝统计 follower-stats 同步（M10-API-S-06 · Channel-A 平台）。
 */
@Service
@RequiredArgsConstructor
public class ChannelFollowerStatsSyncService {

    private static final String BIND_STATUS_BOUND = "BOUND";
    private static final String STATUS_NORMAL = "NORMAL";

    private final CollectorAccountBindMapper collectorAccountBindMapper;
    private final AccountStatusLogMapper accountStatusLogMapper;
    private final UnifiedCollectorApiClient unifiedCollectorApiClient;

    @Transactional
    public int syncWechatMpFollowerStats(Long oaAccountId) {
        return syncFollowerStats(oaAccountId, unifiedCollectorApiClient::getWechatMpFollowerStats);
    }

    @Transactional
    public int syncDouyinFollowerStats(Long oaAccountId) {
        return syncFollowerStats(oaAccountId, unifiedCollectorApiClient::getDouyinFollowerStats);
    }

    @Transactional
    public int syncKuaishouFollowerStats(Long oaAccountId) {
        return syncFollowerStats(oaAccountId, unifiedCollectorApiClient::getKuaishouFollowerStats);
    }

    @Transactional
    public int syncWechatVideoFollowerStats(Long oaAccountId) {
        return syncFollowerStats(oaAccountId, unifiedCollectorApiClient::getWechatVideoFollowerStats);
    }

    @Transactional
    public int syncXiaohongshuFollowerStats(Long oaAccountId) {
        return syncFollowerStats(oaAccountId, unifiedCollectorApiClient::getXiaohongshuFollowerStats);
    }

    @Transactional
    public int syncBilibiliFollowerStats(Long oaAccountId) {
        return syncFollowerStats(oaAccountId, unifiedCollectorApiClient::getBilibiliFollowerStats);
    }

    private int syncFollowerStats(Long oaAccountId, Function<String, Map<String, Object>> fetchStats) {
        Long tenantId = ConfigTenantSupport.requireTenantId();
        CollectorAccountBindDO bind = requireBoundCollector(oaAccountId, tenantId);

        Map<String, Object> payload = fetchStats.apply(bind.getCollectorAccountId());
        Long followerCount = extractFollowerCount(payload);
        if (followerCount == null) {
            throw new ServiceException(2022, "Collector 未返回粉丝数");
        }

        LocalDate statDate = LocalDate.now();
        AccountStatusLogDO existing = accountStatusLogMapper.selectOne(
                new LambdaQueryWrapper<AccountStatusLogDO>()
                        .eq(AccountStatusLogDO::getTenantId, tenantId)
                        .eq(AccountStatusLogDO::getAccountId, oaAccountId)
                        .eq(AccountStatusLogDO::getStatDate, statDate));
        if (existing == null) {
            AccountStatusLogDO entity = new AccountStatusLogDO();
            entity.setAccountId(oaAccountId);
            entity.setStatDate(statDate);
            entity.setStatus(STATUS_NORMAL);
            entity.setFollowerCount(followerCount);
            ConfigTenantSupport.fillCreate(entity);
            accountStatusLogMapper.insert(entity);
        } else {
            existing.setStatus(STATUS_NORMAL);
            existing.setFollowerCount(followerCount);
            ConfigTenantSupport.fillUpdate(existing);
            accountStatusLogMapper.updateById(existing);
        }
        return 1;
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

    private Long extractFollowerCount(Map<String, Object> payload) {
        if (payload == null || payload.isEmpty()) {
            return null;
        }
        Object raw = firstPresent(payload,
                "follower_count", "total_followers", "fans_count", "fan_count", "total_fans",
                "followerCount", "followers", "fans", "follower");
        if (raw == null) {
            return null;
        }
        if (raw instanceof Number number) {
            return number.longValue();
        }
        String text = String.valueOf(raw).trim();
        if (StrUtil.isBlank(text) || !text.chars().allMatch(Character::isDigit)) {
            return null;
        }
        return Long.parseLong(text);
    }

    private Object firstPresent(Map<String, Object> payload, String... keys) {
        for (String key : keys) {
            Object value = payload.get(key);
            if (value != null) {
                return value;
            }
        }
        return null;
    }
}
