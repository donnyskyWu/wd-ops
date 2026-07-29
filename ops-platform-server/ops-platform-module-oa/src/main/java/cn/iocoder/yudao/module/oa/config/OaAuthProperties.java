package cn.iocoder.yudao.module.oa.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * C-WP1 auth cutover (D-SYS-03): Gateway login-user first; legacy @DS token only when enabled.
 */
@Data
@Component
@ConfigurationProperties(prefix = "oa.auth")
public class OaAuthProperties {

    /**
     * Prefer Gateway {@code login-user} header and {@code OAuth2TokenCommonApi.checkAccessToken}.
     */
    private GatewayLoginUser gatewayLoginUser = new GatewayLoginUser();

    /**
     * Legacy {@code FootballOAuth2TokenMapper} / Redis token read (dev-local-multidb / H2 IT only).
     */
    private LegacyDsToken legacyDsToken = new LegacyDsToken();

    @Data
    public static class GatewayLoginUser {
        private boolean enabled = true;
    }

    @Data
    public static class LegacyDsToken {
        private boolean enabled = false;
    }

    public boolean isGatewayLoginUserEnabled() {
        return gatewayLoginUser != null && gatewayLoginUser.isEnabled();
    }

    public boolean isLegacyDsTokenEnabled() {
        return legacyDsToken != null && legacyDsToken.isEnabled();
    }
}
