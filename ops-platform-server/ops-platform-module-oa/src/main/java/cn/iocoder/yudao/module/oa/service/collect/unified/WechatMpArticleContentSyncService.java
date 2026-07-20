package cn.iocoder.yudao.module.oa.service.collect.unified;

import cn.hutool.core.util.StrUtil;
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

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class WechatMpArticleContentSyncService {

    private static final String BIND_STATUS_BOUND = "BOUND";

    private final WechatOfficialAccountResolver wechatOfficialAccountResolver;
    private final CollectorAccountBindMapper collectorAccountBindMapper;
    private final WechatMpArticleMapper wechatMpArticleMapper;
    private final WechatMpArticleSyncService wechatMpArticleSyncService;
    private final UnifiedCollectorApiClient unifiedCollectorApiClient;

    @Transactional
    public int syncArticleContent(Long oaAccountId) {
        Long tenantId = ConfigTenantSupport.requireTenantId();
        CollectorAccountBindDO bind = requireBoundCollector(oaAccountId, tenantId);
        AccountDO account = wechatOfficialAccountResolver.requireTenantAccount(oaAccountId, tenantId);
        boolean officialOnly = WechatMpOfficialCredentialSupport.supportsOfficialApi(account);

        List<WechatMpArticleDO> articles = wechatMpArticleMapper.selectList(
                new LambdaQueryWrapper<WechatMpArticleDO>()
                        .eq(WechatMpArticleDO::getTenantId, tenantId)
                        .eq(WechatMpArticleDO::getAccountId, oaAccountId)
                        .orderByDesc(WechatMpArticleDO::getPublishedAt));
        if (articles.isEmpty()) {
            wechatMpArticleSyncService.syncArticles(oaAccountId);
            articles = wechatMpArticleMapper.selectList(
                    new LambdaQueryWrapper<WechatMpArticleDO>()
                            .eq(WechatMpArticleDO::getTenantId, tenantId)
                            .eq(WechatMpArticleDO::getAccountId, oaAccountId));
        }
        if (articles.isEmpty()) {
            throw new ServiceException(OaErrorCodes.BAD_REQUEST.getCode(), "暂无图文可同步，请先执行图文列表采集");
        }

        LocalDateTime now = LocalDateTime.now();
        int synced = 0;
        for (WechatMpArticleDO article : articles) {
            if (officialOnly) {
                if (syncOfficialContent(bind.getCollectorAccountId(), article, now)) {
                    ConfigTenantSupport.fillUpdate(article);
                    wechatMpArticleMapper.updateById(article);
                    synced++;
                }
                continue;
            }
            if (StrUtil.isBlank(article.getUrl())) {
                continue;
            }
            Map<String, Object> payload = unifiedCollectorApiClient.getWechatMpArticleDownload(
                    bind.getCollectorAccountId(), article.getUrl());
            JSONObject content = JSONUtil.parseObj(JSONUtil.toJsonStr(payload));
            if (applyContent(article, content, now)) {
                ConfigTenantSupport.fillUpdate(article);
                wechatMpArticleMapper.updateById(article);
                synced++;
            }
        }
        return synced;
    }

    private boolean syncOfficialContent(String collectorAccountId, WechatMpArticleDO article, LocalDateTime now) {
        String publishArticleId = WechatMpArticleStatsSyncService.resolvePublishArticleId(article.getArticleId());
        if (StrUtil.isBlank(publishArticleId)) {
            return false;
        }
        try {
            Map<String, Object> payload = unifiedCollectorApiClient.getWechatMpOfficialFreepublishArticle(
                    collectorAccountId, publishArticleId);
            JSONObject content = extractOfficialNewsItem(payload, article.getArticleId());
            if (content == null) {
                log.warn("freepublish/getarticle 无正文 articleId={} publishArticleId={}",
                        article.getArticleId(), publishArticleId);
                return false;
            }
            return applyOfficialContent(article, content, now);
        } catch (UnifiedCollectorApiException ex) {
            log.warn("freepublish/getarticle 失败 articleId={}: {}", article.getArticleId(), ex.getMessage());
            return false;
        }
    }

    private JSONObject extractOfficialNewsItem(Map<String, Object> payload, String articleId) {
        if (payload == null || payload.isEmpty()) {
            return null;
        }
        Object raw = payload.get("news_items");
        if (!(raw instanceof List<?> list) || list.isEmpty()) {
            return null;
        }
        int index = WechatMpArticleStatsSyncService.resolveNewsItemIndex(articleId);
        if (index >= list.size()) {
            index = 0;
        }
        return JSONUtil.parseObj(JSONUtil.toJsonStr(list.get(index)));
    }

    private boolean applyOfficialContent(WechatMpArticleDO entity, JSONObject content, LocalDateTime now) {
        String text = firstNonBlank(content, "content", "content_text", "digest");
        if (StrUtil.isBlank(text)) {
            return false;
        }
        entity.setContentText(text);
        String title = firstNonBlank(content, "title");
        if (StrUtil.isNotBlank(title)) {
            entity.setTitle(title);
        }
        String url = firstNonBlank(content, "url");
        if (StrUtil.isNotBlank(url)) {
            entity.setUrl(url);
        }
        String thumb = firstNonBlank(content, "thumb_url", "cover_url");
        if (StrUtil.isNotBlank(thumb)) {
            entity.setCoverUrl(thumb);
        }
        entity.setContentSyncedAt(now);
        return true;
    }

    private boolean applyContent(WechatMpArticleDO entity, JSONObject content, LocalDateTime now) {
        String text = firstNonBlank(content, "content_text", "text", "body");
        if (StrUtil.isBlank(text)) {
            return false;
        }
        entity.setContentText(text);
        String title = firstNonBlank(content, "title");
        if (StrUtil.isNotBlank(title)) {
            entity.setTitle(title);
        }
        entity.setContentSyncedAt(now);
        return true;
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
}
