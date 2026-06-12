package cn.iocoder.yudao.module.oa.service.sop;

import lombok.Data;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class DagValidator {

    private DagValidator() {
    }

    public static DagValidateResult validate(Map<Long, List<Long>> graph) {
        Set<Long> visiting = new HashSet<>();
        Set<Long> visited = new HashSet<>();
        List<Long> cyclePath = new ArrayList<>();

        for (Long nodeId : graph.keySet()) {
            if (!visited.contains(nodeId)) {
                List<Long> path = new ArrayList<>();
                if (dfs(nodeId, graph, visiting, visited, path, cyclePath)) {
                    return DagValidateResult.invalid(cyclePath);
                }
            }
        }
        return DagValidateResult.valid();
    }

    private static boolean dfs(Long nodeId, Map<Long, List<Long>> graph,
                               Set<Long> visiting, Set<Long> visited,
                               List<Long> path, List<Long> cyclePath) {
        if (visiting.contains(nodeId)) {
            int idx = path.indexOf(nodeId);
            cyclePath.addAll(path.subList(idx, path.size()));
            cyclePath.add(nodeId);
            return true;
        }
        if (visited.contains(nodeId)) {
            return false;
        }
        visiting.add(nodeId);
        path.add(nodeId);
        List<Long> preds = graph.getOrDefault(nodeId, List.of());
        for (Long pred : preds) {
            if (dfs(pred, graph, visiting, visited, path, cyclePath)) {
                return true;
            }
        }
        visiting.remove(nodeId);
        path.remove(path.size() - 1);
        visited.add(nodeId);
        return false;
    }

    @Data
    public static class DagValidateResult {
        private final boolean valid;
        private final List<Long> cyclePath;

        private DagValidateResult(boolean valid, List<Long> cyclePath) {
            this.valid = valid;
            this.cyclePath = cyclePath;
        }

        public static DagValidateResult valid() {
            return new DagValidateResult(true, List.of());
        }

        public static DagValidateResult invalid(List<Long> cyclePath) {
            return new DagValidateResult(false, cyclePath);
        }
    }
}
