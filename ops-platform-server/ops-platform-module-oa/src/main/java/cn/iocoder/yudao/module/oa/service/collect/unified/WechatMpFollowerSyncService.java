package cn.iocoder.yudao.module.oa.service.collect.unified;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import cn.iocoder.yudao.framework.common.exception.OaErrorCodes;
import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.module.oa.dal.dataobject.account.AccountDO;
import cn.iocoder.yudao.module.oa.dal.dataobject.collect.CollectorAccountBindDO;
import cn.iocoder.yudao.module.oa.dal.dataobject.collect.WechatMpFollowerDO;
import cn.iocoder.yudao.module.oa.dal.mysql.account.AccountMapper;
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
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 微信公众号粉丝 follower-list 同步（M10-API-S-05）。
 */
@Service
@RequiredArgsConstructor
public class WechatMpFollowerSyncService {

    private static final String BIND_STATUS_BOUND = "BOUND";
    private static final String[] NICKNAME_KEYS = {"nickname", "nick_name", "user_name"};
    private static final String[] AVATAR_KEYS = {
            "headimgurl", "avatar", "head_img", "headImgUrl", "head_img_url",
            "user_headimg", "headimg", "headImg", "profile_photo", "icon_url"
    };
    private static final String[] NESTED_PROFILE_KEYS = {"user_info", "user_attr", "profile"};

    private final AccountMapper accountMapper;
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

