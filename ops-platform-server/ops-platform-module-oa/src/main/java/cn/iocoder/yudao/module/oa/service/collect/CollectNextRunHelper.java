package cn.iocoder.yudao.module.oa.service.collect;

import org.springframework.scheduling.support.CronExpression;

import java.time.LocalDateTime;

/**
 * 采集任务 cron 下次执行时间计算（M10-COL-S-02）。
 */
public final class CollectNextRunHelper {

    private CollectNextRunHelper() {
    }

    public static LocalDateTime computeNextRun(String cron, LocalDateTime from) {
        CronExpression expression = CronExpression.parse(cron);
        LocalDateTime next = expression.next(from);
        if (next == null) {
            throw new IllegalArgumentException("cron 表达式无法计算下次执行时间: " + cron);
        }
        return next;
    }
}
