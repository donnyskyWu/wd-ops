package cn.iocoder.yudao.module.oa.service.collect.unified;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import cn.iocoder.yudao.framework.common.exception.OaErrorCodes;
import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.module.oa.dal.dataobject.collect.CollectorAccountBindDO;
import cn.iocoder.yudao.module.oa.dal.dataobject.collect.XiaohongshuNoteDO;
import cn.iocoder.yudao.module.oa.dal.mysql.collect.CollectorAccountBindMapper;
import cn.iocoder.yudao.module.oa.dal.mysql.collect.XiaohongshuNoteMapper;
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

@Service
@RequiredArgsConstructor
public class XiaohongshuNoteSyncService {

    private static final String BIND_STATUS_BOUND = "BOUND";
    private static final int PAGE_SIZE = 50;

    private final CollectorAccountBindMapper collectorAccountBindMapper;
    private final XiaohongshuNoteMapper xiaohongshuNoteMapper;
    private final UnifiedCollectorApiClient unifiedCollectorApiClient;

    @Transactional
    public int syncNotes(Long oaAccountId) {
        Long tenantId = ConfigTenantSupport.requireTenantId();
        CollectorAccountBindDO bind = requireBoundCollector(oaAccountId, tenantId);

        LocalDateTime now = LocalDateTime.now();
        int synced = 0;
        String cursor = "";
        String listError = null;
        for (int page = 0; page < 100; page++) {
            Map<String, Object> payload;
            try {
                payload = unifiedCollectorApiClient.getXiaohongshuNoteList(
                        bind.getCollectorAccountId(), cursor, PAGE_SIZE);
            } catch (UnifiedCollectorApiException ex) {
                listError = ex.getMessage();
                break;
            }
            List<JSONObject> notes = extractNotes(payload);
            if (notes.isEmpty()) {
                break;
            }
            for (JSONObject note : notes) {
                if (upsertNote(tenantId, oaAccountId, note, now)) {
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
        if (synced == 0 && StrUtil.isNotBlank(listError)) {
            throw new ServiceException(OaErrorCodes.BAD_REQUEST.getCode(), listError);
        }
        return synced;
    }

    private boolean upsertNote(Long tenantId, Long accountId, JSONObject note, LocalDateTime now) {
        String noteId = firstNonBlank(note, "note_id", "id");
        if (StrUtil.isBlank(noteId)) {
            return false;
        }
        XiaohongshuNoteDO existing = xiaohongshuNoteMapper.selectOne(
                new LambdaQueryWrapper<XiaohongshuNoteDO>()
                        .eq(XiaohongshuNoteDO::getTenantId, tenantId)
                        .eq(XiaohongshuNoteDO::getAccountId, accountId)
                        .eq(XiaohongshuNoteDO::getNoteId, noteId));
        if (existing == null) {
            XiaohongshuNoteDO entity = new XiaohongshuNoteDO();
            entity.setAccountId(accountId);
            entity.setNoteId(noteId);
            applyNoteFields(entity, note, now);
            ConfigTenantSupport.fillCreate(entity);
            xiaohongshuNoteMapper.insert(entity);
            return true;
        }
        applyNoteFields(existing, note, now);
        ConfigTenantSupport.fillUpdate(existing);
        xiaohongshuNoteMapper.updateById(existing);
        return true;
    }

    private void applyNoteFields(XiaohongshuNoteDO entity, JSONObject note, LocalDateTime now) {
        entity.setXsecToken(firstNonBlank(note, "xsec_token", "xsecToken"));
        entity.setTitle(firstNonBlank(note, "title", "display_title", "name"));
        entity.setDescription(firstNonBlank(note, "desc", "description"));
        entity.setNoteUrl(firstNonBlank(note, "note_url", "url", "link"));
        entity.setCoverUrl(firstNonBlank(note, "cover_url", "cover", "image"));
        entity.setPublishedAt(parsePublishTime(note));
        entity.setLikeCount(firstInt(note, "liked_count", "like_count", "likeCount"));
        entity.setCollectCount(firstInt(note, "collected_count", "collect_count", "collectCount"));
        entity.setCommentCount(firstInt(note, "comment_count", "commentCount"));
        entity.setSyncedAt(now);
    }

    private List<JSONObject> extractNotes(Map<String, Object> payload) {
        if (payload == null || payload.isEmpty()) {
            return List.of();
        }
        Object raw = firstPresent(payload, "notes", "items", "list", "videos");
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
        Object raw = firstPresent(payload, "cursor");
        return raw == null ? null : String.valueOf(raw);
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

    private LocalDateTime parsePublishTime(JSONObject note) {
        Object raw = firstPresent(note, "publish_time", "published_at", "time", "create_time");
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
