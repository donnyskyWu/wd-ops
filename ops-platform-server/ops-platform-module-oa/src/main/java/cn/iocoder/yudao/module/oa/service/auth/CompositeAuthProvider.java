package cn.iocoder.yudao.module.oa.service.auth;

import cn.iocoder.yudao.module.oa.framework.auth.AuthProvider;
import cn.iocoder.yudao.module.oa.framework.auth.LoginUser;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * ADR-047 §5.3 + D-SYS-03: Gateway login-user / check Feign first; dev-token; legacy @DS last.
 */
@Service
@Primary
@RequiredArgsConstructor
public class CompositeAuthProvider implements AuthProvider {

    private final GatewayAuthProvider gatewayAuthProvider;
    private final DevAuthProvider devAuthProvider;
    private final FootballAuthProvider footballAuthProvider;

    @Override
    public Optional<LoginUser> authenticate(HttpServletRequest request, String token, Long headerTenantId) {
        Optional<LoginUser> gatewayUser = gatewayAuthProvider.authenticate(request, token, headerTenantId);
        if (gatewayUser.isPresent()) {
            return gatewayUser;
        }
        Optional<LoginUser> devUser = devAuthProvider.authenticate(request, token, headerTenantId);
        if (devUser.isPresent()) {
            return devUser;
        }
        return footballAuthProvider.authenticate(request, token, headerTenantId);
    }
}
