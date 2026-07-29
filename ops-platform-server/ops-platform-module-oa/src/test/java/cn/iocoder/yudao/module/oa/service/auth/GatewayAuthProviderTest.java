package cn.iocoder.yudao.module.oa.service.auth;

import cn.iocoder.yudao.framework.common.biz.system.oauth2.OAuth2TokenCommonApi;
import cn.iocoder.yudao.framework.common.biz.system.oauth2.dto.OAuth2AccessTokenCheckRespDTO;
import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.module.oa.config.OaAuthProperties;
import cn.iocoder.yudao.module.oa.framework.auth.LoginUser;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GatewayAuthProviderTest {

    @Mock
    private OAuth2TokenCommonApi oauth2TokenCommonApi;
    @Mock
    private LoginUserAssemblySupport loginUserAssemblySupport;
    @Mock
    private HttpServletRequest request;

    private GatewayAuthProvider provider;

    @BeforeEach
    void setUp() {
        OaAuthProperties properties = new OaAuthProperties();
        properties.getGatewayLoginUser().setEnabled(true);
        provider = new GatewayAuthProvider(properties, oauth2TokenCommonApi, loginUserAssemblySupport);
    }

    @Test
    @DisplayName("C-WP1: login-user 头优先，不调用 checkAccessToken")
    void prefersLoginUserHeader() {
        when(request.getHeader(GatewayLoginUserSupport.LOGIN_USER_HEADER)).thenReturn(
                "%7B%22id%22%3A1024%2C%22tenantId%22%3A1%7D");
        LoginUser loginUser = LoginUser.builder()
                .userId(1024L)
                .tenantId(1L)
                .username("admin")
                .authorities(Set.of("oa:user:list"))
                .build();
        when(loginUserAssemblySupport.buildLoginUser(eq(1024L), eq(1L), eq(null))).thenReturn(loginUser);

        Optional<LoginUser> result = provider.authenticate(request, "football-token", 1L);

        assertTrue(result.isPresent());
        assertEquals(1024L, result.get().getUserId());
        verify(oauth2TokenCommonApi, never()).checkAccessToken(org.mockito.ArgumentMatchers.any());
    }

    @Test
    @DisplayName("C-WP1: 无 login-user 时走 OAuth2TokenCommonApi.checkAccessToken")
    void fallsBackToCheckAccessToken() {
        when(request.getHeader(GatewayLoginUserSupport.LOGIN_USER_HEADER)).thenReturn(null);
        OAuth2AccessTokenCheckRespDTO checked = new OAuth2AccessTokenCheckRespDTO();
        checked.setUserId(2048L);
        checked.setTenantId(1L);
        checked.setUserInfo(Map.of("nickname", "测试"));
        when(oauth2TokenCommonApi.checkAccessToken("token-abc"))
                .thenReturn(CommonResult.success(checked));
        LoginUser loginUser = LoginUser.builder().userId(2048L).tenantId(1L).build();
        when(loginUserAssemblySupport.buildLoginUser(eq(2048L), eq(1L), eq(checked.getUserInfo())))
                .thenReturn(loginUser);

        Optional<LoginUser> result = provider.authenticate(request, "token-abc", 1L);

        assertTrue(result.isPresent());
        assertEquals(2048L, result.get().getUserId());
    }
}
