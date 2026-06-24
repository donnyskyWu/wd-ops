package cn.iocoder.yudao.module.oa.service.collect.display;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.oa.api.dto.operations.ContentAnalysisVO;
import cn.iocoder.yudao.module.oa.api.dto.operations.ContentStatsVO;
import cn.iocoder.yudao.module.oa.api.dto.operations.InternalContentVO;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class CollectedDataMergeSupport {

    private CollectedDataMergeSupport() {
    }

    public static PageResult<ContentAnalysisVO> mergeAndPage(List<ContentAnalysisVO> legacy,
                                                               List<ContentAnalysisVO> collected,
                                                               Integer pageNo, Integer pageSize) {
        List<ContentAnalysisVO> merged = new ArrayList<>();
        if (legacy != null) {
            merged.addAll(legacy);
        }
        if (collected != null) {
            merged.addAll(collected);
        }
        merged.sort(Comparator.comparing(ContentAnalysisVO::getPublishTime,
                Comparator.nullsLast(Comparator.reverseOrder())));
        return page(merged, pageNo, pageSize);
    }

    public static PageResult<InternalContentVO> mergeInternalAndPage(List<InternalContentVO> legacy,
                                                                   List<InternalContentVO> collected,
                                                                   Integer pageNo, Integer pageSize) {
        List<InternalContentVO> merged = new ArrayList<>();
        if (legacy != null) {
            merged.addAll(legacy);
        }
        if (collected != null) {
            merged.addAll(collected);
        }
        merged.sort(Comparator.comparing(InternalContentVO::getPublishTime,
                Comparator.nullsLast(Comparator.reverseOrder())));
        return page(merged, pageNo, pageSize);
    }

    public static ContentStatsVO mergeStats(ContentStatsVO legacy, ContentStatsVO collected) {
        ContentStatsVO vo = new ContentStatsVO();
        int totalCount = safeInt(legacy.getTotalCount()) + safeInt(collected.getTotalCount());
        vo.setTotalCount(totalCount);
        vo.setHitCount(safeInt(legacy.getHitCount()) + safeInt(collected.getHitCount()));
        long totalRead = safeLong(legacy.getTotalRead()) + safeLong(collected.getTotalRead());
        vo.setTotalRead(totalRead);
        vo.setAvgRead(totalCount == 0 ? 0L : totalRead / totalCount);
        vo.setTotalLikes(safeLong(legacy.getTotalLikes()) + safeLong(collected.getTotalLikes()));
        vo.setTotalComments(safeLong(legacy.getTotalComments()) + safeLong(collected.getTotalComments()));
        vo.setTotalShares(safeLong(legacy.getTotalShares()) + safeLong(collected.getTotalShares()));
        return vo;
    }

    public static <T> PageResult<T> page(List<T> rows, Integer pageNo, Integer pageSize) {
        List<T> safeRows = rows == null ? List.of() : rows;
        int page = pageNo == null || pageNo < 1 ? 1 : pageNo;
        int size = pageSize == null || pageSize < 1 ? 20 : pageSize;
        int from = Math.min((page - 1) * size, safeRows.size());
        int to = Math.min(from + size, safeRows.size());
        return new PageResult<>(safeRows.subList(from, to), (long) safeRows.size());
    }

    private static int safeInt(Integer value) {
        return value == null ? 0 : value;
    }

    private static long safeLong(Long value) {
        return value == null ? 0L : value;
    }
}
