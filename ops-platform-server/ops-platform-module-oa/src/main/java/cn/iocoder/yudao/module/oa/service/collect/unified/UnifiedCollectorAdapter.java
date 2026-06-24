package cn.iocoder.yudao.module.oa.service.collect.unified;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.exception.OaErrorCodes;
import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.module.oa.api.dto.collect.CollectorAccountBindRespVO;
import cn.iocoder.yudao.module.oa.api.dto.collect.CollectorAccountBindSaveReq;
import cn.iocoder.yudao.module.oa.api.dto.collect.CollectorAccountBindTestConnectionRespVO;
import cn.iocoder.yudao.module.oa.dal.dataobject.account.AccountDO;
import cn.iocoder.yudao.module.oa.dal.dataobject.collect.CollectorAccountBindDO;
import cn.iocoder.yudao.module.oa.dal.mysql.account.AccountMapper;
import cn.iocoder.yudao.module.oa.dal.mysql.collect.CollectorAccountBindMapper;
import cn.iocoder.yudao.module.oa.framework.audit.AuditLog;
import cn.iocoder.yudao.module.oa.service.collect.CollectorAccountBindService;
import cn.iocoder.yudao.module.oa.service.config.ConfigTenantSupport;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Channel-A · unify-collector-api Adapter（ADR-047 · M10-API-S-02）。
 */
@Component
@RequiredArgsConstructor
public class UnifiedCollectorAdapter {

    private static final String BIND_STATUS_BOUND = "BOUND";
    private static final String BIND_STATUS_FAILED = "FAILED";

    private final AccountMapper accountMapper;
    private final CollectorAccountBindMapper collectorAccountBindMapper;
    private final CollectorAccountBindService collectorAccountBindService;
    private final CollectorCredentialBuilder credentialBuilder;
    private final UnifiedCollectorApiClient unifiedCollectorApiClient;
    private final WechatMpFollowerSyncService wechatMpFollowerSyncService;
    private final WechatMpArticleSyncService wechatMpArticleSyncService;
    private final ChannelFollowerStatsSyncService channelFollowerStatsSyncService;
    private final DouyinFollowerSyncService douyinFollowerSyncService;
    private final DouyinVideoSyncService douyinVideoSyncService;
    private final DouyinVideoStatsSyncService douyinVideoStatsSyncService;
    private final WechatVideoWorkSyncService wechatVideoWorkSyncService;
    private final WechatVideoWorkStatsSyncService wechatVideoWorkStatsSyncService;
    private final KuaishouVideoSyncService kuaishouVideoSyncService;
    private final KuaishouVideoStatsSyncService kuaishouVideoStatsSyncService;
    private final XiaohongshuNoteSyncService xiaohongshuNoteSyncService;
    private final XiaohongshuNoteStatsSyncService xiaohongshuNoteStatsSyncService;
    private final WechatMpArticleStatsSyncService wechatMpArticleStatsSyncService;
    private final WechatMpArticleContentSyncService wechatMpArticleContentSyncService;

    @Transactional
    @AuditLog(module = "M10-collector-bind", action = "bind")
    public CollectorAccountBindRespVO bindAccount(Long oaAccountId) {
        AccountDO account = requireAccount(oaAccountId);
        CollectorAccountBindDO existing = findBind(oaAccountId);
        String oldCollectorId = existing != null ? existing.getCollectorAccountId() : null;
        return importAndSaveBind(account, oldCollectorId, StrUtil.isNotBlank(oldCollectorId));
    }

    @Transactional
    @AuditLog(module = "M10-collector-bind", action = "sync-credentials")
    public CollectorAccountBindRespVO syncCredentials(Long oaAccountId) {
        AccountDO account = requireAccount(oaAccountId);
        CollectorAccountBindDO existing = findBind(oaAccountId);
        String oldCollectorId = existing != null ? existing.getCollectorAccountId() : null;
        return importAndSaveBind(account, oldCollectorId, StrUtil.isNotBlank(oldCollectorId));
    }

