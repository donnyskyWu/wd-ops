package cn.iocoder.yudao.module.oa.service.collect;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.function.ToIntFunction;

/**
 * 隔离采集执行事务，避免失败时污染外层 run 事务。
 */
@Component
public class CollectExecutionInvoker {

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public int invoke(ToIntFunction<Long> collector, Long accountId) {
        return collector.applyAsInt(accountId);
    }
}
