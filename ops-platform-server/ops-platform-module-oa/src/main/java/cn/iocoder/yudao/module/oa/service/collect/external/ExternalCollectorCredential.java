package cn.iocoder.yudao.module.oa.service.collect.external;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 解析后的租户/环境级 collector 凭账号（ADR-052 §3.4）。
 */
@Getter
@RequiredArgsConstructor
public class ExternalCollectorCredential {

    private final String cookie;
    private final String authToken;

    public boolean hasCookie() {
        return cookie != null && !cookie.isBlank();
    }
}
