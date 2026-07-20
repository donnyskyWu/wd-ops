package cn.iocoder.yudao.framework.common.biz.system.logger.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * Football system-server RPC create DTO (vendored from yudao-common, AL-05).
 */
@Data
public class OperateLogCreateReqDTO {

    private String traceId;

    @NotNull(message = "用户编号不能为空")
    private Long userId;

    @NotNull(message = "用户类型不能为空")
    private Integer userType;

    @NotEmpty(message = "操作模块类型不能为空")
    private String type;

    @NotEmpty(message = "操作名不能为空")
    private String subType;

    @NotNull(message = "操作模块业务编号不能为空")
    private Long bizId;

    @NotEmpty(message = "操作内容不能为空")
    private String action;

    private String extra;

    @NotEmpty(message = "请求方法名不能为空")
    private String requestMethod;

    @NotEmpty(message = "请求地址不能为空")
    private String requestUrl;

    @NotEmpty(message = "用户 IP 不能为空")
    private String userIp;

    @NotEmpty(message = "浏览器 UA 不能为空")
    private String userAgent;
}
