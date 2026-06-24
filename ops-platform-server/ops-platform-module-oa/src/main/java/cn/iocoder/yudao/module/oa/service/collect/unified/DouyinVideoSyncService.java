package cn.iocoder.yudao.module.oa.service.collect.unified;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONArray;
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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 抖音作品 video-list 同步（M10 P2 · Channel-A）。
 */
@Service
@RequiredArgsConstructor
public class DouyinVideoSyncService {

    private static final String BIND_STATUS_BOUND = "BOUND";
    private static final int PAGE_SIZE = 50;

    private final CollectorAccountBindMapper collectorAccountBindMapper;
    private final DouyinVideoMapper douyinVideoMapper;
    private final UnifiedCollectorApiClient unifiedCollectorApiClient;

    @Transactional
    public int syncVideos(Long oaAccountId) {
        Long tenantId = ConfigTenantSupport.requireTenantId();
        CollectorAccountBindDO bind = requireBoundCollector(oaAccountId, tenantId);

        LocalDateTime now = LocalDateTime.now();
        int synced = 0;
        long cursor = 0;
        for (int page = 0; page < 100; page++) {
            Map<String, Object> payload = unifiedCollectorApiClient.getDouyinVideoList(
                    bind.getCollectorAccountId(), cursor, PAGE_SIZE);
            List<JSONObject> videos = extractVideos(payload);
            if (videos.isEmpty()) {
                break;
            }
            for (JSONObject video : videos) {
                if (upsertVideo(tenantId, oaAccountId, video, now)) {
                    synced++;
                }
            }
            if (!hasMore(payload)) {
                break;
            }
            Long nextCursor = nextCursor(payload);
            if (nextCursor == null || nextCursor == cursor) {
                break;
            }
            cursor = nextCursor;
        }
        return synced;
    }

    private boolean upsertVideo(Long tenantId, Long accountId, JSONObject video, LocalDateTime now) {
        String videoId = firstNonBlank(video, "video_id", "item_id", "aweme_id", "id");
        if (StrUtil.isBlank(videoId)) {
            return false;
        }
        DouyinVideoDO existing = douyinVideoMapper.selectOne(
                new LambdaQueryWrapper<DouyinVideoDO>()
                        .eq(DouyinVideoDO::getTenantId, tenantId)
                        .eq(DouyinVideoDO::getAccountId, accountId)
                        .eq(DouyinVideoDO::getVideoId, videoId));
        if (existing == null) {
            DouyinVideoDO entity = new DouyinVideoDO();
            entity.setAccountId(accountId);
            entity.setVideoId(videoId);
            applyVideoFields(entity, video, now, false);
            ConfigTenantSupport.fillCreate(entity);
            douyinVideoMapper.insert(entity);
            return true;
        }
        applyVideoFields(existing, video, now, false);
        ConfigTenantSupport.fillUpdate(existing);
        douyinVideoMapper.updateById(existing);
        return true;
    }

    private void applyVideoFields(DouyinVideoDO entity, JSONObject video, LocalDateTime now, boolean statsOnly) {
        if (!statsOnly) {
            entity.setTitle(firstNonBlank(video, "title", "desc", "description"));
            entity.setDescription(firstNonBlank(video, "description", "desc"));
            entity.setVideoUrl(firstNonBlank(video, "video_url", "url", "share_url"));
            entity.setCoverUrl(firstNonBlank(video, "cover_url", "cover"));
            entity.setDuration(firstInt(video, "duration"));
            entity.setPublishedAt(parsePublishTime(video));
            entity.setSyncedAt(now);
        }
        Integer playCount = firstInt(video, "play_count", "playCount");
        if (playCount != null) {
            entity.setPlayCount(playCount);
            entity.setLikeCount(firstInt(video, "like_count", "digg_count", "likeCount"));
            entity.setShareCount(firstInt(video, "share_count", "shareCount"));
            entity.setCommentCount(firstInt(video, "comment_count", "commentCount"));
            entity.setCollectCount(firstInt(video, "collect_count", "collectCount"));
            entity.setStatsSyncedAt(now);
        }
    }

    private List<JSONObject> extractVideos(Map<String, Object> payload) {
        if (payload == null || payload.isEmpty()) {
            return List.of();
        }
        Object raw = firstPresent(payload, "videos", "list", "items", "aweme_list");
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

    private Long nextCursor(Map<String, Object> payload) {
        Object raw = firstPresent(payload, "cursor", "max_cursor");
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

    private LocalDateTime parsePublishTime(JSONObject video) {
        Object raw = firstPresent(video, "publish_time", "published_at", "create_time");
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
        String text = String.valueOf(raw);
        if (StrUtil.isBlank(text) || !text.chars().allMatch(Character::isDigit)) {
            return null;
        }
        long epoch = Long.parseLong(text);
        if (epoch <= 0) {
            return null;
        }
        if (epoch > 1_000_000_000_000L) {
            epoch = epoch / 1000;
        }
        return LocalDateTime.ofInstant(Instant.ofEpochSecond(epoch), ZoneId.systemDefault());
    }
}
