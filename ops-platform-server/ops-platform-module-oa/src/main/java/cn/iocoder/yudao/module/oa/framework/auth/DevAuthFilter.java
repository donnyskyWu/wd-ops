package cn.iocoder.yudao.module.oa.framework.auth;

import cn.iocoder.yudao.framework.common.exception.OaErrorCodes;
import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class DevAuthFilter extends OncePerRequestFilter {

    public static final String HEADER_TENANT_ID = "X-Tenant-Id";

    private final AuthProvider authProvider;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String uri = request.getRequestURI();
        return uri.startsWith("/actuator")
                || uri.startsWith("/swagger")
                || uri.startsWith("/v3/api-docs");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        try {
            authenticate(request);
            filterChain.doFilter(request, response);
        } catch (ServiceException ex) {
            writeError(response, ex.getCode(), ex.getMessage());
        } finally {
            LoginUserContext.clear();
            TenantContextHolder.clear();
            SecurityContextHolder.clearContext();
        }
    }

    private void authenticate(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        String token;
        Long tenantId;
        if (authHeader != null && !authHeader.isBlank()) {
            token = authHeader.startsWith("Bearer ") ? authHeader.substring(7).trim() : authHeader.trim();
            tenantId = parseTenantId(request.getHeader(HEADER_TENANT_ID));
        } else if (isFileAccessRequest(request)) {
            token = request.getParameter("token");
            if (token == null || token.isBlank()) {
                throw new ServiceException(OaErrorCodes.UNAUTHORIZED);
            }
            tenantId = parseTenantId(request.getParameter("tenantId"));
        } else {
            throw new ServiceException(OaErrorCodes.UNAUTHORIZED);
        }

        LoginUser loginUser = authProvider.authenticate(request, token, tenantId)
                .orElseThrow(() -> new ServiceException(OaErrorCodes.UNAUTHORIZED));

        LoginUserContext.set(loginUser);
        TenantContextHolder.setTenantId(loginUser.getTenantId());
        TenantContextHolder.setUserId(loginUser.getUserId());
        TenantContextHolder.setUsername(loginUser.getUsername());

        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                loginUser.getUsername(),
                null,
                loginUser.getAuthorities().stream()
                        .map(SimpleGrantedAuthority::new)
                        .collect(Collectors.toList()));
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    private boolean isFileAccessRequest(HttpServletRequest request) {
        if (!"GET".equalsIgnoreCase(request.getMethod())) {
            return false;
        }
        String uri = request.getRequestURI();
        return uri.contains("/oa/file/view") || uri.contains("/oa/file/download");
    }

    private Long parseTenantId(String tenantHeader) {
        if (tenantHeader == null || tenantHeader.isBlank()) {
            throw new ServiceException(OaErrorCodes.UNAUTHORIZED.getCode(), "缺少 X-Tenant-Id");
        }
        try {
            return Long.valueOf(tenantHeader.trim());
        } catch (NumberFormatException ex) {
            throw new ServiceException(OaErrorCodes.UNAUTHORIZED.getCode(), "X-Tenant-Id 格式错误");
        }
    }

    private void writeError(HttpServletResponse response, int code, String message) throws IOException {
        response.setStatus(code == 401 ? 401 : 200);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write("{\"code\":" + code + ",\"msg\":\"" + message + "\",\"data\":null}");
    }
}
