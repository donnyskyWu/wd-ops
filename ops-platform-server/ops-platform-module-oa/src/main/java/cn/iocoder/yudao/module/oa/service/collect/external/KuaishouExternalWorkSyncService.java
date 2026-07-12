package cn.iocoder.yudao.module.oa.service.collect.external;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.module.oa.dal.dataobject.config.CollectConfigDO;
import cn.iocoder.yudao.module.oa.dal.dataobject.monitor.ExternalAccountDO;
import cn.iocoder.yudao.module.oa.dal.dataobject.monitor.ExternalWorkDO;
import cn.iocoder.yudao.module.oa.dal.mysql.config.CollectConfigMapper;
import cn.iocoder.yudao.module.oa.dal.mysql.monitor.ExternalAccountMapper;
import cn.iocoder.yudao.module.oa.dal.mysql.monitor.ExternalWorkMapper;
import cn.iocoder.yudao.module.oa.service.config.CollectConfigScope;
import cn.iocoder.yudao.module.oa.service.config.ConfigTenantSupport;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 快手竞品 user-videos → {@code oa_external_account} / {@code oa_external_work}（ADR-052 §4.4 ✅）。
 */
@Service
@RequiredArgsConstructor
public class KuaishouExternalWorkSyncService {

    private static final String PLATFORM_KUAISHOU = "KUAISHOU";
    private static final String SUB_TYPE_ACCOUNT = "account";
    private static final String CONTENT_TYPE_SHORT_VIDEO = "SHORT_VIDEO";
    private static final int PAGE_SIZE = 20;

    private final CollectConfigMapper collectConfigMapper;
    private final ExternalAccountMapper externalAccountMapper;
    private final ExternalWorkMapper externalWorkMapper;
    private final TenantCollectorCredentialResolver credentialResolver;
    private final ExternalCollectorApiClient externalCollectorApiClient;

    @Transactional
    public int syncUserVideos(Long collectConfigId, String credentialProfile) {
        CollectConfigDO config = requireExternalAccountConfig(collectConfigId);
        ExternalCollectorCredential credential = credentialResolver.resolve(PLATFORM_KUAISHOU, credentialProfile);
        String userId = config.getAccountIdentifier();
        if (StrUtil.isBlank(userId)) {
            throw new ServiceException(1501, "竞品配置缺少 account_identifier");
        }

        Long tenantId = ConfigTenantSupport.requireTenantId();
        LocalDateTime now = LocalDateTime.now();
        ExternalAccountDO externalAccount = upsertExternalAccount(tenantId, config, userId, now);

        int synced = 0;
        String cursor = "";
        for (int page = 0; page < 100; page++) {
            Map<String, Object> payload = externalCollectorApiClient.getKuaishouUserVideos(
                    userId, cursor, PAGE_SIZE, credential);
            refreshAccountFromPayload(externalAccount, payload, now);
            List<JSONObject> videos = extractVideos(payload);
            if (videos.isEmpty()) {
                break;
            }
            for (JSONObject video : videos) {
                if (upsertWork(tenantId, config, externalAccount, video, now)) {
                    synced++;
                }
            }
            if (!hasMore(payload)) {
                break;
            }
            String nextCursor = nextCursor(payload);
            if (StrUtil.isBlank(nextCursor) || nextCursor.equals(cursor)) {
                break;
            }
            cursor = nextCursor;
        }
        externalAccount.setLastSyncedAt(now);
        ConfigTenantSupport.fillUpdate(externalAccount);
        externalAccountMapper.updateById(externalAccount);
        return synced;
    }

    private CollectConfigDO requireExternalAccountConfig(Long collectConfigId) {
        CollectConfigDO config = ConfigTenantSupport.getRequiredInTenant(
                collectConfigMapper.selectById(collectConfigId));
        if (!CollectConfigScope.EXTERNAL.equals(config.getScope())) {
            throw new ServiceException(1504, "collect_config 须为 scope=EXTERNAL");
        }
        if (!SUB_TYPE_ACCOUNT.equals(config.getSubType())) {
            throw new ServiceException(1501, "collect_config 须为 sub_type=account");
        }
        if (!PLATFORM_KUAISHOU.equals(config.getPlatformType())) {
            throw new ServiceException(1501, "collect_config 平台须为 KUAISHOU");
        }
        return config;
    }

