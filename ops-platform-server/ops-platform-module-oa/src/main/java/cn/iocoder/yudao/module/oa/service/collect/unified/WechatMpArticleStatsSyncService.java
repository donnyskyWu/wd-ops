package cn.iocoder.yudao.module.oa.service.collect.unified;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import cn.iocoder.yudao.framework.common.exception.OaErrorCodes;
import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.module.oa.dal.dataobject.collect.CollectorAccountBindDO;
import cn.iocoder.yudao.module.oa.dal.dataobject.collect.WechatMpArticleDO;
import cn.iocoder.yudao.module.oa.dal.mysql.collect.CollectorAccountBindMapper;
import cn.iocoder.yudao.module.oa.dal.mysql.collect.WechatMpArticleMapper;
import cn.iocoder.yudao.module.oa.service.config.ConfigTenantSupport;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class WechatMpArticleStatsSyncService {

    private static final String BIND_STATUS_BOUND = "BOUND";

    private final CollectorAccountBindMapper collectorAccountBindMapper;
    private final WechatMpArticleMapper wechatMpArticleMapper;
    private final WechatMpArticleSyncService wechatMpArticleSyncService;
    private final UnifiedCollectorApiClient unifiedCollectorApiClient;

    @Transactional
    public int syncArticleStats(Long oaAccountId) {
        Long tenantId = ConfigTenantSupport.requireTenantId();
        CollectorAccountBindDO bind = requireBoundCollector(oaAccountId, tenantId);

        List<WechatMpArticleDO> articles = loadArticles(tenantId, oaAccountId);
        if (articles.isEmpty()) {
            wechatMpArticleSyncService.syncArticles(oaAccountId);
            articles = loadArticles(tenantId, oaAccountId);
        } else {
            // 刷新 publish-list，修正 article_id 为 appmsgid_itemidx 并带回列表侧已有指标
            try {
                wechatMpArticleSyncService.syncArticles(oaAccountId);
                articles = loadArticles(tenantId, oaAccountId);
            } catch (Exception ex) {
                log.warn("刷新公众号图文列表失败，继续 article-data 明细同步: {}", ex.getMessage());
            }
        }
        if (articles.isEmpty()) {
            throw new ServiceException(OaErrorCodes.BAD_REQUEST.getCode(), "暂无图文可同步，请先执行图文列表采集");
        }

        LocalDateTime now = LocalDateTime.now();
        int synced = 0;
        int attempted = 0;
        String lastError = null;
        for (WechatMpArticleDO article : articles) {
            String msgid = resolveMsgid(article.getArticleId());
            String publishDate = formatPublishDate(article.getPublishedAt());
            if (StrUtil.isBlank(msgid) || StrUtil.isBlank(publishDate)) {
                continue;
            }
            attempted++;
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
            throw new ServiceException(OaErrorCodes.BAD_REQUEST.getCode(), StrUtil.blankToDefault(lastError,
                    "未能同步任何图文明细，请检查 Collector 登录态与 msgid/publish_date"));
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
}
