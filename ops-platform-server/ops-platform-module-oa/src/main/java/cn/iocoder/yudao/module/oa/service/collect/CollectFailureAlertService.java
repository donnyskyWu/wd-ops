package cn.iocoder.yudao.module.oa.service.collect;

import cn.iocoder.yudao.module.oa.dal.dataobject.collect.CollectTaskDO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 采集失败告警（STATE-M10 §1.3：钉钉通知；本期 stub 仅打日志，后续接 NotificationService）。
 */
@Slf4j
@Service
public class CollectFailureAlertService {

    public void alertFailure(CollectTaskDO task, String errorMessage) {
        log.warn("[M10-COL] DingTalk alert stub: taskId={}, taskName={}, tenantId={}, error={}",
                task.getId(), task.getTaskName(), task.getTenantId(), errorMessage);
    }
}
