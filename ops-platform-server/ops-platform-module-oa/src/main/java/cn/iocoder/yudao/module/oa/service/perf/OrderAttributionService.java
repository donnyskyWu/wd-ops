package cn.iocoder.yudao.module.oa.service.perf;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.oa.api.dto.perf.ExportJobVO;
import cn.iocoder.yudao.module.oa.api.dto.perf.OrderAttributionRoiVO;
import cn.iocoder.yudao.module.oa.api.dto.perf.OrderAttributionVO;

import java.time.LocalDate;

public interface OrderAttributionService {

    PageResult<OrderAttributionVO> list(Long ipGroupId, Long accountId, LocalDate startDate, LocalDate endDate,
                                        Integer pageNum, Integer pageSize);

    OrderAttributionRoiVO roi(Long ipGroupId, Long accountId, LocalDate startDate, LocalDate endDate);

    ExportJobVO export(LocalDate startDate, LocalDate endDate);
}
