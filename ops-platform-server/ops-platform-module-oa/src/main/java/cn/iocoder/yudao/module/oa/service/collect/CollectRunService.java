package cn.iocoder.yudao.module.oa.service.collect;

import cn.iocoder.yudao.module.oa.dal.dataobject.collect.CollectLogDO;
import cn.iocoder.yudao.module.oa.dal.dataobject.collect.CollectTaskDO;
import cn.iocoder.yudao.module.oa.dal.mysql.collect.CollectLogMapper;
import cn.iocoder.yudao.module.oa.dal.mysql.collect.CollectTaskMapper;
import cn.iocoder.yudao.module.oa.service.config.ConfigTenantSupport;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class CollectRunService {

    private final CollectTaskMapper collectTaskMapper;
    private final CollectLogMapper collectLogMapper;
    private final CollectExecutionService collectExecutionService;
    private final CollectRetryService collectRetryService;

    @Transactional
    public void run(Long taskId) {
        executeAttempt(taskId, 0);
    }

    @Transactional
    public void executeAttempt(Long taskId, int retryCount) {
        Long tenantId = ConfigTenantSupport.requireTenantId();
        CollectTaskDO task = getRequiredInTenant(taskId);

        task.setStatus("RUNNING");
        ConfigTenantSupport.fillUpdate(task);
        collectTaskMapper.updateById(task);

        LocalDateTime start = LocalDateTime.now();
        CollectExecutionResult result = collectExecutionService.execute(task);
        LocalDateTime end = LocalDateTime.now();
        long durationMs = Duration.between(start, end).toMillis();

        CollectLogDO log = new CollectLogDO();
        log.setTaskId(taskId);
        log.setStartAt(start);
        log.setEndAt(end);
        log.setDurationMs(durationMs);
        log.setRetryCount(retryCount);
        fillCreateFromTask(log, task);

        if (result.isSuccess()) {
            log.setStatus("SUCCESS");
            log.setRecordCount(result.getRecordCount());
            collectLogMapper.insert(log);

            task.setStatus("PENDING");
            task.setLastRunAt(start);
            task.setRunCount(Objects.requireNonNullElse(task.getRunCount(), 0) + 1);
            ConfigTenantSupport.fillUpdate(task);
            collectTaskMapper.updateById(task);
            return;
        }

        log.setStatus("FAILED");
        log.setRecordCount(0);
        log.setErrorMessage(result.getErrorMessage());
        collectLogMapper.insert(log);

        ConfigTenantSupport.fillUpdate(task);
        collectTaskMapper.updateById(task);

        scheduleRetryAfterCommit(taskId, tenantId, retryCount, result.getErrorMessage());
    }

    private void scheduleRetryAfterCommit(Long taskId, Long tenantId, int retryCount, String errorMessage) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            collectRetryService.handleFailure(taskId, tenantId, retryCount, errorMessage);
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                collectRetryService.handleFailure(taskId, tenantId, retryCount, errorMessage);
            }
        });
    }

    private CollectTaskDO getRequiredInTenant(Long id) {
        CollectTaskDO entity = collectTaskMapper.selectById(id);
        return ConfigTenantSupport.getRequiredInTenant(entity);
    }

    private void fillCreateFromTask(CollectLogDO log, CollectTaskDO task) {
        log.setTenantId(task.getTenantId());
        log.setCreator(ConfigTenantSupport.currentUsername());
        log.setUpdater(ConfigTenantSupport.currentUsername());
        log.setCreateTime(LocalDateTime.now());
        log.setUpdateTime(LocalDateTime.now());
    }
}
