package cn.iocoder.yudao.module.oa.service.collect.unified;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.exception.OaErrorCodes;
import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.module.oa.api.dto.collect.CollectorAccountBindSaveReq;
import cn.iocoder.yudao.module.oa.api.dto.collect.CollectorQrLoginPollRespVO;
import cn.iocoder.yudao.module.oa.api.dto.collect.CollectorQrLoginStartRespVO;
import cn.iocoder.yudao.module.oa.dal.dataobject.account.AccountDO;
import cn.iocoder.yudao.module.oa.dal.dataobject.account.OaAccountExtDO;
import cn.iocoder.yudao.module.oa.dal.mysql.account.AccountMapper;
import cn.iocoder.yudao.module.oa.service.account.OaAccountExtDataService;
import cn.iocoder.yudao.module.oa.framework.audit.AuditLog;
import cn.iocoder.yudao.module.oa.service.collect.CollectorAccountBindService;
import cn.iocoder.yudao.module.oa.service.account.WechatOfficialAccountResolver;
import cn.iocoder.yudao.module.oa.service.config.ConfigTenantSupport;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.oa.util.AesUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * OA 代理 unify-collector-api 统一扫码登录（ADR-050 · M4 采集 Tab）。
 */
@Service
@RequiredArgsConstructor
public class CollectorQrLoginService {

    private static final String BIND_STATUS_BOUND = "BOUND";
    private static final Pattern KUAISHOU_AUTH_TOKEN = Pattern.compile("kuaishou\\.web\\.cp\\.api_st=([^;\\s]+)");

    private final AccountMapper accountMapper;
    private final WechatOfficialAccountResolver wechatOfficialAccountResolver;
    private final OaAccountExtDataService oaAccountExtDataService;
    private final CollectorAccountBindService collectorAccountBindService;
    private final UnifiedCollectorAdapter unifiedCollectorAdapter;
    private final UnifiedCollectorApiClient unifiedCollectorApiClient;
    private final AesUtil aesUtil;

    @AuditLog(module = "M10-collector-qr", action = "start")
    public CollectorQrLoginStartRespVO startQrLogin(Long oaAccountId) {
        AccountDO account = requireQrSupportedAccount(oaAccountId);
        String collectorPlatform = CollectorQrLoginSupport.resolveCollectorPlatform(account.getPlatformType());
        Map<String, Object> data;
        try {
            data = unifiedCollectorApiClient.startQrLogin(collectorPlatform);
        } catch (UnifiedCollectorApiException ex) {
            throw new ServiceException(2022, ex.getMessage());
        }
        CollectorQrLoginStartRespVO resp = new CollectorQrLoginStartRespVO();
        resp.setSessionId(str(data.get("session_id")));
        resp.setQrcodeBase64(normalizeQrcodeBase64(str(data.get("qrcode_base64"))));
        String qrcodeUrl = str(data.get("qrcode_url"));
        if (StrUtil.isBlank(resp.getQrcodeBase64()) && isHttpUrl(qrcodeUrl)) {
            resp.setQrcodeUrl(qrcodeUrl.trim());
        } else if (StrUtil.isBlank(resp.getQrcodeBase64()) && StrUtil.isNotBlank(qrcodeUrl)) {
            resp.setQrcodeBase64(normalizeQrcodeBase64(qrcodeUrl));
        }
        resp.setStatus(str(data.get("status")));
        resp.setMessage(firstNonBlank(data, "message", "请使用手机扫码登录"));
        Object ttl = data.get("expires_in");
        if (ttl == null) {
            ttl = data.get("expires_in_seconds");
        }
        if (ttl instanceof Number number) {
            resp.setExpiresInSeconds(number.intValue());
        } else if (ttl != null && StrUtil.isNotBlank(String.valueOf(ttl))) {
            try {
                resp.setExpiresInSeconds(Integer.parseInt(String.valueOf(ttl)));
            } catch (NumberFormatException ignored) {
                resp.setExpiresInSeconds(300);
            }
        } else {
            resp.setExpiresInSeconds(300);
        }
        if (StrUtil.isBlank(resp.getSessionId())) {
            throw new ServiceException(2022, "Collector 未返回扫码会话 ID");
        }
        if (StrUtil.isBlank(resp.getQrcodeBase64()) && StrUtil.isBlank(resp.getQrcodeUrl())) {
            throw new ServiceException(2022, "Collector 未返回二维码，请改用 Cookie 粘贴或本地 helper 登录");
        }
        return resp;
    }

