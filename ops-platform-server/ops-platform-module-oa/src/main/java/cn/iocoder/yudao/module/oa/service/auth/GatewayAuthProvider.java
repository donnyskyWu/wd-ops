package cn.iocoder.yudao.module.oa.service.auth;

import cn.iocoder.yudao.framework.common.biz.system.oauth2.OAuth2TokenCommonApi;
import cn.iocoder.yudao.framework.common.biz.system.oauth2.dto.OAuth2AccessTokenCheckRespDTO;
import cn.iocoder.yudao.framework.common.exception.OaErrorCodes;
import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.module.oa.config.OaAuthProperties;
import cn.iocoder.yudao.module.oa.framework.auth.AuthProvider;
import cn.iocoder.yudao.module.oa.framework.auth.LoginUser;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * C-WP1 / D-SYS-03: Gateway {@code login-user} header first, then {@code OAuth2TokenCommonApi.checkAccessToken}.
 * Does not read {@code system_oauth2_access_token} via {@code @DS("system")}.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GatewayAuthProvider implements AuthProvider {

    private final OaAuthProperties authProperties;
    private final OAuth2TokenCommonApi oauth2TokenCommonApi;
    private final LoginUserAssemblySupport loginUserAssemblySupport;

    @Override
    public Optional<LoginUser> authenticate(HttpServletRequest request, String token, Long headerTenantId) {
        if (!authProperties.isGatewayLoginUserEnabled()) {
            return Optional.empty();
        }
        if (headerTenantId == null) {
            throw new ServiceException(OaErrorCodes.UNAUTHORIZED.getCode(), "缺少 X-Tenant-Id");
        }

        GatewayLoginUserDTO gatewayUser = GatewayLoginUserSupport.parse(request);
        if (gatewayUser != null) {
            Long tenantId = gatewayUser.getTenantId() != null ? gatewayUser.getTenantId() : headerTenantId;
            if (!headerTenantId.equals(tenantId)) {
                throw new ServiceException(OaErrorCodes.TENANT_FORBIDDEN);
            }
            LoginUser loginUser = loginUserAssemblySupport.buildLoginUser(
                    gatewayUser.getId(), tenantId, gatewayUser.getInfo());
            return loginUser != null ? Optional.of(loginUser) : Optional.empty();
        }

        if (token == null || token.isBlank()) {
            return Optional.empty();
        }

        OAuth2AccessTokenCheckRespDTO checked = checkTokenViaFeign(token);
        if (checked == null || checked.getUserId() == null) {
            return Optional.empty();
        }
        Long tenantId = checked.getTenantId() != null ? checked.getTenantId() : headerTenantId;
        if (!headerTenantId.equals(tenantId)) {
            throw new ServiceException(OaErrorCodes.TENANT_FORBIDDEN);
        }
        LoginUser loginUser = loginUserAssemblySupport.buildLoginUser(
                checked.getUserId(), tenantId, checked.getUserInfo());
        return loginUser != null ? Optional.of(loginUser) : Optional.empty();
    }

    private OAuth2AccessTokenCheckRespDTO checkTokenViaFeign(String token) {
        if (oauth2TokenCommonApi == null) {
            return null;
        }
        try {
            CommonResult<OAuth2AccessTokenCheckRespDTO> result = oauth2TokenCommonApi.checkAccessToken(token);
            if (result == null || !result.isSuccess()) {
                return null;
            }
            return result.getData();
        } catch (Exception ex) {
            log.debug("[checkTokenViaFeign][Feign check 不可用: {}]", ex.getMessage());
            return null;
        }
    }
}