    @Transactional
    @AuditLog(module = "M10-collector-bind", action = "test-connection")
    public CollectorAccountBindTestConnectionRespVO testConnection(Long oaAccountId) {
        AccountDO account = requireAccount(oaAccountId);
        CollectorAccountBindDO bind = findBind(oaAccountId);
        if (bind == null || StrUtil.isBlank(bind.getCollectorAccountId())) {
            throw new ServiceException(OaErrorCodes.ENTITY_NOT_EXISTS.getCode(), "请先绑定 Collector 账号");
        }

        CollectorAccountBindTestConnectionRespVO resp = new CollectorAccountBindTestConnectionRespVO();
        resp.setCollectorAccountId(bind.getCollectorAccountId());
        LocalDateTime now = LocalDateTime.now();
        try {
            Map<String, Object> accountInfo = unifiedCollectorApiClient.getAccount(bind.getCollectorAccountId());
            Map<String, Object> health = unifiedCollectorApiClient.getAccountHealthEntry(bind.getCollectorAccountId());
            String collectorStatus = firstNonBlank(accountInfo, health, "status");
            resp.setCollectorStatus(collectorStatus);
            boolean alive = Boolean.TRUE.equals(health.get("alive"));
            boolean active = "active".equalsIgnoreCase(collectorStatus) || alive;
            resp.setSuccess(active);
            resp.setConnStatus(mapConnStatus(collectorStatus, active));
            resp.setMessage(active ? "连接正常" : "Collector 账号需重新登录或凭证已失效");
            collectorAccountBindService.updateConnStatus(oaAccountId, resp.getConnStatus(), now);
        } catch (UnifiedCollectorApiException ex) {
            resp.setSuccess(false);
            resp.setConnStatus(ex.getConnStatus());
            resp.setMessage(ex.getMessage());
            collectorAccountBindService.updateConnStatus(oaAccountId, ex.getConnStatus(), now);
        } catch (Exception ex) {
            resp.setSuccess(false);
            resp.setConnStatus("DISCONNECTED");
            resp.setMessage("连接失败: " + ex.getMessage());
            collectorAccountBindService.updateConnStatus(oaAccountId, "DISCONNECTED", now);
        }
        return resp;
    }

    public CollectorAccountBindRespVO getBind(Long oaAccountId) {
        ConfigTenantSupport.assertAccountInTenant(accountMapper, oaAccountId);
        return collectorAccountBindService.getByOaAccountId(oaAccountId);
    }

    /**
     * M10-API-S-05: oa_account_id → bind → collector follower-list → 落库。
     */
    public int executeWechatMpFollowerCollect(Long oaAccountId) {
        requireAccount(oaAccountId);
        return wechatMpFollowerSyncService.syncFollowers(oaAccountId);
    }

    /** M10 P2: oa_account_id → bind → collector article-list → 落库。 */
    public int executeWechatMpArticleCollect(Long oaAccountId) {
        requireAccount(oaAccountId);
        return wechatMpArticleSyncService.syncArticles(oaAccountId);
    }

    /** M10 P2: MP follower-stats → oa_account_status_log */
    public int executeWechatMpFollowerStatsCollect(Long oaAccountId) {
        requireAccount(oaAccountId);
        return channelFollowerStatsSyncService.syncWechatMpFollowerStats(oaAccountId);
    }

    /** M10 P2: MP article-data → oa_wechat_mp_article 互动字段 */
    public int executeWechatMpArticleStatsCollect(Long oaAccountId) {
        requireAccount(oaAccountId);
        return wechatMpArticleStatsSyncService.syncArticleStats(oaAccountId);
    }

    /** M10 P2: MP article-download → oa_wechat_mp_article 正文字段 */
    public int executeWechatMpArticleContentCollect(Long oaAccountId) {
        requireAccount(oaAccountId);
        return wechatMpArticleContentSyncService.syncArticleContent(oaAccountId);
    }

    /** M10-API-S-06: DOUYIN follower-stats → oa_account_status_log */
    public int executeDouyinFollowerStatsCollect(Long oaAccountId) {
        requireAccount(oaAccountId);
        return channelFollowerStatsSyncService.syncDouyinFollowerStats(oaAccountId);
    }

    /** M10 P2: DOUYIN follower-list → oa_douyin_follower */
    public int executeDouyinFollowerListCollect(Long oaAccountId) {
        requireAccount(oaAccountId);
        return douyinFollowerSyncService.syncFollowers(oaAccountId);
    }

    /** M10 P2: DOUYIN video-list → oa_douyin_video */
    public int executeDouyinVideoListCollect(Long oaAccountId) {
        requireAccount(oaAccountId);
        return douyinVideoSyncService.syncVideos(oaAccountId);
    }