    @Transactional
    @AuditLog(module = "M10-collector-qr", action = "poll")
    public CollectorQrLoginPollRespVO pollQrLogin(Long oaAccountId, String sessionId) {
        AccountDO account = requireQrSupportedAccount(oaAccountId);
        if (StrUtil.isBlank(sessionId)) {
            throw new ServiceException(OaErrorCodes.BAD_REQUEST.getCode(), "sessionId 不能为空");
        }
        Map<String, Object> data;
        try {
            data = unifiedCollectorApiClient.pollQrLogin(sessionId.trim());
        } catch (UnifiedCollectorApiException ex) {
            throw new ServiceException(2022, ex.getMessage());
        }
        CollectorQrLoginPollRespVO resp = new CollectorQrLoginPollRespVO();
        resp.setStatus(normalizeStatus(str(data.get("status"))));
        resp.setMessage(firstNonBlank(data, "message", statusHint(resp.getStatus())));
        resp.setCollectorAccountId(firstNonBlank(data, "account_id", "collector_account_id"));
        if ("confirmed".equals(resp.getStatus())) {
            boolean saved = applyCredentialsFromPoll(account, data);
            resp.setCredentialsSaved(saved);
            if (StrUtil.isNotBlank(resp.getCollectorAccountId())) {
                persistBind(account, resp.getCollectorAccountId());
                resp.setBindStatus(BIND_STATUS_BOUND);
                resp.setConnStatus("CONNECTED");
                resp.setMessage("扫码登录成功，凭证与绑定已更新");
            } else if (saved) {
                try {
                    var bind = unifiedCollectorAdapter.bindAccount(account.getId());
                    resp.setCollectorAccountId(bind.getCollectorAccountId());
                    resp.setBindStatus(bind.getBindStatus());
                    resp.setConnStatus(bind.getConnStatus());
                    resp.setMessage("扫码登录成功，已保存凭证并绑定采集服务");
                } catch (ServiceException ex) {
                    resp.setMessage("凭证已保存，绑定采集服务失败：" + ex.getMessage());
                }
            } else {
                resp.setMessage("扫码已完成但未返回凭证，请手动粘贴 Cookie 后绑定");
            }
        }
        return resp;
    }

    @AuditLog(module = "M10-collector-qr", action = "cancel")
    public void cancelQrLogin(Long oaAccountId, String sessionId) {
        requireQrSupportedAccount(oaAccountId);
        if (StrUtil.isBlank(sessionId)) {
            throw new ServiceException(OaErrorCodes.BAD_REQUEST.getCode(), "sessionId 不能为空");
        }
        try {
            unifiedCollectorApiClient.cancelQrLogin(sessionId.trim());
        } catch (UnifiedCollectorApiException ex) {
            throw new ServiceException(2022, ex.getMessage());
        }
    }

    private void persistBind(AccountDO account, String collectorAccountId) {
        CollectorAccountBindSaveReq req = new CollectorAccountBindSaveReq();
        req.setOaAccountId(account.getId());
        req.setCollectorAccountId(collectorAccountId);
        req.setPlatformType(account.getPlatformType());
        req.setBindStatus(BIND_STATUS_BOUND);
        req.setConnStatus("CONNECTED");
        collectorAccountBindService.saveOrUpdate(req);
    }

    private boolean applyCredentialsFromPoll(AccountDO account, Map<String, Object> pollData) {
        Map<String, Object> credential = extractCredential(pollData);
        if (credential.isEmpty()) {
            return false;
        }
        boolean updated = false;
        String cookie = str(credential.get("cookie"));
        if (StrUtil.isNotBlank(cookie)) {
            account.setCookieEncrypted(aesUtil.encrypt(cookie.trim()));
            updated = true;
        }
        if ("WECHAT_OFFICIAL".equals(account.getPlatformType())) {
            String token = firstNonBlank(credential, "token", "mp_token");
            if (StrUtil.isNotBlank(token)) {
                account.setMpTokenEncrypted(aesUtil.encrypt(token.trim()));
                updated = true;
            }
        }
        if ("KUAISHOU".equals(account.getPlatformType())) {
            String authToken = str(credential.get("auth_token"));
            if (StrUtil.isBlank(authToken) && StrUtil.isNotBlank(cookie)) {
                authToken = extractKuaishouAuthToken(cookie);
            }
            if (StrUtil.isNotBlank(authToken)) {
                account.setAuthTokenEncrypted(aesUtil.encrypt(authToken.trim()));
                updated = true;
            }
        }
        if (updated) {
            Long tenantId = ConfigTenantSupport.requireTenantId();
            if (wechatOfficialAccountResolver.isMpBackedAccount(account.getId(), tenantId)) {
                persistMpCredentials(account.getId(), tenantId, account);
            } else {
                account.setUpdateTime(LocalDateTime.now());
                accountMapper.updateById(account);
            }
        }
        return updated;
    }

