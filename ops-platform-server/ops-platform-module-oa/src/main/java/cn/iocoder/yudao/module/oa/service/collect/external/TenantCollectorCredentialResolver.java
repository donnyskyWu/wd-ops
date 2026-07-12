package cn.iocoder.yudao.module.oa.service.collect.external;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.module.oa.dal.dataobject.collect.TenantCollectorCredentialDO;
import cn.iocoder.yudao.module.oa.dal.mysql.collect.TenantCollectorCredentialMapper;
import cn.iocoder.yudao.module.oa.service.config.ConfigTenantSupport;
import cn.iocoder.yudao.module.oa.util.AesUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * 租户级 collector 凭账号解析：租户表 &gt; 部署 env（ADR-052 §3.4）。
 */
@Component
@RequiredArgsConstructor
public class TenantCollectorCredentialResolver {

    private static final String STATUS_ENABLED = "ENABLED";
    private static final String DEFAULT_PROFILE = "default";

    private final TenantCollectorCredentialMapper tenantCollectorCredentialMapper;
    private final AesUtil aesUtil;

    @Value("${oa.external-collector.kuaishou-cookie:}")
    private String envKuaishouCookie;

    @Value("${oa.external-collector.kuaishou-auth-token:}")
    private String envKuaishouAuthToken;

    public ExternalCollectorCredential resolve(String platform, String credentialProfile) {
        String profile = StrUtil.blankToDefault(credentialProfile, DEFAULT_PROFILE);
        ExternalCollectorCredential tenantCredential = resolveFromTenant(platform, profile);
        if (tenantCredential != null && tenantCredential.hasCookie()) {
            return tenantCredential;
        }
        ExternalCollectorCredential envCredential = resolveFromEnv(platform);
        if (envCredential != null && envCredential.hasCookie()) {
            return envCredential;
        }
        throw new ServiceException(1501, "租户采集凭账号缺失（platform=" + platform + ", profile=" + profile + "）");
    }

    private ExternalCollectorCredential resolveFromTenant(String platform, String profile) {
        Long tenantId = ConfigTenantSupport.requireTenantId();
        TenantCollectorCredentialDO row = tenantCollectorCredentialMapper.selectOne(
                new LambdaQueryWrapper<TenantCollectorCredentialDO>()
                        .eq(TenantCollectorCredentialDO::getTenantId, tenantId)
                        .eq(TenantCollectorCredentialDO::getPlatform, platform)
                        .eq(TenantCollectorCredentialDO::getCredentialProfile, profile)
                        .eq(TenantCollectorCredentialDO::getStatus, STATUS_ENABLED));
        if (row == null || StrUtil.isBlank(row.getCookieEncrypted())) {
            return null;
        }
        String cookie = decrypt(row.getCookieEncrypted());
        String authToken = decryptOptional(row.getAuthTokenEncrypted());
        return new ExternalCollectorCredential(mergeKuaishouCookie(cookie, authToken, platform), authToken);
    }

    private ExternalCollectorCredential resolveFromEnv(String platform) {
        if (!"KUAISHOU".equals(platform)) {
            return null;
        }
        if (StrUtil.isBlank(envKuaishouCookie)) {
            return null;
        }
        return new ExternalCollectorCredential(
                mergeKuaishouCookie(envKuaishouCookie, envKuaishouAuthToken, platform),
                envKuaishouAuthToken);
    }

    private String mergeKuaishouCookie(String cookie, String authToken, String platform) {
        if (!"KUAISHOU".equals(platform) || StrUtil.isBlank(authToken) || cookie.contains("kuaishou.web.cp.api_st=")) {
            return cookie;
        }
        return cookie + "; kuaishou.web.cp.api_st=" + authToken;
    }

    private String decrypt(String encrypted) {
        try {
            return aesUtil.decrypt(encrypted);
        } catch (Exception ex) {
            throw new ServiceException(1501, "租户采集凭账号解密失败");
        }
    }

    private String decryptOptional(String encrypted) {
        if (StrUtil.isBlank(encrypted)) {
            return null;
        }
        return decrypt(encrypted);
    }
}