    /** M10 P2: DOUYIN video-stats → oa_douyin_video 明细字段 */
    public int executeDouyinVideoStatsCollect(Long oaAccountId) {
        requireAccount(oaAccountId);
        return douyinVideoStatsSyncService.syncVideoStats(oaAccountId);
    }

    /** M10-API-S-06: KUAISHOU follower-stats → oa_account_status_log */
    public int executeKuaishouFollowerStatsCollect(Long oaAccountId) {
        requireAccount(oaAccountId);
        return channelFollowerStatsSyncService.syncKuaishouFollowerStats(oaAccountId);
    }

    /** M10 P2: KUAISHOU video-list → oa_kuaishou_video */
    public int executeKuaishouVideoListCollect(Long oaAccountId) {
        requireAccount(oaAccountId);
        return kuaishouVideoSyncService.syncVideos(oaAccountId);
    }

    /** M10 P2: KUAISHOU video-stats → oa_kuaishou_video 明细字段 */
    public int executeKuaishouVideoStatsCollect(Long oaAccountId) {
        requireAccount(oaAccountId);
        return kuaishouVideoStatsSyncService.syncVideoStats(oaAccountId);
    }

    /** M10-API-S-06: WECHAT_VIDEO follower-stats → oa_account_status_log */
    public int executeWechatVideoFollowerStatsCollect(Long oaAccountId) {
        requireAccount(oaAccountId);
        return channelFollowerStatsSyncService.syncWechatVideoFollowerStats(oaAccountId);
    }

    /** M10 P2: WECHAT_VIDEO video-list → oa_wechat_video_work */
    public int executeWechatVideoWorkListCollect(Long oaAccountId) {
        requireAccount(oaAccountId);
        return wechatVideoWorkSyncService.syncWorks(oaAccountId);
    }

    /** M10 P2: WECHAT_VIDEO video-stats → oa_wechat_video_work 明细字段 */
    public int executeWechatVideoWorkStatsCollect(Long oaAccountId) {
        requireAccount(oaAccountId);
        return wechatVideoWorkStatsSyncService.syncWorkStats(oaAccountId);
    }

    /** M10-API-S-06: XIAOHONGSHU follower-stats → oa_account_status_log */
    public int executeXiaohongshuFollowerStatsCollect(Long oaAccountId) {
        requireAccount(oaAccountId);
        return channelFollowerStatsSyncService.syncXiaohongshuFollowerStats(oaAccountId);
    }

    /** M10 P2: XIAOHONGSHU note-list → oa_xiaohongshu_note */
    public int executeXiaohongshuNoteListCollect(Long oaAccountId) {
        requireAccount(oaAccountId);
        return xiaohongshuNoteSyncService.syncNotes(oaAccountId);
    }

    /** M10 P2: XIAOHONGSHU note-stats → oa_xiaohongshu_note 明细字段 */
    public int executeXiaohongshuNoteStatsCollect(Long oaAccountId) {
        requireAccount(oaAccountId);
        return xiaohongshuNoteStatsSyncService.syncNoteStats(oaAccountId);
    }

    /** M10-API-S-06: BILIBILI follower-stats → oa_account_status_log */
    public int executeBilibiliFollowerStatsCollect(Long oaAccountId) {
        requireAccount(oaAccountId);
        return channelFollowerStatsSyncService.syncBilibiliFollowerStats(oaAccountId);
    }

    private CollectorAccountBindRespVO importAndSaveBind(AccountDO account, String oldCollectorId, boolean syncMode) {
        String collectorPlatform = credentialBuilder.resolveCollectorPlatform(account.getPlatformType());
        Map<String, Object> credential = credentialBuilder.buildCredential(account);
        Map<String, Object> identity = buildIdentity(account);

        try {
            if (syncMode && StrUtil.isNotBlank(oldCollectorId)) {
                safeDeleteCollectorAccount(oldCollectorId);
            }
            UnifiedCollectorImportResult imported = unifiedCollectorApiClient.importAccount(
                    collectorPlatform,
                    credential,
                    account.getAccountName(),
                    identity);
            CollectorAccountBindSaveReq req = new CollectorAccountBindSaveReq();
            req.setOaAccountId(account.getId());
            req.setCollectorAccountId(imported.getAccountId());
            req.setPlatformType(account.getPlatformType());
            req.setBindStatus(BIND_STATUS_BOUND);
            req.setConnStatus(mapConnStatus(imported.getStatus(), true));
            collectorAccountBindService.saveOrUpdate(req);
            return collectorAccountBindService.getByOaAccountId(account.getId());
        } catch (UnifiedCollectorApiException ex) {
            persistFailedBind(account, ex.getConnStatus());
            throw new ServiceException(2022, ex.getMessage());
        } catch (ServiceException ex) {
            throw ex;
        } catch (Exception ex) {
            persistFailedBind(account, "DISCONNECTED");
            throw new ServiceException(2022, "Collector 绑定失败: " + ex.getMessage());
        }
    }

