package cn.iocoder.yudao.module.oa.service.collect;

import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.oa.config.CollectProperties;
import cn.iocoder.yudao.module.oa.dal.dataobject.auth.SysTenantDO;
import cn.iocoder.yudao.module.oa.dal.dataobject.collect.CollectTaskDO;
import cn.iocoder.yudao.module.oa.dal.mysql.auth.SysTenantMapper;
import cn.iocoder.yudao.module.oa.dal.mysql.collect.CollectTaskMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 采集任务 cron 调度扫描（M10-COL-S-02 · Spring {@code @Scheduled}）。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CollectCronScheduler {

    private static final String STATUS_PENDING = "PENDING";

    private final CollectProperties collectProperties;
    private final SysTenantMapper sysTenantMapper;
    private final CollectTaskMapper collectTaskMapper;
    private final CollectRunService collectRunService;

    @Scheduled(cron = "${oa.collect.schedule.scan-cron:0 * * * * ?}")
    public void scanDueTasks() {
        if (!collectProperties.getSchedule().isEnabled()) {
            return;
        }
        List<SysTenantDO> tenants = sysTenantMapper.selectList(new LambdaQueryWrapper<SysTenantDO>()
                .in(SysTenantDO::getStatus, List.of("NORMAL", "TRIAL", "ENABLED")));
        for (SysTenantDO tenant : tenants) {
            try {
                scanTenantDueTasks(tenant.getId());
            } catch (Exception ex) {
                log.warn("Collect cron scan failed for tenant {}: {}", tenant.getId(), ex.getMessage());
            }
        }
    }

    public void scanTenantDueTasks(Long tenantId) {
        TenantContextHolder.setTenantId(tenantId);
        TenantContextHolder.setUsername("system");
        try {
            LocalDateTime now = LocalDateTime.now();
            List<CollectTaskDO> dueTasks = collectTaskMapper.selectList(new LambdaQueryWrapper<CollectTaskDO>()
                    .eq(CollectTaskDO::getTenantId, tenantId)
                    .eq(CollectTaskDO::getStatus, STATUS_PENDING)
                    .and(wrapper -> wrapper.isNull(CollectTaskDO::getNextRunAt)
                            .or()
                            .le(CollectTaskDO::getNextRunAt, now)));
            for (CollectTaskDO task : dueTasks) {
                try {
                    collectRunService.run(task.getId());
                } catch (Exception ex) {
                    log.warn("Collect cron run failed for task {}: {}", task.getId(), ex.getMessage());
                }
            }
        } finally {
            TenantContextHolder.clear();
        }
    }
}
