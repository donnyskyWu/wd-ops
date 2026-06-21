package cn.iocoder.yudao.module.oa.service.collect;

import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class CollectRetryScheduler {

    private final CollectRunService collectRunService;

    public CollectRetryScheduler(@Lazy CollectRunService collectRunService) {
        this.collectRunService = collectRunService;
    }

    @Async("collectRetryExecutor")
    public void scheduleRetry(Long taskId, Long tenantId, int nextRetryCount, long delayMs) {
        try {
            if (delayMs > 0) {
                Thread.sleep(delayMs);
            }
            TenantContextHolder.setTenantId(tenantId);
            TenantContextHolder.setUsername("system");
            collectRunService.executeAttempt(taskId, nextRetryCount);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("Collect retry interrupted for task {}", taskId);
        } finally {
            TenantContextHolder.clear();
        }
    }
}
