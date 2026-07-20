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
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class WechatMpArticleStatsSyncService {

    private static final String BIND_STATUS_BOUND = "BOUND";
    /** 微信 getarticletotaldetail 数据起始日 */
    private static final LocalDate STATS_DATA_SINCE = LocalDate.of(2025, 11, 1);
    private static final int OFFICIAL_STATS_LOOKBACK_DAYS = 30;

    private final WechatOfficialAccountResolver wechatOfficialAccountResolver;
    private final CollectorAccountBindMapper collectorAccountBindMapper;
    private final WechatMpArticleMapper wechatMpArticleMapper;
    private final WechatMpArticleSyncService wechatMpArticleSyncService;
    private final UnifiedCollectorApiClient unifiedCollectorApiClient;

    @Transactional
    public int syncArticleStats(Long oaAccountId) {
        Long tenantId = ConfigTenantSupport.requireTenantId();
        CollectorAccountBindDO bind = requireBoundCollector(oaAccountId, tenantId);
        AccountDO account = wechatOfficialAccountResolver.requireTenantAccount(oaAccountId, tenantId);
        boolean officialApi = WechatMpOfficialCredentialSupport.supportsOfficialApi(account);

        List<WechatMpArticleDO> articles = loadArticles(tenantId, oaAccountId);
        if (articles.isEmpty()) {
            wechatMpArticleSyncService.syncArticles(oaAccountId);
            articles = loadArticles(tenantId, oaAccountId);
        } else {
            try {
                wechatMpArticleSyncService.syncArticles(oaAccountId);
                articles = loadArticles(tenantId, oaAccountId);
            } catch (Exception ex) {
                log.warn("刷新公众号图文列表失败，继续统计同步: {}", ex.getMessage());
            }
        }
        if (articles.isEmpty()) {
            throw new ServiceException(OaErrorCodes.BAD_REQUEST.getCode(), "暂无图文可同步，请先执行图文列表采集");
        }

        LocalDateTime now = LocalDateTime.now();
        int synced = 0;
        int attempted = 0;
        String lastError = null;
        OfficialStatsIndex officialIndex = officialApi
                ? loadOfficialArticleStatsIndex(bind.getCollectorAccountId())
                : OfficialStatsIndex.empty();
        for (WechatMpArticleDO article : articles) {
            String msgid = resolveMsgid(article.getArticleId());
            String publishDate = formatPublishDate(article.getPublishedAt());
            if (StrUtil.isBlank(publishDate)) {
                continue;
            }
            attempted++;
            boolean updated = false;
            if (officialApi) {
                JSONObject official = officialIndex.find(article, msgid);
                if (official != null && applyOfficialStats(article, official, now)) {
                    ConfigTenantSupport.fillUpdate(article);
                    wechatMpArticleMapper.updateById(article);
                    synced++;
                    updated = true;
                }
            }
            if (updated) {
                continue;
            }
            if (officialApi) {
                lastError = buildOfficialStatsEmptyMessage(officialIndex);
                continue;
            }
            if (StrUtil.isBlank(msgid)) {
                continue;
            }
            try {
                Map<String, Object> payload = unifiedCollectorApiClient.getWechatMpArticleData(
                        bind.getCollectorAccountId(), msgid, publishDate);
                JSONObject stats = JSONUtil.parseObj(JSONUtil.toJsonStr(payload));
                if (applyStats(article, stats, now)) {
                    ConfigTenantSupport.fillUpdate(article);
                    wechatMpArticleMapper.updateById(article);
                    synced++;
                }
            } catch (UnifiedCollectorApiException ex) {
                lastError = ex.getMessage();
                log.warn("公众号图文明细同步失败 articleId={} msgid={} publishDate={}: {}",
                        article.getArticleId(), msgid, publishDate, ex.getMessage());
            }
        }
        if (synced == 0 && attempted > 0) {
            String fallback = officialApi
                    ? "未能同步任何图文明细：官方 getarticletotaldetail 无匹配数据"
                    : "未能同步任何图文明细，请检查 Collector 登录态与 msgid/publish_date";
            throw new ServiceException(OaErrorCodes.BAD_REQUEST.getCode(), StrUtil.blankToDefault(lastError, fallback));
        }
        return synced;
    }

    private List<WechatMpArticleDO> loadArticles(Long tenantId, Long oaAccountId) {
        return wechatMpArticleMapper.selectList(
                new LambdaQueryWrapper<WechatMpArticleDO>()
                        .eq(WechatMpArticleDO::getTenantId, tenantId)
                        .eq(WechatMpArticleDO::getAccountId, oaAccountId)
                        .orderByDesc(WechatMpArticleDO::getPublishedAt));
    }

    /** 图文分析页要求 msgid 格式为 appmsgid_itemidx。 */
    public static String resolveMsgid(String articleId) {
        if (StrUtil.isBlank(articleId)) {
            return null;
        }
        if (articleId.contains("_")) {
            return articleId;
        }
        if (articleId.chars().allMatch(Character::isDigit)) {
            return articleId + "_1";
        }
        return articleId;
    }

    /** 从 freepublish article_id 解析 publish 级 ID（getarticle 入参）。 */
    public static String resolvePublishArticleId(String articleId) {
        if (StrUtil.isBlank(articleId)) {
            return null;
        }
        int sep = articleId.lastIndexOf('_');
        if (sep > 0 && isNewsItemIndexSuffix(articleId.substring(sep + 1))) {
            return articleId.substring(0, sep);
        }
        return articleId;
    }

    /** 多图文条目在 getarticle 返回 news_items 中的 0-based 下标。 */
    public static int resolveNewsItemIndex(String articleId) {
        if (StrUtil.isBlank(articleId)) {
            return 0;
        }
        int sep = articleId.lastIndexOf('_');
        if (sep > 0) {
            String suffix = articleId.substring(sep + 1);
            if (isNewsItemIndexSuffix(suffix)) {
                return Math.max(Integer.parseInt(suffix) - 1, 0);
            }
        }
        return 0;
    }

    /** 多图文 itemidx 后缀：1-20，避免把 article_id 本体中的 _001 等误识别为篇序。 */
    static boolean isNewsItemIndexSuffix(String suffix) {
        if (StrUtil.isBlank(suffix) || suffix.length() > 2) {
            return false;
        }
        if (!suffix.chars().allMatch(Character::isDigit)) {
            return false;
        }
        int value = Integer.parseInt(suffix);
        return value >= 1 && value <= 20;
    }

    private boolean applyOfficialStats(WechatMpArticleDO entity, JSONObject stats, LocalDateTime now) {
        Integer readCount = firstInt(stats, "read_user", "int_page_read_user", "int_page_read_count");
        if (readCount == null) {
            return false;
        }
        entity.setReadCount(readCount);
        entity.setLikeCount(firstInt(stats, "like_user", "zaikan_user", "like_count"));
        entity.setShareCount(firstInt(stats, "share_user", "share_count"));
        entity.setStatsSyncedAt(now);
        return true;
    }

    private OfficialStatsIndex loadOfficialArticleStatsIndex(String collectorAccountId) {
        Map<String, JSONObject> byMsgid = new HashMap<>();
        Map<String, JSONObject> byTitle = new HashMap<>();
        Map<String, JSONObject> byUrl = new HashMap<>();
        LocalDate end = LocalDate.now().minusDays(1);
        LocalDate start = end.minusDays(OFFICIAL_STATS_LOOKBACK_DAYS - 1L);
        if (start.isBefore(STATS_DATA_SINCE)) {
            start = STATS_DATA_SINCE;
        }
        boolean anyDayQueried = false;
        boolean anyDayHasData = false;
        for (LocalDate day = start; !day.isAfter(end); day = day.plusDays(1)) {
            String date = day.format(DateTimeFormatter.ISO_LOCAL_DATE);
            anyDayQueried = true;
            try {
                Map<String, Object> payload = unifiedCollectorApiClient.getWechatMpOfficialArticleTotalDetail(
                        collectorAccountId, date, date);
                List<JSONObject> rows = extractArticleTotalDetailRows(payload);
                if (!rows.isEmpty()) {
                    anyDayHasData = true;
                }
                for (JSONObject row : rows) {
                    JSONObject metrics = pickLatestDetailMetrics(row);
                    if (metrics == null) {
                        continue;
                    }
                    String msgid = row.getStr("msgid");
                    if (StrUtil.isNotBlank(msgid)) {
                        byMsgid.putIfAbsent(msgid.trim(), metrics);
                    }
                    String title = row.getStr("title");
                    if (StrUtil.isNotBlank(title)) {
                        byTitle.putIfAbsent(title.trim(), metrics);
                    }
                    String url = row.getStr("content_url");
                    if (StrUtil.isNotBlank(url)) {
                        byUrl.putIfAbsent(normalizeUrl(url), metrics);
                    }
                }
            } catch (UnifiedCollectorApiException ex) {
                log.warn("官方 article-total-detail 拉取失败 date={}: {}", date, ex.getMessage());
            }
        }
        return new OfficialStatsIndex(byMsgid, byTitle, byUrl, anyDayQueried, anyDayHasData);
    }

    private List<JSONObject> extractArticleTotalDetailRows(Map<String, Object> payload) {
        if (payload == null || payload.isEmpty()) {
            return List.of();
        }
        Object raw = payload.get("articles");
        if (!(raw instanceof List<?> list)) {
            return List.of();
        }
        return list.stream()
                .map(item -> JSONUtil.parseObj(JSONUtil.toJsonStr(item)))
                .toList();
    }

    private JSONObject pickLatestDetailMetrics(JSONObject row) {
        Object rawDetails = row.get("detail_list");
        if (!(rawDetails instanceof JSONArray details) || details.isEmpty()) {
            return null;
        }
        JSONObject latest = null;
        String latestDate = "";
        for (int i = 0; i < details.size(); i++) {
            JSONObject detail = details.getJSONObject(i);
            if (detail == null) {
                continue;
            }
            String statDate = detail.getStr("stat_date", "");
            if (latest == null || statDate.compareTo(latestDate) >= 0) {
                latest = detail;
                latestDate = statDate;
            }
        }
        return latest;
    }

    private String buildOfficialStatsEmptyMessage(OfficialStatsIndex index) {
        if (!index.anyDayQueried) {
            return "官方统计 API 暂不可查：发表数据仅自 2025-11-01 起有效，且 end_date 最大为昨日";
        }
        if (!index.anyDayHasData) {
            return "近 " + OFFICIAL_STATS_LOOKBACK_DAYS + " 天 getarticletotaldetail 均无发表记录（"
                    + STATS_DATA_SINCE + " 起有效；当日无发表则返回空）";
        }
        return "getarticletotaldetail 有数据但未匹配到本地图文（按 msgid/标题/URL 匹配）";
    }

    private String normalizeUrl(String url) {
        return url == null ? "" : url.trim().replaceAll("/+$", "");
    }

    private boolean applyStats(WechatMpArticleDO entity, JSONObject stats, LocalDateTime now) {
        Integer readCount = firstInt(stats, "read_count", "read_num", "int_page_read_count");
        if (readCount == null) {
            return false;
        }
        entity.setReadCount(readCount);
        entity.setLikeCount(firstInt(stats, "like_count", "like_num", "old_like_count"));
        entity.setShareCount(firstInt(stats, "share_count", "share_num", "share_page"));
        entity.setStatsSyncedAt(now);
        return true;
    }

    private String formatPublishDate(LocalDateTime publishedAt) {
        if (publishedAt == null) {
            return null;
        }
        return publishedAt.format(DateTimeFormatter.ISO_LOCAL_DATE);
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

    private record OfficialStatsIndex(
            Map<String, JSONObject> byMsgid,
            Map<String, JSONObject> byTitle,
            Map<String, JSONObject> byUrl,
            boolean anyDayQueried,
            boolean anyDayHasData) {

        static OfficialStatsIndex empty() {
            return new OfficialStatsIndex(Map.of(), Map.of(), Map.of(), false, false);
        }

        JSONObject find(WechatMpArticleDO article, String msgid) {
            if (StrUtil.isNotBlank(msgid)) {
                JSONObject hit = byMsgid.get(msgid.trim());
                if (hit != null) {
                    return hit;
                }
            }
            if (StrUtil.isNotBlank(article.getTitle())) {
                JSONObject hit = byTitle.get(article.getTitle().trim());
                if (hit != null) {
                    return hit;
                }
            }
            if (StrUtil.isNotBlank(article.getUrl())) {
                JSONObject hit = byUrl.get(normalizeUrlStatic(article.getUrl()));
                if (hit != null) {
                    return hit;
                }
            }
            return null;
        }

        private static String normalizeUrlStatic(String url) {
            return url == null ? "" : url.trim().replaceAll("/+$", "");
        }
    }
}
