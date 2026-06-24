package cn.iocoder.yudao.module.oa.service.collect.display;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.oa.api.dto.operations.ContentAnalysisVO;
import cn.iocoder.yudao.module.oa.api.dto.operations.ContentStatsVO;
import cn.iocoder.yudao.module.oa.api.dto.operations.FollowerAnalysisVO;
import cn.iocoder.yudao.module.oa.api.dto.operations.InternalContentVO;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Reads M10 collector tables for display in M1/M4 analysis pages.
 */
public interface CollectedDataQueryService {

    String DATA_SOURCE_COLLECT = "COLLECT";

    Set<String> COLLECTOR_PLATFORMS = Set.of(
            "WECHAT_OFFICIAL", "WECHAT_VIDEO", "DOUYIN", "KUAISHOU", "XIAOHONGSHU");

    boolean supportsPlatform(String platformType);

    Long latestFollowerCount(Long tenantId, Long accountId);

    int workCount(Long tenantId, Long accountId, String platformType);

    List<FollowerAnalysisVO> listFollowerStats(Long tenantId, Long accountId, String platformType,
                                               String accountName, String ipGroupName,
                                               LocalDate startDate, LocalDate endDate);

    PageResult<ContentAnalysisVO> pageCollectedContents(Long tenantId, Collection<Long> accountIds,
                                                          String platformType, String contentType,
                                                          String keyword, LocalDate startDate, LocalDate endDate,
                                                          Integer pageNo, Integer pageSize);

    PageResult<InternalContentVO> pageInternalContents(Long tenantId, Collection<Long> accountIds,
                                                       String platformType, String contentType,
                                                       String keyword, LocalDate startDate, LocalDate endDate,
                                                       Integer pageNo, Integer pageSize);

    ContentStatsVO aggregateStats(Long tenantId, Collection<Long> accountIds, String platformType,
                                  String contentType, LocalDate startDate, LocalDate endDate);

    List<Map<String, Object>> contentTrendByDay(Long tenantId, Long accountId, String platformType,
                                                LocalDate startDate, LocalDate endDate);
}
