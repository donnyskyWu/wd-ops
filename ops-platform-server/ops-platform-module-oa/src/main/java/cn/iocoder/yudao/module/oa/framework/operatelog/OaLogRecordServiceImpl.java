package cn.iocoder.yudao.module.oa.framework.operatelog;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.biz.system.logger.OperateLogCommonApi;
import cn.iocoder.yudao.framework.common.biz.system.logger.dto.OperateLogCreateReqDTO;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.oa.dal.dataobject.auth.FootballSystemUserDO;
import cn.iocoder.yudao.module.oa.dal.mysql.auth.FootballOAuth2MasterTokenMapper;
import cn.iocoder.yudao.module.oa.dal.mysql.auth.FootballOAuth2TokenMapper;
import cn.iocoder.yudao.module.oa.framework.auth.LoginUser;
import cn.iocoder.yudao.module.oa.framework.auth.LoginUserContext;
import cn.iocoder.yudao.module.oa.service.auth.FootballOAuth2TokenSnapshot;
import cn.iocoder.yudao.module.oa.service.support.FootballSystemUserValidator;
import com.mzt.logapi.beans.LogRecord;
import com.mzt.logapi.service.ILogRecordService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * OPS {@link ILogRecordService} — mirrors Football {@code LogRecordServiceImpl} (read-only reference).
 * Persists via Feign {@link OperateLogCommonApi} → system-server {@code system_operate_log}.
 */
@Slf4j
@RequiredArgsConstructor
public class OaLogRecordServiceImpl implements ILogRecordService {

    private static final String ROLE_OA_ADMIN = "ROLE_OA_ADMIN";
    /** Football tenant admin username (dev-token oa-admin bridges here for operate-log display). */
    private static final String FOOTBALL_TENANT_ADMIN_USERNAME = "admin";

    private final OperateLogCommonApi operateLogApi;
    private final FootballSystemUserValidator footballSystemUserValidator;
    private final FootballOAuth2MasterTokenMapper footballOAuth2MasterTokenMapper;
    private final FootballOAuth2TokenMapper footballOAuth2TokenMapper;

    @Override
    public void record(LogRecord logRecord) {
        OperateLogCreateReqDTO reqDTO = new OperateLogCreateReqDTO();
        try {
            reqDTO.setTraceId(resolveTraceId());
            fillUserFields(reqDTO);
            fillModuleFields(reqDTO, logRecord);
            fillRequestFields(reqDTO);
            // Sync Feign call: @Async default method runs without TenantContextHolder (AL-05).
            operateLogApi.createOperateLog(reqDTO).checkError();
        } catch (Throwable ex) {
            log.error("[record][url({}) log({}) 发生异常]", reqDTO.getRequestUrl(), reqDTO, ex);
        }
    }

    private static String resolveTraceId() {
        String traceId = UUID.randomUUID().toString().replace("-", "");
        return traceId.length() > 32 ? traceId.substring(0, 32) : traceId;
    }

    private void fillUserFields(OperateLogCreateReqDTO reqDTO) {
        LoginUser loginUser = LoginUserContext.get();
        Long rawUserId = loginUser != null ? loginUser.getUserId() : TenantContextHolder.getUserId();
        String username = loginUser != null ? loginUser.getUsername() : TenantContextHolder.getUsername();
        Long tenantId = loginUser != null ? loginUser.getTenantId() : TenantContextHolder.getTenantId();

        Long footballUserId = resolveFootballOperateLogUserId(loginUser, rawUserId, username, tenantId);
        if (footballUserId == null) {
            log.warn("[fillUserFields][无法解析 Football system_users.id rawUserId={} username={}]", rawUserId, username);
            return;
        }
        reqDTO.setUserId(footballUserId);
        reqDTO.setUserType(FootballOAuth2TokenSnapshot.USER_TYPE_ADMIN);
    }

    /**
     * Football operate-log UI resolves {@code userName} via {@code userId} → {@code system_users.nickname}.
     * DevAuth {@code sys_user.id} (e.g. 1001 / oa-admin) must be mapped to Football {@code system_users.id}.
     */
    Long resolveFootballOperateLogUserId(LoginUser loginUser, Long rawUserId, String username, Long tenantId) {
        if (rawUserId != null && footballSystemUserValidator.findFootballUser(rawUserId) != null) {
            return rawUserId;
        }
        if (rawUserId != null) {
            Long presentable = footballSystemUserValidator.resolvePresentableUserId(rawUserId);
            if (presentable != null && footballSystemUserValidator.findFootballUser(presentable) != null) {
                return presentable;
            }
        }
        if (StrUtil.isNotBlank(username)) {
            FootballSystemUserDO byUsername = lookupFootballUserByUsername(username);
            if (byUsername != null && byUsername.getId() != null) {
                return byUsername.getId();
            }
        }
        if (isOaAdminDevUser(loginUser) && tenantId != null) {
            FootballSystemUserDO tenantAdmin = lookupFootballUserByUsername(FOOTBALL_TENANT_ADMIN_USERNAME);
            if (tenantAdmin != null && tenantAdmin.getId() != null
                    && Objects.equals(tenantAdmin.getTenantId(), tenantId)) {
                return tenantAdmin.getId();
            }
        }
        return rawUserId;
    }

    /** Prefer shenyu-system id (operate-log + @Trans target) over wd master overlay. */
    private FootballSystemUserDO lookupFootballUserByUsername(String username) {
        try {
            FootballSystemUserDO systemUser = footballOAuth2TokenMapper.selectUserByUsername(username);
            if (systemUser != null) {
                return systemUser;
            }
        } catch (Exception ignored) {
            // H2 / no shenyu-system overlay
        }
        try {
            return footballOAuth2MasterTokenMapper.selectUserByUsername(username);
        } catch (Exception ignored) {
            return null;
        }
    }

    private static boolean isOaAdminDevUser(LoginUser loginUser) {
        return loginUser != null && loginUser.getAuthorities() != null
                && loginUser.getAuthorities().contains(ROLE_OA_ADMIN);
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
