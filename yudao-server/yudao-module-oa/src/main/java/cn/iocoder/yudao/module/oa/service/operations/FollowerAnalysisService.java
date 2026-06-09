package cn.iocoder.yudao.module.oa.service.operations;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.oa.api.dto.operations.FollowerAnalysisVO;
import cn.iocoder.yudao.module.oa.api.dto.operations.FollowerTrendVO;

import java.time.LocalDate;
import java.util.List;

public interface FollowerAnalysisService {

    PageResult<FollowerAnalysisVO> list(LocalDate startDate, LocalDate endDate, Long ipGroupId,
                                        Long accountId, String platformType, Integer pageNo, Integer pageSize);

    List<FollowerTrendVO> trend(LocalDate startDate, LocalDate endDate, Long ipGroupId, Long accountId);
}