    private void persistFailedBind(AccountDO account, String connStatus) {
        CollectorAccountBindDO existing = findBind(account.getId());
        if (existing == null || StrUtil.isBlank(existing.getCollectorAccountId())) {
            return;
        }
        CollectorAccountBindSaveReq req = new CollectorAccountBindSaveReq();
        req.setOaAccountId(account.getId());
        req.setCollectorAccountId(existing.getCollectorAccountId());
        req.setPlatformType(account.getPlatformType());
        req.setBindStatus(BIND_STATUS_FAILED);
        req.setConnStatus(connStatus);
        collectorAccountBindService.saveOrUpdate(req);
    }

    private Map<String, Object> buildIdentity(AccountDO account) {
        Map<String, Object> identity = new LinkedHashMap<>();
        identity.put("oa_account_id", account.getId());
        if (StrUtil.isNotBlank(account.getExternalAccountId())) {
            String externalAccountId = account.getExternalAccountId().trim();
            identity.put("external_account_id", externalAccountId);
            // 抖音采集依赖 collector identity.sec_uid；运营可将 sec_uid 填入「平台账号 ID」
            if ("DOUYIN".equals(account.getPlatformType()) && looksLikeDouyinSecUid(externalAccountId)) {
                identity.put("sec_uid", externalAccountId);
            }
            // 小红书 user_id（24 位 hex）；可手动填入「平台账号 ID」作为兜底
            if ("XIAOHONGSHU".equals(account.getPlatformType()) && looksLikeXiaohongshuUserId(externalAccountId)) {
                identity.put("user_id", externalAccountId);
            }
        }
        return identity;
    }

    /** 抖音 sec_uid 通常为 MS4wLjABAAAA 前缀的长串，区别于短 numeric unique_id。 */
    private static boolean looksLikeDouyinSecUid(String value) {
        return value.length() >= 32 && value.startsWith("MS4wLjAB");
    }

    /** 小红书 user_id 通常为 24 位十六进制字符串。 */
    private static boolean looksLikeXiaohongshuUserId(String value) {
        return value != null && value.matches("^[a-f0-9]{24}$");
    }

    private AccountDO requireAccount(Long oaAccountId) {
        AccountDO account = accountMapper.selectById(oaAccountId);
        return ConfigTenantSupport.getRequiredInTenant(account);
    }

    private CollectorAccountBindDO findBind(Long oaAccountId) {
        Long tenantId = ConfigTenantSupport.requireTenantId();
        return collectorAccountBindMapper.selectOne(new LambdaQueryWrapper<CollectorAccountBindDO>()
                .eq(CollectorAccountBindDO::getTenantId, tenantId)
                .eq(CollectorAccountBindDO::getOaAccountId, oaAccountId));
    }

    private void safeDeleteCollectorAccount(String collectorAccountId) {
        try {
            unifiedCollectorApiClient.deleteAccount(collectorAccountId);
        } catch (Exception ex) {
            // 旧账号可能已被 collector 侧清理，忽略删除失败
        }
    }

    private String mapConnStatus(String collectorStatus, boolean active) {
        if (active) {
            return "CONNECTED";
        }
        if (StrUtil.isBlank(collectorStatus)) {
            return "DISCONNECTED";
        }
        return switch (collectorStatus.toLowerCase()) {
            case "relogin_needed", "cookie_expired" -> "TOKEN_FAIL";
            case "circuit_broken" -> "DISCONNECTED";
            default -> "DISCONNECTED";
        };
    }

    private String firstNonBlank(Map<String, Object> primary, Map<String, Object> secondary, String key) {
        Object val = primary.get(key);
        if (val != null && StrUtil.isNotBlank(String.valueOf(val))) {
            return String.valueOf(val);
        }
        val = secondary.get(key);
        return val == null ? null : String.valueOf(val);
    }
}