    private void persistMpCredentials(Long mpAccountId, Long tenantId, AccountDO account) {
        OaAccountExtDO ext = oaAccountExtDataService.findByMpAccountId(tenantId, mpAccountId);
        if (ext == null) {
            ext = new OaAccountExtDO();
            ext.setTenantId(tenantId);
            ext.setMpAccountId(mpAccountId);
            ext.setPlatformType("WECHAT_OFFICIAL");
            ext.setCreator(TenantContextHolder.getUsername());
            ext.setCreateTime(LocalDateTime.now());
        }
        if (StrUtil.isNotBlank(account.getCookieEncrypted())) {
            ext.setCookieEncrypted(account.getCookieEncrypted());
        }
        if (StrUtil.isNotBlank(account.getMpTokenEncrypted())) {
            ext.setMpTokenEncrypted(account.getMpTokenEncrypted());
        }
        ext.setUpdater(TenantContextHolder.getUsername());
        ext.setUpdateTime(LocalDateTime.now());
        if (ext.getId() == null) {
            oaAccountExtDataService.insert(ext);
        } else {
            oaAccountExtDataService.updateById(ext);
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> extractCredential(Map<String, Object> pollData) {
        Object raw = pollData.get("credential");
        if (raw instanceof Map<?, ?> map) {
            Map<String, Object> credential = new LinkedHashMap<>();
            map.forEach((k, v) -> credential.put(String.valueOf(k), v));
            return credential;
        }
        Map<String, Object> fallback = new LinkedHashMap<>();
        if (pollData.get("cookie") != null) {
            fallback.put("cookie", pollData.get("cookie"));
        }
        if (pollData.get("token") != null) {
            fallback.put("token", pollData.get("token"));
        }
        if (pollData.get("auth_token") != null) {
            fallback.put("auth_token", pollData.get("auth_token"));
        }
        return fallback;
    }

    private AccountDO requireQrSupportedAccount(Long oaAccountId) {
        AccountDO account = wechatOfficialAccountResolver.requireTenantAccount(
                oaAccountId, ConfigTenantSupport.requireTenantId());
        if (!CollectorQrLoginSupport.supportsQrLogin(account.getPlatformType())) {
            throw new ServiceException(OaErrorCodes.DICT_VALUE_INVALID.getCode(), "当前平台不支持扫码登录");
        }
        return account;
    }

    private static String normalizeStatus(String status) {
        if (StrUtil.isBlank(status)) {
            return "pending";
        }
        return switch (status.toUpperCase()) {
            case "SUCCESS", "CONFIRMED" -> "confirmed";
            case "WAITING_SCAN", "PENDING" -> "pending";
            case "SCANNED" -> "scanned";
            case "EXPIRED" -> "expired";
            case "ERROR", "FAILED" -> "error";
            default -> status.toLowerCase();
        };
    }

    private static String statusHint(String status) {
        return switch (status) {
            case "pending" -> "等待扫码";
            case "scanned" -> "已扫码，请在手机上确认";
            case "confirmed" -> "登录成功";
            case "expired" -> "二维码已过期，请重新发起";
            case "error" -> "扫码失败";
            default -> "";
        };
    }

    private static boolean isHttpUrl(String value) {
        if (StrUtil.isBlank(value)) {
            return false;
        }
        String trimmed = value.trim().toLowerCase();
        return trimmed.startsWith("http://") || trimmed.startsWith("https://");
    }

    private static String normalizeQrcodeBase64(String value) {
        if (StrUtil.isBlank(value)) {
            return null;
        }
        String trimmed = value.trim();
        int comma = trimmed.indexOf(',');
        if (trimmed.startsWith("data:image") && comma > 0) {
            return trimmed.substring(comma + 1);
        }
        return trimmed;
    }

    private static String extractKuaishouAuthToken(String cookie) {
        Matcher matcher = KUAISHOU_AUTH_TOKEN.matcher(cookie);
        return matcher.find() ? matcher.group(1) : null;
    }

    private static String str(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private static String firstNonBlank(Map<String, Object> map, String... keys) {
        for (String key : keys) {
            Object val = map.get(key);
            if (val != null && StrUtil.isNotBlank(String.valueOf(val))) {
                return String.valueOf(val);
            }
        }
        return null;
    }
}
