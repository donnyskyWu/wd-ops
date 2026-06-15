package cn.iocoder.yudao.module.oa.config;

import cn.iocoder.yudao.framework.common.exception.OaErrorCodes;
import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

@RestControllerAdvice(basePackages = "cn.iocoder.yudao.module.oa")
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(AccessDeniedException.class)
    public CommonResult<?> handleAccessDeniedException(AccessDeniedException ex, HttpServletRequest request) {
        log.warn("[OA] access denied uri={} msg={}", request.getRequestURI(), ex.getMessage());
        return CommonResult.error(OaErrorCodes.FORBIDDEN.getCode(), OaErrorCodes.FORBIDDEN.getMsg());
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public CommonResult<?> handleMaxUploadSizeExceeded(MaxUploadSizeExceededException ex, HttpServletRequest request) {
        log.warn("[OA] upload too large uri={} msg={}", request.getRequestURI(), ex.getMessage());
        return CommonResult.error(OaErrorCodes.BAD_REQUEST.getCode(), "图片大小不能超过 5MB");
    }

    @ExceptionHandler(ServiceException.class)
    public CommonResult<?> handleServiceException(ServiceException ex, HttpServletRequest request) {
        log.warn("[OA] service error uri={} code={} msg={}", request.getRequestURI(), ex.getCode(), ex.getMessage());
        return CommonResult.error(ex.getCode(), ex.getMessage());
    }

    @ExceptionHandler(DataAccessException.class)
    public CommonResult<?> handleDataAccessException(DataAccessException ex, HttpServletRequest request) {
        log.error("[OA] data access uri={}", request.getRequestURI(), ex);
        String hint = ex.getMessage() != null && ex.getMessage().contains("Unknown column")
                ? "数据库结构未更新，请重启后端以执行 Flyway 迁移"
                : "数据保存失败，请检查内容长度或联系管理员";
        return CommonResult.error(500, hint);
    }

    @ExceptionHandler({MethodArgumentNotValidException.class, BindException.class, ConstraintViolationException.class})
    public CommonResult<?> handleValidationException(Exception ex) {
        String message;
        if (ex instanceof MethodArgumentNotValidException manve) {
            message = manve.getBindingResult().getFieldErrors().stream()
                    .map(this::formatFieldError)
                    .collect(Collectors.joining("; "));
        } else if (ex instanceof BindException be) {
            message = be.getFieldErrors().stream()
                    .map(this::formatFieldError)
                    .collect(Collectors.joining("; "));
        } else {
            ConstraintViolationException cve = (ConstraintViolationException) ex;
            message = cve.getConstraintViolations().stream()
                    .map(ConstraintViolation::getMessage)
                    .collect(Collectors.joining("; "));
        }
        return CommonResult.error(OaErrorCodes.DICT_VALUE_INVALID.getCode(), message);
    }

    @ExceptionHandler(Exception.class)
    public CommonResult<?> handleUnknownException(Exception ex, HttpServletRequest request) {
        log.error("[OA] unhandled uri={}", request.getRequestURI(), ex);
        return CommonResult.error(500, "服务器内部错误");
    }

    private String formatFieldError(FieldError fieldError) {
        return fieldError.getField() + ": " + fieldError.getDefaultMessage();
    }
}
