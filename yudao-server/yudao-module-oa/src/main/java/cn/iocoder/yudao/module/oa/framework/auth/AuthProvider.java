package cn.iocoder.yudao.module.oa.framework.auth;

import jakarta.servlet.http.HttpServletRequest;

import java.util.Optional;

public interface AuthProvider {

    Optional<LoginUser> authenticate(HttpServletRequest request, String token, Long headerTenantId);
}
