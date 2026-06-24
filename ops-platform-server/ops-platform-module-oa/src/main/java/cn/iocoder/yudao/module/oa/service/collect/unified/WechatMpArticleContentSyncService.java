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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class WechatMpArticleContentSyncService {

    private static final String BIND_STATUS_BOUND = "BOUND";

    private final CollectorAccountBindMapper collectorAccountBindMapper;
    private final WechatMpArticleMapper wechatMpArticleMapper;
    private final WechatMpArticleSyncService wechatMpArticleSyncService;
    private final UnifiedCollectorApiClient unifiedCollectorApiClient;

    @Transactional
    public int syncArticleContent(Long oaAccountId) {
        Long tenantId = ConfigTenantSupport.requireTenantId();
        CollectorAccountBindDO bind = requireBoundCollector(oaAccountId, tenantId);

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
