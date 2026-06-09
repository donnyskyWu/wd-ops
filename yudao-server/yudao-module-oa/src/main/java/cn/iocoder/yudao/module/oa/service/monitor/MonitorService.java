package cn.iocoder.yudao.module.oa.service.monitor;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.oa.api.dto.monitor.ExternalWorkVO;

import java.time.LocalDate;
import java.util.Map;

public interface MonitorService {

    PageResult<ExternalWorkVO> externalList(String platformType, Long ipGroupId, String industry,
                                            LocalDate startDate, LocalDate endDate, Integer pageNum, Integer pageSize);

    PageResult<ExternalWorkVO> hitList(String platformType, Long ipGroupId, LocalDate startDate, LocalDate endDate,
                                       Integer pageNum, Integer pageSize);

    PageResult<ExternalWorkVO> lowScoreList(String platformType, Long ipGroupId, LocalDate startDate, LocalDate endDate,
                                            Integer pageNum, Integer pageSize);

    PageResult<ExternalWorkVO> highFollowerList(Long ipGroupId, Integer pageNum, Integer pageSize);

    PageResult<ExternalWorkVO> lowFollowerList(Long ipGroupId, Integer pageNum, Integer pageSize);

    Map<String, Object> ipTheme(Long id);

    Map<String, Object> industryStats(Long id);
}
