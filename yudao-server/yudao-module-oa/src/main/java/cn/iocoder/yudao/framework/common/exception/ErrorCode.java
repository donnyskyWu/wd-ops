package cn.iocoder.yudao.framework.common.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ErrorCode {

    private final Integer code;
    private final String msg;

    public static ErrorCode of(int code, String msg) {
        return new ErrorCode(code, msg);
    }
}
