package cn.iocoder.yudao.module.oa.service.collect.unified;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import cn.iocoder.yudao.framework.common.exception.OaErrorCodes;
import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.module.oa.dal.dataobject.collect.CollectorAccountBindDO;
import cn.iocoder.yudao.module.oa.dal.dataobject.collect.WechatMpFollowerDO;
import cn.iocoder.yudao.module.oa.dal.mysql.collect.CollectorAccountBindMapper;
import cn.iocoder.yudao.module.oa.dal.mysql.collect.WechatMpFollowerMapper;
import cn.iocoder.yudao.module.oa.service.config.ConfigTenantSupport;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;

/**
 * 微信公众号粉丝 follower-list 同步（M10-API-S-05）。
 */
@Service
@RequiredArgsConstructor
public class WechatMpFollowerSyncService {

    private static final String BIND_STATUS_BOUND = "BOUND";

    private final CollectorAccountBindMapper collectorAccountBindMapper;
    private final WechatMpFollowerMapper wechatMpFollowerMapper;
    private final UnifiedCollectorApiClient unifiedCollectorApiClient;

    @Transactional
    public int syncFollowers(Long oaAccountId) {
        Long tenantId = ConfigTenantSupport.requireTenantId();
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

        Map<String, Object> payload = unifiedCollectorApiClient.getWechatMpFollowerList(bind.getCollectorAccountId());
        List<JSONObject> followers = extractFollowers(payload);
        LocalDateTime now = LocalDateTime.now();
        int synced = 0;
        for (JSONObject follower : followers) {
            if (upsertFollower(tenantId, oaAccountId, follower, now)) {
                synced++;
            }
        }
        return synced;
    }

    private boolean upsertFollower(Long tenantId, Long accountId, JSONObject follower, LocalDateTime now) {
        String openid = firstNonBlank(follower, "openid", "user_openid", "user_name");
        if (StrUtil.isBlank(openid)) {
            return false;
        }
        WechatMpFollowerDO existing = wechatMpFollowerMapper.selectOne(
                new LambdaQueryWrapper<WechatMpFollowerDO>()
                        .eq(WechatMpFollowerDO::getTenantId, tenantId)
                        .eq(WechatMpFollowerDO::getAccountId, accountId)
                        .eq(WechatMpFollowerDO::getOpenid, openid));
        if (existing == null) {
            WechatMpFollowerDO entity = new WechatMpFollowerDO();
            entity.setAccountId(accountId);
            entity.setOpenid(openid);
            applyFollowerFields(entity, follower, now);
            ConfigTenantSupport.fillCreate(entity);
            wechatMpFollowerMapper.insert(entity);
            return true;
        }
        applyFollowerFields(existing, follower, now);
        ConfigTenantSupport.fillUpdate(existing);
        wechatMpFollowerMapper.updateById(existing);
        return true;
    }

    private void applyFollowerFields(WechatMpFollowerDO entity, JSONObject follower, LocalDateTime now) {
        entity.setNickname(firstNonBlank(follower, "nickname", "nick_name"));
        entity.setAvatar(firstNonBlank(follower, "headimgurl", "avatar", "head_img"));
        entity.setUnionid(firstNonBlank(follower, "unionid"));
        entity.setSubscribedAt(parseSubscribeTime(follower));
        entity.setSyncedAt(now);
    }

    private List<JSONObject> extractFollowers(Map<String, Object> payload) {
        if (payload == null || payload.isEmpty()) {
            return List.of();
        }
        Object raw = firstPresent(payload, "followers", "list", "items", "user_list", "user_info_list");
        if (raw instanceof JSONArray array) {
            return array.toList(JSONObject.class);
        }
        if (raw instanceof List<?> list) {
            return list.stream()
                    .map(this::toJSONObject)
                    .filter(obj -> obj != null && StrUtil.isNotBlank(firstNonBlank(obj, "openid", "user_openid", "user_name")))
                    .toList();
        }
        if (JSONUtil.isTypeJSONArray(String.valueOf(raw))) {
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

    private Object firstPresent(Map<String, Object> payload, String... keys) {
        for (String key : keys) {
            Object value = payload.get(key);
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

    private LocalDateTime parseSubscribeTime(JSONObject follower) {
        Object raw = firstPresent(follower, "subscribe_time", "subscribed_at", "follow_time");
        if (raw == null) {
            return null;
        }
        if (raw instanceof Number number) {
            long epoch = number.longValue();
            if (epoch > 1_000_000_000_000L) {
                epoch = epoch / 1000;
            }
            return LocalDateTime.ofInstant(Instant.ofEpochSecond(epoch), ZoneId.systemDefault());
        }
        String text = String.valueOf(raw);
        if (StrUtil.isBlank(text)) {
            return null;
        }
        if (text.chars().allMatch(Character::isDigit)) {
            long epoch = Long.parseLong(text);
            if (epoch > 1_000_000_000_000L) {
                epoch = epoch / 1000;
            }
            return LocalDateTime.ofInstant(Instant.ofEpochSecond(epoch), ZoneId.systemDefault());
        }
        try {
            return LocalDateTime.parse(text.replace(' ', 'T'));
        } catch (Exception ignored) {
            return null;
        }
    }
}