    private ExternalAccountDO upsertExternalAccount(Long tenantId, CollectConfigDO config,
                                                      String userId, LocalDateTime now) {
        ExternalAccountDO existing = externalAccountMapper.selectOne(
                new LambdaQueryWrapper<ExternalAccountDO>()
                        .eq(ExternalAccountDO::getTenantId, tenantId)
                        .eq(ExternalAccountDO::getCollectConfigId, config.getId()));
        if (existing != null) {
            existing.setExternalUserId(userId);
            existing.setPlatformType(config.getPlatformType());
            existing.setLastSyncedAt(now);
            ConfigTenantSupport.fillUpdate(existing);
            externalAccountMapper.updateById(existing);
            return existing;
        }
        ExternalAccountDO entity = new ExternalAccountDO();
        entity.setCollectConfigId(config.getId());
        entity.setPlatformType(config.getPlatformType());
        entity.setExternalUserId(userId);
        entity.setDisplayName(config.getConfigName());
        entity.setFollowerCount(0L);
        entity.setWorkCount(0);
        entity.setLastSyncedAt(now);
        ConfigTenantSupport.fillCreate(entity);
        externalAccountMapper.insert(entity);
        return entity;
    }

    private void refreshAccountFromPayload(ExternalAccountDO account, Map<String, Object> payload, LocalDateTime now) {
        if (payload == null || payload.isEmpty()) {
            return;
        }
        Object userName = firstPresent(payload, "user_name", "nickname");
        if (userName != null && StrUtil.isNotBlank(String.valueOf(userName))) {
            account.setDisplayName(String.valueOf(userName));
        }
        Integer fanCount = firstIntMap(payload, "fan_count", "follower_count", "fans_count");
        if (fanCount != null) {
            account.setFollowerCount(fanCount.longValue());
        }
        account.setLastSyncedAt(now);
    }

    private boolean upsertWork(Long tenantId, CollectConfigDO config, ExternalAccountDO externalAccount,
                               JSONObject video, LocalDateTime now) {
        String platformWorkId = firstNonBlank(video, "photo_id", "id", "video_id");
        if (StrUtil.isBlank(platformWorkId)) {
            return false;
        }
        ExternalWorkDO existing = externalWorkMapper.selectOne(
                new LambdaQueryWrapper<ExternalWorkDO>()
                        .eq(ExternalWorkDO::getTenantId, tenantId)
                        .eq(ExternalWorkDO::getPlatformType, PLATFORM_KUAISHOU)
                        .eq(ExternalWorkDO::getPlatformWorkId, platformWorkId));
        if (existing == null) {
            ExternalWorkDO entity = new ExternalWorkDO();
            entity.setAccountId(externalAccount.getId());
            entity.setCollectConfigId(config.getId());
            entity.setPlatformType(PLATFORM_KUAISHOU);
            entity.setPlatformWorkId(platformWorkId);
            entity.setContentType(CONTENT_TYPE_SHORT_VIDEO);
            entity.setIsExternal(1);
            applyWorkFields(entity, video);
            ConfigTenantSupport.fillCreate(entity);
            externalWorkMapper.insert(entity);
            return true;
        }
        existing.setAccountId(externalAccount.getId());
        existing.setCollectConfigId(config.getId());
        applyWorkFields(existing, video);
        ConfigTenantSupport.fillUpdate(existing);
        externalWorkMapper.updateById(existing);
        return true;
    }

    private void applyWorkFields(ExternalWorkDO entity, JSONObject video) {
        entity.setTitle(firstNonBlank(video, "caption", "title", "description"));
        if (StrUtil.isBlank(entity.getTitle())) {
            entity.setTitle("未命名作品");
        }
        entity.setWorkUrl(resolveWorkUrl(video));
        entity.setPlayCount(firstLong(video, "view_count", "play_count", "playCount"));
        entity.setLikeCount(firstInt(video, "like_count", "likeCount"));
        entity.setCommentCount(firstInt(video, "comment_count", "commentCount"));
        entity.setPublishTime(parsePublishTime(video));
    }

