package cn.iocoder.yudao.module.oa.service.collect.unified;

import lombok.Getter;

@Getter
public class UnifiedCollectorApiException extends RuntimeException {

    private final String connStatus;
    private final int businessCode;

    public UnifiedCollectorApiException(String connStatus, String message) {
        this(connStatus, message, 0);
    }

    public UnifiedCollectorApiException(String connStatus, String message, int businessCode) {
        super(message);
        this.connStatus = connStatus;
        this.businessCode = businessCode;
    }
}
