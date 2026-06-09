package cn.iocoder.yudao.module.oa.service.report;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.oa.api.dto.perf.ExportJobVO;
import cn.iocoder.yudao.module.oa.api.dto.report.ReportStatsVO;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public interface ReportService {

    PageResult<Map<String, Object>> unifiedAccountList(Long ipGroupId, Long accountId, String platformType,
                                                       LocalDate startDate, LocalDate endDate, Integer pageNum, Integer pageSize);

    ReportStatsVO unifiedAccountStats(Long ipGroupId, Long accountId, String platformType,
                                      LocalDate startDate, LocalDate endDate);

    ExportJobVO unifiedAccountExport(LocalDate startDate, LocalDate endDate);

    List<Map<String, Object>> accountStatusTrend(Long accountId, LocalDate startDate, LocalDate endDate);

    Map<String, Object> accountStatusSummary(Long accountId, LocalDate startDate, LocalDate endDate);

    PageResult<Map<String, Object>> accountStatusLog(Long accountId, LocalDate startDate, LocalDate endDate,
                                                    Integer pageNum, Integer pageSize);

    ExportJobVO accountStatusExport(LocalDate startDate, LocalDate endDate);

    PageResult<Map<String, Object>> videoOutputList(Long ipGroupId, Long accountId, LocalDate startDate, LocalDate endDate,
                                                    Integer pageNum, Integer pageSize);

    List<Map<String, Object>> videoOutputTrend(Long accountId, LocalDate startDate, LocalDate endDate);

    List<Map<String, Object>> videoOutputRanking(LocalDate startDate, LocalDate endDate, Integer limit);

    ExportJobVO videoOutputExport(LocalDate startDate, LocalDate endDate);

    PageResult<Map<String, Object>> liveDurationList(Long ipGroupId, LocalDate startDate, LocalDate endDate,
                                                    Integer pageNum, Integer pageSize);

    List<Map<String, Object>> liveDurationTrend(Long ipGroupId, LocalDate startDate, LocalDate endDate);

    ExportJobVO liveDurationExport(LocalDate startDate, LocalDate endDate);

    PageResult<Map<String, Object>> costAllocationList(Long accountId, LocalDate startDate, LocalDate endDate,
                                                       Integer pageNum, Integer pageSize);

    ExportJobVO costAllocationExport(LocalDate startDate, LocalDate endDate);

    PageResult<Map<String, Object>> roiList(Long ipGroupId, LocalDate startDate, LocalDate endDate,
                                            Integer pageNum, Integer pageSize);

    ExportJobVO roiExport(LocalDate startDate, LocalDate endDate);

    List<Map<String, Object>> teamConfigList(Long ipGroupId);

    PageResult<Map<String, Object>> accountAlertList(LocalDate startDate, LocalDate endDate,
                                                     Integer pageNum, Integer pageSize);

    ExportJobVO accountAlertExport(LocalDate startDate, LocalDate endDate);
}
