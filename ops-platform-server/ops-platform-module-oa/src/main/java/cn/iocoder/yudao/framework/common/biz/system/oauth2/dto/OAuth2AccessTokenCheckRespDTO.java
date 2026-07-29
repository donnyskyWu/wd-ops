package cn.iocoder.yudao.framework.common.biz.system.oauth2.dto;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Vendored from Football {@code OAuth2AccessTokenCheckRespDTO} (D-SYS-03 check path).
 */
@Data
public class OAuth2AccessTokenCheckRespDTO implements Serializable {

    @JsonSerialize(using = ToStringSerializer.class)
    private Long userId;

    private Integer userType;

    private Map<String, String> userInfo;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long tenantId;

    private List<String> scopes;

    private LocalDateTime expiresTime;
}
