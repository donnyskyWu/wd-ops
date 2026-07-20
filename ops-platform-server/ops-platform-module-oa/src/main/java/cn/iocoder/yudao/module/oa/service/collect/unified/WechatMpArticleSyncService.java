package cn.iocoder.yudao.module.oa.service.collect.unified;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import cn.iocoder.yudao.framework.common.exception.OaErrorCodes;
import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.module.oa.dal.dataobject.account.AccountDO;
import cn.iocoder.yudao.module.oa.dal.dataobject.collect.CollectorAccountBindDO;
import cn.iocoder.yudao.module.oa.dal.dataobject.collect.WechatMpArticleDO;
import cn.iocoder.yudao.module.oa.dal.mysql.collect.CollectorAccountBindMapper;
import cn.iocoder.yudao.module.oa.dal.mysql.collect.WechatMpArticleMapper;
import cn.iocoder.yudao.module.oa.service.account.WechatOfficialAccountResolver;
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
 * 微信公众号图文 article-list 同步（M10 P2 · Channel-A MVP）。
 */
@Service
@RequiredArgsConstructor
public class WechatMpArticleSyncService {

    private static final String BIND_STATUS_BOUND = "BOUND";

    private final WechatOfficialAccountResolver wechatOfficialAccountResolver;
    private final CollectorAccountBindMapper collectorAccountBindMapper;
    private final WechatMpArticleMapper wechatMpArticleMapper;
    private final UnifiedCollectorApiClient unifiedCollectorApiClient;

    @Transactional
    public int syncArticles(Long oaAccountId) {
        Long tenantId = ConfigTenantSupport.requireTenantId();
        AccountDO account = wechatOfficialAccountResolver.requireTenantAccount(oaAccountId, tenantId);

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

        if (WechatMpOfficialCredentialSupport.supportsOfficialApi(account)) {
            return syncOfficialArticles(tenantId, oaAccountId, bind.getCollectorAccountId(), account);
        }

        Map<String, Object> payload = unifiedCollectorApiClient.getWechatMpPublishList(bind.getCollectorAccountId());
        List<JSONObject> articles = extractArticles(payload);
        if (articles.isEmpty() && StrUtil.isNotBlank(account.getExternalAccountId())) {
            payload = unifiedCollectorApiClient.getWechatMpArticleList(
                    bind.getCollectorAccountId(), account.getExternalAccountId().trim());
            articles = extractArticles(payload);
        }
        LocalDateTime now = LocalDateTime.now();
        int synced = 0;
        for (JSONObject article : articles) {
            if (upsertArticle(tenantId, oaAccountId, article, now)) {
                synced++;
            }
        }
        return synced;
    }

    /** 已认证号：official/freepublish-list 同步已发布图文 */
    private int syncOfficialArticles(Long tenantId, Long oaAccountId, String collectorAccountId, AccountDO account) {
        List<JSONObject> articles = new ArrayList<>();
        int offset = 0;
        int pageSize = 20;
        int totalCount = Integer.MAX_VALUE;
        while (offset < totalCount && offset < 500) {
            Map<String, Object> payload = unifiedCollectorApiClient.getWechatMpOfficialFreepublishList(
                    collectorAccountId, offset, pageSize);
            List<JSONObject> pageArticles = extractFreepublishArticles(payload);
            if (pageArticles.isEmpty()) {
                break;
            }
            articles.addAll(pageArticles);
            totalCount = toInt(firstPresent(payload, "total_count"));
            int itemCount = toInt(firstPresent(payload, "item_count"));
            if (itemCount < pageSize) {
                break;
            }
            offset += pageSize;
        }
        if (articles.isEmpty()) {
            throw new ServiceException(OaErrorCodes.BAD_REQUEST.getCode(),
                    "官方 API 暂无已发布图文（freepublish/batchget 返回空）。"
                            + "请确认账号已通过 freepublish 正式发布内容；"
                            + "注意 batchget 可能不含开启群发通知的发表记录；"
                            + "统计类数据仅自 2025-11-01 起有效");
        }
        LocalDateTime now = LocalDateTime.now();
        int synced = 0;
        for (JSONObject article : articles) {
            if (upsertArticle(tenantId, oaAccountId, article, now)) {
                synced++;
            }
        }
        return synced;
    }

    @SuppressWarnings("unchecked")
    private List<JSONObject> extractFreepublishArticles(Map<String, Object> payload) {
        if (payload == null || payload.isEmpty()) {
            return List.of();
        }
        Object rawItems = firstPresent(payload, "items", "item");
        if (!(rawItems instanceof List<?> items)) {
            return List.of();
        }
        List<JSONObject> articles = new ArrayList<>();
        for (Object itemObj : items) {
            JSONObject item = toJSONObject(itemObj);
            if (item == null) {
                continue;
            }
            String publishArticleId = item.getStr("article_id");
            long updateTime = item.getLong("update_time", 0L);
            List<?> newsItems = resolveNewsItems(item);
            if (newsItems.isEmpty()) {
                continue;
            }
            int idx = 0;
            for (Object newsObj : newsItems) {
                JSONObject news = toJSONObject(newsObj);
                if (news == null) {
                    continue;
                }
                idx++;
                JSONObject article = new JSONObject(news);
                if (StrUtil.isNotBlank(publishArticleId)) {
                    article.set("article_id", newsItems.size() > 1
                            ? publishArticleId + "_" + idx
                            : publishArticleId);
                    article.set("publish_article_id", publishArticleId);
                }
                if (updateTime > 0) {
                    article.set("publish_time", updateTime);
                }
                String digest = news.getStr("digest");
                if (StrUtil.isNotBlank(digest)) {
                    article.set("content_text", digest);
                }
                articles.add(article);
            }
        }
        return articles;
    }

