package cn.iocoder.yudao.module.oa.service.monitor;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.oa.api.dto.monitor.ExternalWorkVO;
import cn.iocoder.yudao.module.oa.api.dto.monitor.MonitorFollowerAccountVO;

import java.time.LocalDate;
import java.util.Map;

public interface MonitorService {

    PageResult<ExternalWorkVO> externalList(String platformType, String contentType, Long ipGroupId, String industry,
                                            LocalDate startDate, LocalDate endDate, Integer pageNum, Integer pageSize);

    PageResult<ExternalWorkVO> hitList(String platformType, String contentType, Long ipGroupId,
                                       LocalDate startDate, LocalDate endDate, Integer pageNum, Integer pageSize);

    PageResult<ExternalWorkVO> lowScoreList(String platformType, String contentType, Long ipGroupId,
                                            LocalDate startDate, LocalDate endDate, Integer pageNum, Integer pageSize);

    PageResult<MonitorFollowerAccountVO> highFollowerList(String platformType, Long ipGroupId, Integer pageNum, Integer pageSize);

    PageResult<MonitorFollowerAccountVO> lowFollowerList(String platformType, Long ipGroupId, Integer pageNum, Integer pageSize);

    Map<String, Object> ipTheme(Long id);

    Map<String, Object> industryStats(Long id);
}
