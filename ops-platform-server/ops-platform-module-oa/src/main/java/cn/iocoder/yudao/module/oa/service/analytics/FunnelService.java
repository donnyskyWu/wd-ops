package cn.iocoder.yudao.module.oa.service.analytics;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.oa.api.dto.analytics.FunnelCreateReq;
import cn.iocoder.yudao.module.oa.api.dto.analytics.FunnelDataVO;
import cn.iocoder.yudao.module.oa.api.dto.analytics.FunnelVO;
import cn.iocoder.yudao.module.oa.api.dto.perf.ExportJobVO;

public interface FunnelService {

    PageResult<FunnelVO> list(Integer pageNum, Integer pageSize);

    Long create(FunnelCreateReq req);

    FunnelDataVO getData(Long id, java.time.LocalDate startDate, java.time.LocalDate endDate,
                         Long ipGroupId, String platformType);

    ExportJobVO export(Long id);
}
