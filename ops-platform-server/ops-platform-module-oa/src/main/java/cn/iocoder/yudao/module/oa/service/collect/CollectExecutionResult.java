package cn.iocoder.yudao.module.oa.service.collect;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class CollectExecutionResult {

    private final boolean success;
    private final int recordCount;
    private final String errorMessage;
    private final List<TypeOutcome> typeOutcomes;

    public static CollectExecutionResult success(int recordCount) {
        return new CollectExecutionResult(true, recordCount, null, null);
    }

    public static CollectExecutionResult failure(String errorMessage) {
        return new CollectExecutionResult(false, 0, errorMessage, null);
    }

    public static CollectExecutionResult aggregate(int totalCount, List<TypeOutcome> outcomes) {
        List<TypeOutcome> safeOutcomes = outcomes == null ? List.of() : List.copyOf(outcomes);
        boolean allSuccess = safeOutcomes.stream().allMatch(TypeOutcome::isSuccess);
        if (allSuccess) {
            return new CollectExecutionResult(true, totalCount, null, safeOutcomes);
        }
        String errors = safeOutcomes.stream()
                .filter(outcome -> !outcome.isSuccess())
                .map(outcome -> outcome.getDataType() + ": " + outcome.getErrorMessage())
                .collect(Collectors.joining("; "));
        return new CollectExecutionResult(false, totalCount, "部分采集失败: " + errors, safeOutcomes);
    }

    public boolean isMultiType() {
        return typeOutcomes != null && typeOutcomes.size() > 1;
    }

    @Getter
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    public static class TypeOutcome {
        private final String dataType;
        private final boolean success;
        private final int recordCount;
        private final String errorMessage;

        public static TypeOutcome of(String dataType, CollectExecutionResult result) {
            String label = dataType == null ? CollectPlatformDefaults.SENTINEL_FOLLOWER_STATS : dataType;
            if (result.isSuccess()) {
                return new TypeOutcome(label, true, result.getRecordCount(), null);
            }
            return new TypeOutcome(label, false, 0, result.getErrorMessage());
        }
    }

    public static List<TypeOutcome> emptyOutcomes() {
        return Collections.unmodifiableList(new ArrayList<>());
    }
}
