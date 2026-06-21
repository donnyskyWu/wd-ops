package cn.iocoder.yudao.module.oa.service.collect;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class CollectExecutionResult {

    private final boolean success;
    private final int recordCount;
    private final String errorMessage;

    public static CollectExecutionResult success(int recordCount) {
        return new CollectExecutionResult(true, recordCount, null);
    }

    public static CollectExecutionResult failure(String errorMessage) {
        return new CollectExecutionResult(false, 0, errorMessage);
    }
}
