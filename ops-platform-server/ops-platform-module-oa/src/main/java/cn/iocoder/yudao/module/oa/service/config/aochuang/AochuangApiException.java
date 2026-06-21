package cn.iocoder.yudao.module.oa.service.config.aochuang;

import lombok.Getter;

@Getter
public class AochuangApiException extends RuntimeException {

    private final String connStatus;

    public AochuangApiException(String connStatus, String message) {
        super(message);
        this.connStatus = connStatus;
    }
}
