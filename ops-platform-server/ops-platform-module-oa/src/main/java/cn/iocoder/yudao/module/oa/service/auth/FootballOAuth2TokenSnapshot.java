package cn.iocoder.yudao.module.oa.service.auth;

import lombok.Data;

import java.time.LocalDateTime;

/** Subset of Football {@code OAuth2AccessTokenDO} JSON stored in Redis. */
@Data
public class FootballOAuth2TokenSnapshot {

    /** Football {@code UserTypeEnum.ADMIN} = 2 */
    public static final int USER_TYPE_ADMIN = 2;

    private Long userId;
    private Long tenantId;
    private Integer userType;
    private LocalDateTime expiresTime;

    public boolean isExpired() {
        return expiresTime != null && expiresTime.isBefore(LocalDateTime.now());
    }
}
