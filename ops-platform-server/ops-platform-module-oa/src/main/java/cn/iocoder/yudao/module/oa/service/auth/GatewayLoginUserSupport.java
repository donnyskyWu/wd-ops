package cn.iocoder.yudao.module.oa.service.auth;

import cn.hutool.core.util.StrUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;

/**
 * Parses Gateway {@code login-user} header (URL-encoded JSON).
 */
@Slf4j
public final class GatewayLoginUserSupport {

    public static final String LOGIN_USER_HEADER = "login-user";

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private GatewayLoginUserSupport() {
    }

    public static GatewayLoginUserDTO parse(HttpServletRequest request) {
        if (request == null) {
            return null;
        }
        String header = request.getHeader(LOGIN_USER_HEADER);
        if (StrUtil.isBlank(header)) {
            return null;
        }
        try {
            String decoded = URLDecoder.decode(header, StandardCharsets.UTF_8);
            GatewayLoginUserDTO user = OBJECT_MAPPER.readValue(decoded, GatewayLoginUserDTO.class);
            if (user == null || user.getId() == null) {
                return null;
            }
            if (user.getExpiresTime() != null && user.getExpiresTime().isBefore(LocalDateTime.now())) {
                return null;
            }
            return user;
        } catch (Exception ex) {
            log.warn("[parse][login-user 解析失败 header={}]", header, ex);
            return null;
        }
    }
}
