package cn.iocoder.yudao.framework.common.pojo;

import cn.iocoder.yudao.framework.common.exception.ErrorCode;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;

import java.io.Serializable;

@Data
public class CommonResult<T> implements Serializable {

    public static final int CODE_SUCCESS = 0;

    private Integer code;
    private String msg;
    private T data;

    public static <T> CommonResult<T> success(T data) {
        CommonResult<T> result = new CommonResult<>();
        result.code = CODE_SUCCESS;
        result.msg = "ok";
        result.data = data;
        return result;
    }

    public static <T> CommonResult<T> error(int code, String msg) {
        CommonResult<T> result = new CommonResult<>();
        result.code = code;
        result.msg = msg;
        return result;
    }

    public static <T> CommonResult<T> error(ErrorCode errorCode) {
        return error(errorCode.getCode(), errorCode.getMsg());
    }

    @JsonIgnore
    public boolean isSuccess() {
        return code != null && code == CODE_SUCCESS;
    }
}
