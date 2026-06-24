package cn.iocoder.yudao.module.oa.service.collect.wework;

import lombok.Getter;

@Getter
public class WeComApiException extends RuntimeException {

    private final int errcode;

    public WeComApiException(int errcode, String message) {
        super(message);
        this.errcode = errcode;
    }

    public WeComApiException(String message) {
        this(2022, message);
    }
}
