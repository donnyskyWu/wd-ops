package cn.iocoder.yudao.module.oa.framework.audit;

import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

@Aspect
@Component
@Slf4j
public class AuditLogAspect {

    @AfterReturning("@annotation(auditLog)")
    public void afterSuccess(JoinPoint joinPoint, AuditLog auditLog) {
        log.info("[AUDIT] tenant={} user={} module={} action={} method={}",
                TenantContextHolder.getTenantId(),
                TenantContextHolder.getUsername(),
                auditLog.module(),
                auditLog.action(),
                joinPoint.getSignature().toShortString());
    }
}
