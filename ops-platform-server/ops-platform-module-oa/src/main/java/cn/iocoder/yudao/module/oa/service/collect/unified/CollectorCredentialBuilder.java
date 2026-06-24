package cn.iocoder.yudao.module.oa.service.collect.unified;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import cn.iocoder.yudao.framework.common.exception.OaErrorCodes;
import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.module.oa.dal.dataobject.account.AccountDO;
import cn.iocoder.yudao.module.oa.util.AesUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * 从 oa_account 组装 unify-collector credential JSON（ADR-047 · 不读 AppSecret）。
 */
@Component
@RequiredArgsConstructor
public class CollectorCredentialBuilder {

    private static final Set<String> SUPPORTED_PLATFORMS = Set.of(
            "WECHAT_OFFICIAL", "WECHAT_VIDEO", "DOUYIN", "KUAISHOU", "XIAOHONGSHU", "BILIBILI");

    private static final Map<String, String> OA_TO_COLLECTOR = Map.of(
            "WECHAT_OFFICIAL", "wechat_mp",
            "WECHAT_VIDEO", "wechat_channels",
            "DOUYIN", "douyin",
            "KUAISHOU", "kuaishou",
            "XIAOHONGSHU", "xiaohongshu",
            "BILIBILI", "bilibili");

    private final AesUtil aesUtil;

    public String resolveCollectorPlatform(String oaPlatformType) {
        String collectorPlatform = OA_TO_COLLECTOR.get(oaPlatformType);
        if (collectorPlatform == null) {
            if (!SUPPORTED_PLATFORMS.contains(oaPlatformType)) {
                throw new ServiceException(OaErrorCodes.DICT_VALUE_INVALID.getCode(), "平台不支持 Channel-A 采集绑定");
            }
            throw new ServiceException(OaErrorCodes.DICT_VALUE_INVALID.getCode(), "平台映射未配置");
        }
        return collectorPlatform;
    }

    public Map<String, Object> buildCredential(AccountDO account) {
        String platform = account.getPlatformType();
        if (!SUPPORTED_PLATFORMS.contains(platform)) {
            throw new ServiceException(OaErrorCodes.DICT_VALUE_INVALID.getCode(), "平台不支持 Channel-A 采集绑定");
        }
        String cookie = decrypt(account.getCookieEncrypted());
        Map<String, Object> credential = new LinkedHashMap<>();
        switch (platform) {
            case "WECHAT_OFFICIAL" -> {
                String token = decrypt(account.getMpTokenEncrypted());
                if (StrUtil.isBlank(cookie) || StrUtil.isBlank(token)) {
                    throw new ServiceException(OaErrorCodes.ENTITY_NOT_EXISTS.getCode(), "公众号凭证不完整，需 cookie 与 mp_token");
                }
                credential.put("cookie", cookie);
                credential.put("token", token);
            }
            case "WECHAT_VIDEO", "DOUYIN", "XIAOHONGSHU", "BILIBILI" -> {
                if (StrUtil.isBlank(cookie)) {
                    throw new ServiceException(OaErrorCodes.ENTITY_NOT_EXISTS.getCode(), "Cookie 凭证不完整");
                }
                credential.put("cookie", cookie);
            }
            case "KUAISHOU" -> {
                String authToken = decrypt(account.getAuthTokenEncrypted());
                if (StrUtil.isBlank(cookie)) {
                    throw new ServiceException(OaErrorCodes.ENTITY_NOT_EXISTS.getCode(), "快手 Cookie 凭证不完整");
                }
                credential.put("cookie", mergeKuaishouCookie(cookie, authToken));
                if (StrUtil.isNotBlank(authToken)) {
                    credential.put("auth_token", authToken);
                }
                mergeFieldMapping(credential, account.getFieldMapping());
            }
            default -> throw new ServiceException(OaErrorCodes.DICT_VALUE_INVALID.getCode(), "平台不支持 Channel-A 采集绑定");
        }
        return credential;
    }

    private void mergeFieldMapping(Map<String, Object> credential, String fieldMappingJson) {
        if (StrUtil.isBlank(fieldMappingJson)) {
            return;
        }
        if (!JSONUtil.isTypeJSONObject(fieldMappingJson)) {
            throw new ServiceException(OaErrorCodes.BAD_REQUEST.getCode(), "field_mapping 必须为 JSON 对象");
        }
        credential.put("field_mapping", JSONUtil.parseObj(fieldMappingJson));
    }

    private String mergeKuaishouCookie(String cookie, String authToken) {
        if (StrUtil.isBlank(authToken) || cookie.contains("kuaishou.web.cp.api_st=")) {
            return cookie;
        }
        return cookie + "; kuaishou.web.cp.api_st=" + authToken;
    }

    private String decrypt(String encrypted) {
        if (StrUtil.isBlank(encrypted)) {
            return null;
        }
        try {
            return aesUtil.decrypt(encrypted);
        } catch (Exception ex) {
            throw new ServiceException(OaErrorCodes.ENTITY_NOT_EXISTS.getCode(), "凭证解密失败");
        }
    }
}
