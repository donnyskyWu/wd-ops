package cn.iocoder.yudao.module.oa.framework.operatelog;

import cn.iocoder.yudao.framework.common.biz.system.logger.OperateLogCommonApi;
import cn.iocoder.yudao.framework.common.biz.system.logger.dto.OperateLogCreateReqDTO;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.oa.framework.auth.LoginUser;
import cn.iocoder.yudao.module.oa.framework.auth.LoginUserContext;
import cn.iocoder.yudao.module.oa.service.auth.FootballOAuth2TokenSnapshot;
import com.mzt.logapi.beans.LogRecord;
import com.mzt.logapi.service.ILogRecordService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.List;
import java.util.UUID;

/**
 * OPS {@link ILogRecordService} — mirrors Football {@code LogRecordServiceImpl} (read-only reference).
 * Persists via Feign {@link OperateLogCommonApi} → system-server {@code system_operate_log}.
 */
@Slf4j
@RequiredArgsConstructor
public class OaLogRecordServiceImpl implements ILogRecordService {

    private final OperateLogCommonApi operateLogApi;

    @Override
    public void record(LogRecord logRecord) {
        OperateLogCreateReqDTO reqDTO = new OperateLogCreateReqDTO();
        try {
            reqDTO.setTraceId(resolveTraceId());
            fillUserFields(reqDTO);
            fillModuleFields(reqDTO, logRecord);
            fillRequestFields(reqDTO);
            operateLogApi.createOperateLogAsync(reqDTO);
        } catch (Throwable ex) {
            log.error("[record][url({}) log({}) 发生异常]", reqDTO.getRequestUrl(), reqDTO, ex);
        }
    }

    private static String resolveTraceId() {
        String traceId = UUID.randomUUID().toString().replace("-", "");
        return traceId.length() > 32 ? traceId.substring(0, 32) : traceId;
    }

    private static void fillUserFields(OperateLogCreateReqDTO reqDTO) {
        LoginUser loginUser = LoginUserContext.get();
        if (loginUser != null && loginUser.getUserId() != null) {
            reqDTO.setUserId(loginUser.getUserId());
            reqDTO.setUserType(FootballOAuth2TokenSnapshot.USER_TYPE_ADMIN);
            return;
        }
        Long userId = TenantContextHolder.getUserId();
        if (userId != null) {
            reqDTO.setUserId(userId);
            reqDTO.setUserType(FootballOAuth2TokenSnapshot.USER_TYPE_ADMIN);
        }
    }

    static void fillModuleFields(OperateLogCreateReqDTO reqDTO, LogRecord logRecord) {
        reqDTO.setType(logRecord.getType());
        reqDTO.setSubType(logRecord.getSubType());
        reqDTO.setBizId(Long.parseLong(logRecord.getBizNo()));
        reqDTO.setAction(logRecord.getAction());
        reqDTO.setExtra(logRecord.getExtra());
    }

    private static void fillRequestFields(OperateLogCreateReqDTO reqDTO) {
        HttpServletRequest request = currentRequest();
        if (request == null) {
            return;
        }
        reqDTO.setRequestMethod(request.getMethod());
        reqDTO.setRequestUrl(request.getRequestURI());
        reqDTO.setUserIp(resolveClientIp(request));
        reqDTO.setUserAgent(resolveUserAgent(request));
    }

    private static HttpServletRequest currentRequest() {
        if (!(RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attributes)) {
            return null;
        }
        return attributes.getRequest();
    }

    private static String resolveClientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        String realIp = request.getHeader("X-Real-IP");
        if (realIp != null && !realIp.isBlank()) {
            return realIp.trim();
        }
        return request.getRemoteAddr();
    }

    private static String resolveUserAgent(HttpServletRequest request) {
        String userAgent = request.getHeader("User-Agent");
        return userAgent != null ? userAgent : "";
    }

    @Override
    public List<LogRecord> queryLog(String bizNo, String type) {
        throw new UnsupportedOperationException("使用 OperateLogCommonApi 进行操作日志的查询");
    }

    @Override
    public List<LogRecord> queryLogByBizNo(String bizNo, String type, String subType) {
        throw new UnsupportedOperationException("使用 OperateLogCommonApi 进行操作日志的查询");
    }
}
