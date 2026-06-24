package cn.iocoder.yudao.module.oa.service.collect.wework;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.exception.OaErrorCodes;
import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.module.oa.api.dto.personal.WeworkTestConnectionRespVO;
import cn.iocoder.yudao.module.oa.dal.dataobject.personal.WeworkAccountDO;
import cn.iocoder.yudao.module.oa.dal.mysql.personal.WeworkAccountMapper;
import cn.iocoder.yudao.module.oa.framework.audit.AuditLog;
import cn.iocoder.yudao.module.oa.service.config.ConfigTenantSupport;
import cn.iocoder.yudao.module.oa.util.AesUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * 企微通道 Adapter（Channel-C · ADR-048 · M10-WECOM-S-01/S-02）。
 */
@Component
@RequiredArgsConstructor
public class WeComAdapter {

    public static final String DATA_TYPE_DAILY_STATS = "WECOM_DAILY_STATS";

    private static final String CONN_CONNECTED = "CONNECTED";
    private static final String CONN_DISCONNECTED = "DISCONNECTED";
    private static final String CONN_TOKEN_FAIL = "TOKEN_FAIL";
    private static final String CONN_PERMISSION_DENIED = "PERMISSION_DENIED";

    private final WeworkAccountMapper weworkAccountMapper;
    private final WeComDailyStatsSyncService weComDailyStatsSyncService;
    private final WeComApiClient weComApiClient;
    private final AesUtil aesUtil;

    public int executeDailyStatsCollect(Long weworkAccountId) {
        requireWeworkAccount(weworkAccountId);
        return weComDailyStatsSyncService.syncDailyStats(weworkAccountId);
    }

    @Transactional
    @AuditLog(module = "M10-wework", action = "test-connection")
    public WeworkTestConnectionRespVO testConnection(Long weworkAccountId) {
        WeworkAccountDO account = requireWeworkAccount(weworkAccountId);
        WeworkTestConnectionRespVO resp = new WeworkTestConnectionRespVO();
        LocalDateTime now = LocalDateTime.now();
        try {
            String secret = decryptSecret(account.getSecretEncrypted());
            String accessToken = weComApiClient.getAccessToken(account.getCorpId(), secret);
            if (StrUtil.isBlank(accessToken)) {
                throw new WeComApiException("企微未返回 access_token");
            }
            resp.setSuccess(true);
            resp.setConnStatus(CONN_CONNECTED);
            resp.setMessage("连接正常");
            account.setConnStatus(CONN_CONNECTED);
        } catch (WeComApiException ex) {
            resp.setSuccess(false);
            resp.setConnStatus(mapConnStatus(ex.getErrcode()));
            resp.setMessage(ex.getMessage());
            account.setConnStatus(resp.getConnStatus());
        } catch (ServiceException ex) {
            throw ex;
        } catch (Exception ex) {
            resp.setSuccess(false);
            resp.setConnStatus(CONN_DISCONNECTED);
            resp.setMessage("连接失败: " + ex.getMessage());
            account.setConnStatus(CONN_DISCONNECTED);
        }
        account.setLastHealthCheckAt(now);
        ConfigTenantSupport.fillUpdate(account);
        weworkAccountMapper.updateById(account);
        return resp;
    }

    public WeworkAccountDO requireWeworkAccount(Long weworkAccountId) {
        WeworkAccountDO entity = weworkAccountMapper.selectById(weworkAccountId);
        return ConfigTenantSupport.getRequiredInTenant(entity);
    }

    private String decryptSecret(String secretEncrypted) {
        if (StrUtil.isBlank(secretEncrypted)) {
            throw new ServiceException(OaErrorCodes.ENTITY_NOT_EXISTS.getCode(), "企微应用 Secret 未配置");
        }
        return aesUtil.decrypt(secretEncrypted);
    }

    private static String mapConnStatus(int errcode) {
        return switch (errcode) {
            case 40001, 40013, 40014, 42001 -> CONN_TOKEN_FAIL;
            case 60011, 60020, 60030 -> CONN_PERMISSION_DENIED;
            default -> CONN_DISCONNECTED;
        };
    }
}
