package cn.iocoder.yudao.module.oa.service.operations;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.oa.api.dto.operations.ContentAnalysisVO;
import cn.iocoder.yudao.module.oa.api.dto.operations.ContentStatsVO;

import java.time.LocalDate;

public interface ContentAnalysisService {

    PageResult<ContentAnalysisVO> list(LocalDate startDate, LocalDate endDate, Long ipGroupId,
                                       Long accountId, String platformType, Boolean isHit,
                                       Integer pageNo, Integer pageSize);

    ContentStatsVO stats(LocalDate startDate, LocalDate endDate, Long ipGroupId, Long accountId);
}