    private String resolveWorkUrl(JSONObject video) {
        String direct = firstNonBlank(video, "share_url", "video_url", "url", "work_url");
        if (StrUtil.isNotBlank(direct)) {
            return direct;
        }
        String photoId = firstNonBlank(video, "photo_id", "id", "video_id");
        if (StrUtil.isNotBlank(photoId)) {
            return "https://www.kuaishou.com/short-video/" + photoId;
        }
        return null;
    }

    private List<JSONObject> extractVideos(Map<String, Object> payload) {
        if (payload == null || payload.isEmpty()) {
            return List.of();
        }
        Object raw = firstPresent(payload, "videos", "items", "list", "photo_list");
        return toJSONObjectList(raw);
    }

    private List<JSONObject> toJSONObjectList(Object raw) {
        if (raw instanceof JSONArray array) {
            return array.toList(JSONObject.class);
        }
        if (raw instanceof List<?> list) {
            List<JSONObject> result = new ArrayList<>();
            for (Object item : list) {
                JSONObject obj = toJSONObject(item);
                if (obj != null) {
                    result.add(obj);
                }
            }
            return result;
        }
        if (raw != null && JSONUtil.isTypeJSONArray(String.valueOf(raw))) {
            return JSONUtil.parseArray(String.valueOf(raw)).toList(JSONObject.class);
        }
        return List.of();
    }

    private JSONObject toJSONObject(Object value) {
        if (value instanceof JSONObject jsonObject) {
            return jsonObject;
        }
        if (value instanceof Map<?, ?> map) {
            return JSONUtil.parseObj(JSONUtil.toJsonStr(map));
        }
        return null;
    }

    private boolean hasMore(Map<String, Object> payload) {
        Object raw = payload.get("has_more");
        if (raw instanceof Boolean bool) {
            return bool;
        }
        if (raw instanceof Number number) {
            return number.intValue() != 0;
        }
        return false;
    }

    private String nextCursor(Map<String, Object> payload) {
        Object raw = firstPresent(payload, "cursor", "next_cursor");
        return raw == null ? null : String.valueOf(raw);
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

    private Object firstPresent(JSONObject obj, String... keys) {
        for (String key : keys) {
            Object value = obj.get(key);
            if (value != null) {
                return value;
            }
        }
        return null;
    }

    private String firstNonBlank(JSONObject obj, String... keys) {
        for (String key : keys) {
            String value = obj.getStr(key);
            if (StrUtil.isNotBlank(value)) {
                return value;
            }
        }
        return null;
    }

    private Integer firstInt(JSONObject obj, String... keys) {
        Object raw = firstPresent(obj, keys);
        return toInteger(raw);
    }

    private Integer firstIntMap(Map<String, Object> payload, String... keys) {
        Object raw = firstPresent(payload, keys);
        return toInteger(raw);
    }

    private Integer toInteger(Object raw) {
        if (raw == null) {
            return null;
        }
        if (raw instanceof Number number) {
            return number.intValue();
        }
        String text = String.valueOf(raw);
        if (StrUtil.isBlank(text) || !text.chars().allMatch(c -> Character.isDigit(c) || c == '-')) {
            return null;
        }
        return Integer.parseInt(text);
    }

    private Long firstLong(JSONObject obj, String... keys) {
        Integer value = firstInt(obj, keys);
        return value == null ? 0L : value.longValue();
    }

    private LocalDateTime parsePublishTime(JSONObject video) {
        Object raw = firstPresent(video, "create_time", "publish_time", "published_at");
        if (raw == null) {
            return null;
        }
        if (raw instanceof Number number) {
            long epoch = number.longValue();
            if (epoch <= 0) {
                return null;
            }
            if (epoch > 1_000_000_000_000L) {
                epoch = epoch / 1000;
            }
            return LocalDateTime.ofInstant(Instant.ofEpochSecond(epoch), ZoneId.systemDefault());
        }
        return null;
    }
}
