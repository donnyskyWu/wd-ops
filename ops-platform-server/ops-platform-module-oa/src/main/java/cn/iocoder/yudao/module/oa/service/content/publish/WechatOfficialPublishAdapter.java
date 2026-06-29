package cn.iocoder.yudao.module.oa.service.content.publish;

import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HtmlUtil;
import cn.iocoder.yudao.framework.common.exception.OaErrorCodes;
import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.module.oa.dal.dataobject.account.AccountDO;
import cn.iocoder.yudao.module.oa.dal.dataobject.content.ProductionContentDO;
import cn.iocoder.yudao.module.oa.service.collect.unified.WechatMpOfficialCredentialSupport;
import cn.iocoder.yudao.module.oa.util.AesUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 微信公众号发布适配器：调用 Open API {@code draft/add} 写入草稿箱（P2-M2-PUB-02 · ADR-047）。
 */
@Slf4j
@Component
@Order(0)
@RequiredArgsConstructor
public class WechatOfficialPublishAdapter implements PlatformPublishAdapter {

    private static final int DIGEST_MAX_LEN = 120;

    private final WechatOfficialApiClient apiClient;
    private final AesUtil aesUtil;

    @Override
    public boolean supports(String platformType) {
        return "WECHAT_OFFICIAL".equals(platformType);
    }

    @Override
    public PlatformPublishResult publish(ProductionContentDO content, AccountDO account) {
        if (!WechatMpOfficialCredentialSupport.supportsOfficialApi(account)) {
            return PlatformPublishResult.failure(
                    "公众号未通过微信认证或未配置 AppID/AppSecret，无法推送到草稿箱");
        }
        try {
            String appSecret = decryptAppSecret(account.getAppSecretEncrypted());
            String accessToken = apiClient.getAccessToken(account.getAppId().trim(), appSecret);
            byte[] thumbBytes = apiClient.resolveThumbBytes(content.getCoverImage());
            String thumbMediaId = apiClient.uploadThumbMedia(accessToken, thumbBytes, "cover.jpg");
            Map<String, Object> article = buildDraftArticle(content, thumbMediaId);
            String draftMediaId = apiClient.addDraft(accessToken, article);
            log.info("[WechatOfficial] draft/add success contentId={} accountId={} mediaId={}",
                    content.getId(), account.getId(), draftMediaId);
            return PlatformPublishResult.success(draftMediaId, false);
        } catch (WechatOfficialApiException ex) {
            log.warn("[WechatOfficial] publish failed contentId={} accountId={}: {}",
                    content.getId(), account.getId(), ex.getMessage());
            return PlatformPublishResult.failure(ex.getMessage());
        } catch (ServiceException ex) {
            return PlatformPublishResult.failure(ex.getMessage());
        } catch (Exception ex) {
            log.error("[WechatOfficial] publish error contentId={} accountId={}",
                    content.getId(), account.getId(), ex);
            return PlatformPublishResult.failure("微信公众号发布异常: " + ex.getMessage());
        }
    }

    @Override
    public boolean supportsFormalPublish() {
        return true;
    }

    @Override
    public PlatformPublishResult formalPublish(ProductionContentDO content, AccountDO account, String draftMediaId) {
        if (!WechatMpOfficialCredentialSupport.supportsOfficialApi(account)) {
            return PlatformPublishResult.failure(
                    "公众号未通过微信认证或未配置 AppID/AppSecret，无法正式发布");
        }
        if (StrUtil.isBlank(draftMediaId)) {
            return PlatformPublishResult.failure("缺少草稿 media_id，请先发布为草稿");
        }
        try {
            String appSecret = decryptAppSecret(account.getAppSecretEncrypted());
            String accessToken = apiClient.getAccessToken(account.getAppId().trim(), appSecret);
            String publishId = apiClient.freepublishSubmit(accessToken, draftMediaId.trim());
            log.info("[WechatOfficial] freepublish/submit success contentId={} accountId={} publishId={}",
                    content.getId(), account.getId(), publishId);
            return PlatformPublishResult.formalSuccess(publishId, false);
        } catch (WechatOfficialApiException ex) {
            log.warn("[WechatOfficial] formal publish failed contentId={} accountId={}: {}",
                    content.getId(), account.getId(), ex.getMessage());
            return PlatformPublishResult.failure(ex.getMessage());
        } catch (ServiceException ex) {
            return PlatformPublishResult.failure(ex.getMessage());
        } catch (Exception ex) {
            log.error("[WechatOfficial] formal publish error contentId={} accountId={}",
                    content.getId(), account.getId(), ex);
            return PlatformPublishResult.failure("微信公众号正式发布异常: " + ex.getMessage());
        }
    }

    private Map<String, Object> buildDraftArticle(ProductionContentDO content, String thumbMediaId) {
        Map<String, Object> article = new LinkedHashMap<>();
        article.put("article_type", "news");
        article.put("title", StrUtil.blankToDefault(content.getTitle(), "无标题"));
        article.put("author", "");
        article.put("digest", buildDigest(content));
        article.put("content", buildContentHtml(content));
        article.put("content_source_url", "");
        article.put("thumb_media_id", thumbMediaId);
        article.put("need_open_comment", 0);
        article.put("only_fans_can_comment", 0);
        return article;
    }

    private String buildContentHtml(ProductionContentDO content) {
        if (StrUtil.isNotBlank(content.getLayoutHtml())) {
            return content.getLayoutHtml();
        }
        if (StrUtil.isNotBlank(content.getBody())) {
            String escaped = HtmlUtil.escape(content.getBody());
            return "<p>" + escaped.replace("\n\n", "</p><p>").replace("\n", "<br/>") + "</p>";
        }
        return "<p></p>";
    }

    private String buildDigest(ProductionContentDO content) {
        String plain = StrUtil.blankToDefault(content.getBody(), "");
        if (StrUtil.isBlank(plain) && StrUtil.isNotBlank(content.getLayoutHtml())) {
            plain = HtmlUtil.cleanHtmlTag(content.getLayoutHtml());
        }
        plain = plain.replaceAll("\\s+", " ").trim();
        return StrUtil.sub(plain, 0, DIGEST_MAX_LEN);
    }

    private String decryptAppSecret(String encrypted) {
        if (StrUtil.isBlank(encrypted)) {
            throw new ServiceException(OaErrorCodes.ENTITY_NOT_EXISTS.getCode(), "AppSecret 未配置");
        }
        try {
            return aesUtil.decrypt(encrypted);
        } catch (Exception ex) {
            throw new ServiceException(OaErrorCodes.ENTITY_NOT_EXISTS.getCode(), "AppSecret 解密失败");
        }
    }
}
