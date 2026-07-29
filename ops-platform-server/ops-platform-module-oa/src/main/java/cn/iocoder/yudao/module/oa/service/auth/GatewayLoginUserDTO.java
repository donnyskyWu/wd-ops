package cn.iocoder.yudao.module.oa.service.auth;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Vendored subset of Football Gateway {@code LoginUser} (login-user header JSON).
 */
@Data
public class GatewayLoginUserDTO {

    private Long id;
    private Integer userType;
    private Map<String, String> info;
    private Long tenantId;
    private List<String> scopes;
    private LocalDateTime expiresTime;
}
