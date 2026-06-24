package cn.iocoder.yudao.module.oa.service.collect.unified;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import cn.iocoder.yudao.framework.common.exception.OaErrorCodes;
import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.module.oa.dal.dataobject.collect.CollectorAccountBindDO;
import cn.iocoder.yudao.module.oa.dal.dataobject.collect.DouyinFollowerDO;
import cn.iocoder.yudao.module.oa.dal.mysql.collect.CollectorAccountBindMapper;
import cn.iocoder.yudao.module.oa.dal.mysql.collect.DouyinFollowerMapper;
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
 * 抖音粉丝 follower-list 同步（M10 P2 · Channel-A）。
 */
@Service
@RequiredArgsConstructor
public class DouyinFollowerSyncService {

    private static final String BIND_STATUS_BOUND = "BOUND";
    private static final int PAGE_SIZE = 50;

    private final CollectorAccountBindMapper collectorAccountBindMapper;
    private final DouyinFollowerMapper douyinFollowerMapper;
    private final UnifiedCollectorApiClient unifiedCollectorApiClient;

    @Transactional
    public int syncFollowers(Long oaAccountId) {
        Long tenantId = ConfigTenantSupport.requireTenantId();
        CollectorAccountBindDO bind = requireBoundCollector(oaAccountId, tenantId);

        LocalDateTime now = LocalDateTime.now();
        int synced = 0;
        long cursor = 0;
        for (int page = 0; page < 100; page++) {
            Map<String, Object> payload = unifiedCollectorApiClient.getDouyinFollowerList(
                    bind.getCollectorAccountId(), cursor, PAGE_SIZE);
            List<JSONObject> followers = extractFollowers(payload);
            if (followers.isEmpty()) {
                break;
            }
            for (JSONObject follower : followers) {
                if (upsertFollower(tenantId, oaAccountId, follower, now)) {
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

    private boolean upsertFollower(Long tenantId, Long accountId, JSONObject follower, LocalDateTime now) {
        String followerId = firstNonBlank(follower, "follower_id", "open_id", "sec_uid", "uid");
        if (StrUtil.isBlank(followerId)) {
            return false;
        }
        DouyinFollowerDO existing = douyinFollowerMapper.selectOne(
                new LambdaQueryWrapper<DouyinFollowerDO>()
                        .eq(DouyinFollowerDO::getTenantId, tenantId)
                        .eq(DouyinFollowerDO::getAccountId, accountId)
                        .eq(DouyinFollowerDO::getFollowerId, followerId));
        if (existing == null) {
            DouyinFollowerDO entity = new DouyinFollowerDO();
            entity.setAccountId(accountId);
            entity.setFollowerId(followerId);
            applyFollowerFields(entity, follower, now);
            ConfigTenantSupport.fillCreate(entity);
            douyinFollowerMapper.insert(entity);
            return true;
        }
        applyFollowerFields(existing, follower, now);
        ConfigTenantSupport.fillUpdate(existing);
        douyinFollowerMapper.updateById(existing);
        return true;
    }

    private void applyFollowerFields(DouyinFollowerDO entity, JSONObject follower, LocalDateTime now) {
        entity.setNickname(firstNonBlank(follower, "nickname", "nick_name"));
        entity.setAvatar(firstNonBlank(follower, "avatar_url", "avatar", "headimgurl"));
        entity.setFollowedAt(parseFollowTime(follower));
        entity.setSyncedAt(now);
    }

    private List<JSONObject> extractFollowers(Map<String, Object> payload) {
        if (payload == null || payload.isEmpty()) {
            return List.of();
        }
        Object raw = firstPresent(payload, "followers", "list", "items", "user_list");
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

    private LocalDateTime parseFollowTime(JSONObject follower) {
        Object raw = firstPresent(follower, "follow_time", "followed_at", "subscribe_time");
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