    private List<?> resolveNewsItems(JSONObject item) {
        Object newsRaw = item.get("news_items");
        if (newsRaw instanceof List<?> list && !list.isEmpty()) {
            return list;
        }
        Object contentObj = item.get("content");
        if (contentObj instanceof Map<?, ?> contentMap) {
            Object nested = contentMap.get("news_item");
            if (nested instanceof List<?> list && !list.isEmpty()) {
                return list;
            }
        }
        return List.of();
    }

    @SuppressWarnings("unchecked")
    private List<JSONObject> extractOfficialMaterialArticles(Map<String, Object> payload) {
        if (payload == null || payload.isEmpty()) {
            return List.of();
        }
        Object rawItems = firstPresent(payload, "items");
        if (!(rawItems instanceof List<?> items)) {
            return List.of();
        }
        List<JSONObject> articles = new ArrayList<>();
        for (Object itemObj : items) {
            JSONObject item = toJSONObject(itemObj);
            if (item == null) {
                continue;
            }
            Object newsRaw = item.get("news_items");
            if (!(newsRaw instanceof List<?> newsItems)) {
                continue;
            }
            long updateTime = item.getLong("update_time", 0L);
            String mediaId = item.getStr("media_id");
            int idx = 0;
            for (Object newsObj : newsItems) {
                JSONObject news = toJSONObject(newsObj);
                if (news == null) {
                    continue;
                }
                idx++;
                JSONObject article = new JSONObject(news);
                if (StrUtil.isNotBlank(mediaId)) {
                    article.set("article_id", mediaId + "_" + idx);
                }
                if (updateTime > 0) {
                    article.set("publish_time", updateTime);
                }
                String digest = news.getStr("digest");
                if (StrUtil.isNotBlank(digest)) {
                    article.set("content_text", digest);
                }
                articles.add(article);
            }
        }
        return articles;
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

    private boolean upsertArticle(Long tenantId, Long accountId, JSONObject article, LocalDateTime now) {
        String articleId = resolveArticleId(article);
        if (StrUtil.isBlank(articleId)) {
            return false;
        }
        WechatMpArticleDO existing = wechatMpArticleMapper.selectOne(
                new LambdaQueryWrapper<WechatMpArticleDO>()
                        .eq(WechatMpArticleDO::getTenantId, tenantId)
                        .eq(WechatMpArticleDO::getAccountId, accountId)
                        .eq(WechatMpArticleDO::getArticleId, articleId));
        if (existing == null) {
            WechatMpArticleDO entity = new WechatMpArticleDO();
            entity.setAccountId(accountId);
            entity.setArticleId(articleId);
            applyArticleFields(entity, article, now);
            ConfigTenantSupport.fillCreate(entity);
            wechatMpArticleMapper.insert(entity);
            return true;
        }
        applyArticleFields(existing, article, now);
        ConfigTenantSupport.fillUpdate(existing);
        wechatMpArticleMapper.updateById(existing);
        return true;
    }

    private void applyArticleFields(WechatMpArticleDO entity, JSONObject article, LocalDateTime now) {
        entity.setTitle(firstNonBlank(article, "title", "name"));
        entity.setUrl(firstNonBlank(article, "url", "link", "content_url", "article_url"));
        entity.setCoverUrl(firstNonBlank(article, "cover", "cover_url", "thumb_url", "thumb"));
        entity.setPublishedAt(parsePublishTime(article));
        entity.setReadCount(firstInt(article, "read_count", "read_num", "int_page_read_count"));
        entity.setLikeCount(firstInt(article, "like_count", "like_num", "old_like_count"));
        entity.setShareCount(firstInt(article, "share_count", "share_num", "share_page"));
        entity.setSyncedAt(now);
        String contentText = firstNonBlank(article, "content_text");
        if (StrUtil.isNotBlank(contentText)) {
            entity.setContentText(contentText);
            entity.setContentSyncedAt(now);
        }
    }

    private List<JSONObject> extractArticles(Map<String, Object> payload) {
        if (payload == null || payload.isEmpty()) {
            return List.of();
        }
        Object raw = firstPresent(payload, "articles", "list", "items", "publish_list", "app_msg_list");
        if (raw instanceof JSONArray array) {
            return array.toList(JSONObject.class);
        }
        if (raw instanceof List<?> list) {
            return list.stream()
                    .map(this::toJSONObject)
                    .filter(obj -> obj != null && StrUtil.isNotBlank(firstNonBlank(obj, "article_id", "aid", "msgid", "appmsgid", "id")))
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

    private Object firstPresent(JSONObject obj, String... keys) {
        for (String key : keys) {
            Object value = obj.get(key);
            if (value != null) {
                return value;
            }
        }
        return null;
    }

    private String resolveArticleId(JSONObject article) {
        String articleId = firstNonBlank(article, "aid", "article_id", "msgid", "appmsgid", "id");
        if (StrUtil.isBlank(articleId)) {
            return null;
        }
        if (articleId.contains("_")) {
            return articleId;
        }
        if (articleId.chars().allMatch(Character::isDigit)) {
            Integer itemidx = firstInt(article, "itemidx");
            return articleId + "_" + (itemidx != null ? itemidx : 1);
        }
        return articleId;
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
        if (StrUtil.isBlank(text) || !text.chars().allMatch(Character::isDigit)) {
            return null;
        }
        return Integer.parseInt(text);
    }

    private LocalDateTime parsePublishTime(JSONObject article) {
        Object raw = firstPresent(article, "publish_time", "published_at", "create_time", "update_time");
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
