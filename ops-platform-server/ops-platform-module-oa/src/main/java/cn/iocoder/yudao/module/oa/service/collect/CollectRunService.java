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

    private static final String STATUS_RUNNING = "RUNNING";

    private final CollectTaskMapper collectTaskMapper;
    private final CollectLogMapper collectLogMapper;
    private final CollectExecutionService collectExecutionService;
    private final CollectRetryService collectRetryService;
    private final CollectLogResultBuilder collectLogResultBuilder;

    @Transactional
    public void run(Long taskId) {
        executeAttempt(taskId, 0);
    }

    @Transactional
    public void executeAttempt(Long taskId, int retryCount) {
        Long tenantId = ConfigTenantSupport.requireTenantId();
        CollectTaskDO task = getRequiredInTenant(taskId);
        String operationalStatus = task.getStatus();

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

        boolean hasTypeOutcomes = result.getTypeOutcomes() != null && !result.getTypeOutcomes().isEmpty();
        if (result.isSuccess() || hasTypeOutcomes) {
            log.setStatus(result.isSuccess() ? "SUCCESS" : "PARTIAL");
            log.setRecordCount(result.getRecordCount());
            log.setResultJson(collectLogResultBuilder.build(task, result));
            if (!result.isSuccess()) {
                log.setErrorMessage(result.getErrorMessage());
            }
            collectLogMapper.insert(log);

            applyPostRunTaskState(task, operationalStatus, start, end, result.isSuccess());
            ConfigTenantSupport.fillUpdate(task);
            collectTaskMapper.updateById(task);
            if (result.isSuccess()) {
                return;
            }
            scheduleRetryAfterCommit(taskId, tenantId, retryCount, result.getErrorMessage());
            return;
        }

        log.setStatus("FAILED");
        log.setRecordCount(0);
        log.setErrorMessage(result.getErrorMessage());
        collectLogMapper.insert(log);

        applyPostRunTaskState(task, operationalStatus, start, end, false);
        ConfigTenantSupport.fillUpdate(task);
        collectTaskMapper.updateById(task);

        scheduleRetryAfterCommit(taskId, tenantId, retryCount, result.getErrorMessage());
    }

    private void applyPostRunTaskState(CollectTaskDO task, String operationalStatus,
                                       LocalDateTime start, LocalDateTime end, boolean success) {
        task.setLastRunAt(start);
        if (STATUS_RUNNING.equals(operationalStatus)) {
            task.setStatus(STATUS_RUNNING);
            if (success) {
                task.setNextRunAt(CollectNextRunHelper.computeNextRun(task.getCron(), end));
                task.setRunCount(Objects.requireNonNullElse(task.getRunCount(), 0) + 1);
            }
            return;
        }
        task.setStatus(operationalStatus);
        if (success) {
            task.setRunCount(Objects.requireNonNullElse(task.getRunCount(), 0) + 1);
        }
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
