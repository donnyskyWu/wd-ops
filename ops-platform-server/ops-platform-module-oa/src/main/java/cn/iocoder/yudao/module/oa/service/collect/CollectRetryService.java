package cn.iocoder.yudao.module.oa.service.collect;

import cn.iocoder.yudao.module.oa.config.CollectProperties;
import cn.iocoder.yudao.module.oa.dal.dataobject.collect.CollectTaskDO;
import cn.iocoder.yudao.module.oa.dal.mysql.collect.CollectTaskMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

@Slf4j
@Service
public class CollectRetryService {

    private final CollectProperties collectProperties;
    private final CollectTaskMapper collectTaskMapper;
    private final CollectFailureAlertService collectFailureAlertService;
    private final CollectRetryScheduler collectRetryScheduler;

    public CollectRetryService(CollectProperties collectProperties,
                               CollectTaskMapper collectTaskMapper,
                               CollectFailureAlertService collectFailureAlertService,
                               CollectRetryScheduler collectRetryScheduler) {
        this.collectProperties = collectProperties;
        this.collectTaskMapper = collectTaskMapper;
        this.collectFailureAlertService = collectFailureAlertService;
        this.collectRetryScheduler = collectRetryScheduler;
    }

    public void handleFailure(Long taskId, Long tenantId, int retryCount, String errorMessage) {
        int maxAttempts = collectProperties.getRetry().getMaxAttempts();
        if (retryCount < maxAttempts) {
            long delayMs = backoffDelayMs(retryCount);
            collectRetryScheduler.scheduleRetry(taskId, tenantId, retryCount + 1, delayMs);
            log.info("Collect task {} scheduled retry {}/{} in {}ms",
                    taskId, retryCount + 1, maxAttempts, delayMs);
            return;
        }
        markTaskFailed(taskId, tenantId);
        CollectTaskDO task = collectTaskMapper.selectById(taskId);
        if (task != null && tenantId.equals(task.getTenantId())) {
            collectFailureAlertService.alertFailure(task, errorMessage);
        }
    }

    private long backoffDelayMs(int retryIndex) {
        List<Integer> seconds = collectProperties.getRetry().getBackoffSeconds();
        if (seconds == null || seconds.isEmpty()) {
            return 0L;
        }
        int index = Math.min(retryIndex, seconds.size() - 1);
        return seconds.get(index) * 1000L;
    }

    private void markTaskFailed(Long taskId, Long tenantId) {
        CollectTaskDO task = collectTaskMapper.selectById(taskId);
        if (task == null || !tenantId.equals(task.getTenantId())) {
            return;
        }
        task.setStatus("FAILED");
        task.setFailCount(Objects.requireNonNullElse(task.getFailCount(), 0) + 1);
        task.setUpdater("system");
        task.setUpdateTime(LocalDateTime.now());
        collectTaskMapper.updateById(task);
        log.warn("Collect task {} exhausted retries, status=FAILED", taskId);
    }
}