        AccountDO account = accountMapper.selectById(oaAccountId);
        account = ConfigTenantSupport.getRequiredInTenant(account);
        if (WechatMpOfficialCredentialSupport.supportsOfficialApi(account)) {
            int synced = syncOfficialFollowers(tenantId, oaAccountId, bind.getCollectorAccountId());
            enrichFromCookieFollowerList(tenantId, oaAccountId, bind.getCollectorAccountId(), account);
            return synced;
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

    /** 已认证号：official/follower-list + follower-batch 全量同步 */
    private int syncOfficialFollowers(Long tenantId, Long oaAccountId, String collectorAccountId) {
        List<String> allOpenids = new ArrayList<>();
        int page = 1;
        while (page <= 100) {
            Map<String, Object> pageData = unifiedCollectorApiClient
                    .getWechatMpOfficialFollowerListPage(collectorAccountId, page);
            allOpenids.addAll(extractOpenids(pageData));
            Object next = pageData.get("next_openid");
            int count = toInt(pageData.get("count"));
            if (count <= 0 || next == null || StrUtil.isBlank(String.valueOf(next))) {
                break;
            }
            page++;
        }
        if (allOpenids.isEmpty()) {
            return 0;
        }

        LocalDateTime now = LocalDateTime.now();
        int synced = 0;
        for (int i = 0; i < allOpenids.size(); i += 100) {
            List<String> batch = allOpenids.subList(i, Math.min(i + 100, allOpenids.size()));
            Map<String, Object> batchData = unifiedCollectorApiClient
                    .getWechatMpOfficialFollowerBatch(collectorAccountId, batch);
            List<JSONObject> users = extractFollowers(batchData);
            for (JSONObject follower : users) {
                if (upsertFollower(tenantId, oaAccountId, follower, now)) {
                    synced++;
                }
            }
        }
        return synced;
    }

    /**
     * 官方 batchget 自 2021-12 起不再返回 nickname/headimgurl；若账号仍配置了 Cookie+Token，
     * 用 mp.weixin.qq.com 粉丝列表补全头像与昵称（best-effort，失败不影响主流程）。
     */
    private void enrichFromCookieFollowerList(Long tenantId, Long oaAccountId, String collectorAccountId,
                                              AccountDO account) {
        if (!hasCookieCredentials(account)) {
            return;
        }
        try {
            Map<String, Object> payload = unifiedCollectorApiClient.getWechatMpFollowerList(collectorAccountId);
            List<JSONObject> followers = extractFollowers(payload);
            if (followers.isEmpty()) {
                return;
            }
            LocalDateTime now = LocalDateTime.now();
            Map<String, JSONObject> profileByOpenid = indexProfilesByOpenid(followers);
            for (Map.Entry<String, JSONObject> entry : profileByOpenid.entrySet()) {
                mergeProfileIntoExisting(tenantId, oaAccountId, entry.getKey(), entry.getValue(), now);
            }
        } catch (RuntimeException ignored) {
            // cookie 未登录或 collector 不可用时跳过 enrichment
        }
    }

    private boolean hasCookieCredentials(AccountDO account) {
        return account != null
                && StrUtil.isNotBlank(account.getCookieEncrypted())
                && StrUtil.isNotBlank(account.getMpTokenEncrypted());
    }

    private Map<String, JSONObject> indexProfilesByOpenid(List<JSONObject> followers) {
        Map<String, JSONObject> profiles = new LinkedHashMap<>();
        for (JSONObject follower : followers) {
            String openid = firstNonBlank(follower, "openid", "user_openid", "user_name");
            if (StrUtil.isBlank(openid)) {
                continue;
            }
            if (StrUtil.isNotBlank(resolveProfileField(follower, NICKNAME_KEYS))
                    || StrUtil.isNotBlank(resolveProfileField(follower, AVATAR_KEYS))) {
                profiles.put(openid, follower);
            }
        }
        return profiles;
    }

    private void mergeProfileIntoExisting(Long tenantId, Long accountId, String openid, JSONObject profile,
                                          LocalDateTime now) {
        WechatMpFollowerDO existing = wechatMpFollowerMapper.selectOne(
                new LambdaQueryWrapper<WechatMpFollowerDO>()
                        .eq(WechatMpFollowerDO::getTenantId, tenantId)
                        .eq(WechatMpFollowerDO::getAccountId, accountId)
                        .eq(WechatMpFollowerDO::getOpenid, openid));
        if (existing == null) {
            upsertFollower(tenantId, accountId, profile, now);
            return;
        }
        applyFollowerFields(existing, profile, now);
        ConfigTenantSupport.fillUpdate(existing);
        wechatMpFollowerMapper.updateById(existing);
    }

    @SuppressWarnings("unchecked")
    private List<String> extractOpenids(Map<String, Object> payload) {
        if (payload == null || payload.isEmpty()) {
            return List.of();
        }
        Object raw = firstPresent(payload, "openids", "data");
        if (raw instanceof List<?> list) {
            return list.stream().map(String::valueOf).filter(StrUtil::isNotBlank).toList();
        }
        return List.of();
    }

    private int toInt(Object value) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        if (value == null) {
            return 0;
        }
        try {
            return Integer.parseInt(String.valueOf(value));
        } catch (NumberFormatException ex) {
            return 0;
        }
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
        String nickname = resolveProfileField(follower, NICKNAME_KEYS);
        if (StrUtil.isNotBlank(nickname)) {
            entity.setNickname(nickname);
        }
        String avatar = resolveProfileField(follower, AVATAR_KEYS);
        if (StrUtil.isNotBlank(avatar)) {
            entity.setAvatar(avatar);
        }
        String unionid = resolveProfileField(follower, "unionid");
        if (StrUtil.isNotBlank(unionid)) {
            entity.setUnionid(unionid);
        }
        LocalDateTime subscribedAt = parseSubscribeTime(follower);
        if (subscribedAt != null) {
            entity.setSubscribedAt(subscribedAt);
        }
        entity.setSyncedAt(now);
    }

    private String resolveProfileField(JSONObject follower, String... keys) {
        String direct = firstNonBlank(follower, keys);
        if (StrUtil.isNotBlank(direct)) {
            return direct;
        }
        for (String nestedKey : NESTED_PROFILE_KEYS) {
            JSONObject nested = follower.getJSONObject(nestedKey);
            if (nested == null) {
                continue;
            }
            String nestedValue = firstNonBlank(nested, keys);
            if (StrUtil.isNotBlank(nestedValue)) {
                return nestedValue;
            }
        }
        return null;
    }

    private List<JSONObject> extractFollowers(Map<String, Object> payload) {
        if (payload == null || payload.isEmpty()) {
            return List.of();
        }
        Object raw = firstPresent(payload, "followers", "list", "items", "user_list", "user_info_list", "users");
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
